package moe.ahao.commerce.order.application;


import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.*;
import moe.ahao.commerce.common.infrastructure.event.PayOrderTimeoutEvent;
import moe.ahao.commerce.market.api.dto.CalculateOrderAmountDTO;
import moe.ahao.commerce.market.api.query.CalculateOrderAmountQuery;
import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.api.dto.CreateOrderDTO;
import moe.ahao.commerce.order.infrastructure.enums.AccountTypeEnum;
import moe.ahao.commerce.order.infrastructure.enums.DeliveryTypeEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.MarketCalculateGateway;
import moe.ahao.commerce.order.infrastructure.gateway.ProductGateway;
import moe.ahao.commerce.order.infrastructure.gateway.RiskGateway;
import moe.ahao.commerce.order.infrastructure.publisher.OrderEventPublisher;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.OrderStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import moe.ahao.commerce.product.api.dto.ProductSkuDTO;
import moe.ahao.commerce.product.api.query.ListProductSkuQuery;
import moe.ahao.commerce.risk.api.command.CheckOrderRiskCommand;
import moe.ahao.exception.CommonBizExceptionEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CreateOrderAppService {
    @Autowired
    private CreateOrderTxService createOrderTxService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private ProductGateway productGateway;
    @Autowired
    private MarketCalculateGateway marketCalculateGateway;
    @Autowired
    private RiskGateway riskGateway;

    @Autowired
    private OrderEventPublisher orderEventPublisher;
    @Autowired
    private StateMachineFactory stateMachineFactory;

    public CreateOrderDTO createOrder(CreateOrderCommand command) {
        // 1. 获取初始状态为NULL的订单状态机
        OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.NULL);
        // 2. 触发ORDER_CREATED事件, 将订单状态从null扭转为created, 并触发对应的业务逻辑操作
        orderStateMachine.fire(OrderStatusChangeEnum.ORDER_CREATED, command);

        // 返回订单信息
        CreateOrderDTO createOrderDTO = new CreateOrderDTO();
        createOrderDTO.setOrderId(command.getOrderId());
        return createOrderDTO;
    }

    public CreateOrderDTO createOrderV1(CreateOrderCommand command) {
        log.info("创建订单command:{}", command);

        // 1. 入参检查
        this.checkCommand(command);
        String orderId = command.getOrderId();

        // 2. 风控检查
        this.checkRisk(command);

        // 3. 获取商品信息
        List<ProductSkuDTO> productList = this.listProduct(command);

        // 4. 计算订单价格
        CalculateOrderAmountDTO calculateOrderAmountDTO = this.calculateOrderAmount(command, productList);

        // 5. 验证订单实付金额
        this.checkRealPayAmount(command, calculateOrderAmountDTO);

        // 6. 生成订单（包含锁定优惠券、扣减库存等逻辑）
        createOrderTxService.addNewOrder(command, productList, calculateOrderAmountDTO);

        // 7. 发送订单延迟消息用于支付超时自动关单
        this.sendPayOrderTimeoutEvent(command);

        // 返回订单信息
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setOrderId(orderId);
        return dto;
    }

    private void checkCommand(CreateOrderCommand command) {
        if (command == null) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }

        // 订单编号
        String orderId = command.getOrderId();
        if (StringUtils.isEmpty(orderId)) {
            throw OrderExceptionEnum.ORDER_ID_IS_NULL.msg();
        }
        OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
        if (orderInfo != null) {
            throw OrderExceptionEnum.ORDER_EXISTED.msg();
        }

        // 业务线标识
        Integer businessIdentifier = command.getBusinessIdentifier();
        if(businessIdentifier == null) {
            throw OrderExceptionEnum.BUSINESS_IDENTIFIER_IS_NULL.msg();
        }
        BusinessIdentifierEnum businessIdentifierEnum = BusinessIdentifierEnum.getByCode(businessIdentifier);
        if(businessIdentifierEnum == null) {
            throw OrderExceptionEnum.BUSINESS_IDENTIFIER_ERROR.msg();
        }

        // 用户ID
        String userId = command.getUserId();
        if(StringUtils.isEmpty(userId)) {
            throw OrderExceptionEnum.USER_ID_IS_NULL.msg();
        }

        // 订单类型
        Integer orderType = command.getOrderType();
        if(orderType == null) {
            throw OrderExceptionEnum.ORDER_TYPE_IS_NULL.msg();
        }
        OrderTypeEnum orderTypeEnum = OrderTypeEnum.getByCode(orderType);
        if (OrderTypeEnum.UNKNOWN == orderTypeEnum) {
            throw OrderExceptionEnum.ORDER_TYPE_ERROR.msg();
        }

        // 卖家ID
        String sellerId = command.getSellerId();
        if(StringUtils.isEmpty(sellerId)) {
            throw OrderExceptionEnum.SELLER_ID_IS_NULL.msg();
        }

        // 配送类型
        Integer deliveryType = command.getDeliveryType();
        if(deliveryType == null) {
            throw OrderExceptionEnum.DELIVERY_TYPE_IS_NULL.msg();
        }
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.getByCode(deliveryType);
        if (deliveryTypeEnum == null) {
            throw OrderExceptionEnum.DELIVERY_TYPE_ERROR.msg();
        }

        // 地址信息
        String province = command.getProvince();
        String city = command.getCity();
        String area = command.getArea();
        String street = command.getStreet();
        if(StringUtils.isAnyEmpty(province, city, area, street)) {
            throw OrderExceptionEnum.USER_ADDRESS_ERROR.msg();
        }

        // 区域ID
        String regionId = command.getRegionId();
        if(StringUtils.isEmpty(regionId)) {
            throw OrderExceptionEnum.REGION_ID_IS_NULL.msg();
        }

        // 经纬度
        BigDecimal lon = command.getLon();
        BigDecimal lat = command.getLat();
        if(lon == null || lat == null) {
            throw OrderExceptionEnum.USER_LOCATION_IS_NULL.msg();
        }

        // 收货人信息
        String receiverName = command.getReceiverName();
        String receiverPhone = command.getReceiverPhone();
        if(StringUtils.isAnyEmpty(receiverName, receiverPhone)) {
            throw OrderExceptionEnum.ORDER_RECEIVER_IS_NULL.msg();
        }

        // 客户端设备信息
        String clientIp = command.getClientIp();
        if(StringUtils.isEmpty(clientIp)) {
            throw OrderExceptionEnum.CLIENT_IP_IS_NULL.msg();
        }

        // 商品条目信息
        List<CreateOrderCommand.OrderItem> orderItems = command.getOrderItems();
        if(CollectionUtils.isEmpty(orderItems)) {
            throw OrderExceptionEnum.ORDER_ITEM_IS_NULL.msg();
        }
        for (CreateOrderCommand.OrderItem orderItem : orderItems) {
            Integer productType = orderItem.getProductType();
            BigDecimal saleQuantity = orderItem.getSaleQuantity();
            String skuCode = orderItem.getSkuCode();
            if(productType == null || saleQuantity == null || StringUtils.isEmpty(skuCode)) {
                throw OrderExceptionEnum.ORDER_ITEM_PARAM_ERROR.msg();
            }
        }

        // 订单费用信息
        List<CreateOrderCommand.OrderAmount> orderAmounts = command.getOrderAmounts();
        if(CollectionUtils.isEmpty(orderAmounts)) {
            throw OrderExceptionEnum.ORDER_AMOUNT_IS_NULL.msg();
        }
        Map<AmountTypeEnum, BigDecimal> orderAmountMap = new HashMap<>();
        for (CreateOrderCommand.OrderAmount orderAmount : orderAmounts) {
            Integer amountType = orderAmount.getAmountType();
            BigDecimal amount = orderAmount.getAmount();
            if(amountType == null) {
                throw OrderExceptionEnum.ORDER_AMOUNT_TYPE_IS_NULL.msg();
            }
            AmountTypeEnum amountTypeEnum = AmountTypeEnum.getByCode(amountType);
            if (amountTypeEnum == null) {
                throw OrderExceptionEnum.ORDER_AMOUNT_TYPE_PARAM_ERROR.msg();
            }
            orderAmountMap.put(amountTypeEnum, amount);
        }
        // 订单支付原价不能为空
        if (orderAmountMap.get(AmountTypeEnum.ORIGIN_PAY_AMOUNT) == null) {
            throw OrderExceptionEnum.ORDER_ORIGIN_PAY_AMOUNT_IS_NULL.msg();
        }
        // 订单运费不能为空
        if (orderAmountMap.get(AmountTypeEnum.SHIPPING_AMOUNT) == null) {
            throw OrderExceptionEnum.ORDER_SHIPPING_AMOUNT_IS_NULL.msg();
        }
        // 订单实付金额不能为空
        if (orderAmountMap.get(AmountTypeEnum.REAL_PAY_AMOUNT) == null) {
            throw OrderExceptionEnum.ORDER_REAL_PAY_AMOUNT_IS_NULL.msg();
        }

        String couponId = command.getCouponId();
        BigDecimal couponDiscountAmount = orderAmountMap.get(AmountTypeEnum.COUPON_DISCOUNT_AMOUNT);
        // 订单优惠券抵扣金额不能为空
        if (StringUtils.isNotEmpty(couponId) && couponDiscountAmount == null) {
            throw OrderExceptionEnum.ORDER_DISCOUNT_AMOUNT_IS_NULL.msg();
        }

        // 订单支付信息
        List<CreateOrderCommand.OrderPayment> orderPayments = command.getOrderPayments();
        if(CollectionUtils.isEmpty(orderPayments)) {
            throw OrderExceptionEnum.ORDER_PAYMENT_IS_NULL.msg();
        }
        for (CreateOrderCommand.OrderPayment orderPayment : orderPayments) {
            Integer payType = orderPayment.getPayType();
            PayTypeEnum payTypeEnum = PayTypeEnum.getByCode(payType);
            if (payTypeEnum == null) {
                throw OrderExceptionEnum.PAY_TYPE_PARAM_ERROR.msg();
            }
            Integer accountType = orderPayment.getAccountType();
            AccountTypeEnum accountTypeEnum = AccountTypeEnum.getByCode(accountType);
            if (accountTypeEnum == null) {
                throw OrderExceptionEnum.ORDER_AMOUNT_TYPE_PARAM_ERROR.msg();
            }
        }
    }

    private void checkRisk(CreateOrderCommand createOrderCommand) {
        // 调用风控服务进行风控检查
        CheckOrderRiskCommand command = new CheckOrderRiskCommand();
        command.setBusinessIdentifier(createOrderCommand.getBusinessIdentifier());
        command.setOrderId(createOrderCommand.getOrderId());
        command.setUserId(createOrderCommand.getUserId());
        command.setSellerId(createOrderCommand.getSellerId());
        command.setClientIp(createOrderCommand.getClientIp());
        command.setDeviceId(createOrderCommand.getDeviceId());

        riskGateway.checkOrderRisk(command);
    }

    private List<ProductSkuDTO> listProduct(CreateOrderCommand command) {
        String sellerId = command.getSellerId();
        List<String> skuCodeList = command.getOrderItems().stream()
            .map(CreateOrderCommand.OrderItem::getSkuCode)
            .collect(Collectors.toList());

        ListProductSkuQuery query = new ListProductSkuQuery();
        query.setSellerId(sellerId);
        query.setSkuCodeList(skuCodeList);
        List<ProductSkuDTO> productSkuList = productGateway.listBySkuCodes(query);
        return productSkuList;
    }

    private CalculateOrderAmountDTO calculateOrderAmount(CreateOrderCommand command, List<ProductSkuDTO> productSkuList) {
        // 1. 订单条目补充商品信息
        Map<String, ProductSkuDTO> productSkuDTOMap = productSkuList.stream()
            .collect(Collectors.toMap(ProductSkuDTO::getSkuCode, Function.identity()));

        // 2. 组装营销服务计算价格请求体
        CalculateOrderAmountQuery query = new CalculateOrderAmountQuery();
        query.setOrderId(command.getOrderId());
        query.setUserId(command.getUserId());
        query.setSellerId(command.getSellerId());
        query.setCouponId(command.getCouponId());
        query.setRegionId(command.getRegionId());
        List<CalculateOrderAmountQuery.OrderItem> orderItemQueryList = new ArrayList<>();
        for (CreateOrderCommand.OrderItem orderItemCommand : command.getOrderItems()) {
            ProductSkuDTO productSkuDTO = productSkuDTOMap.get(orderItemCommand.getSkuCode());

            CalculateOrderAmountQuery.OrderItem orderItemAmountQuery = new CalculateOrderAmountQuery.OrderItem();
            orderItemAmountQuery.setProductId(productSkuDTO.getProductId());
            orderItemAmountQuery.setSkuCode(orderItemCommand.getSkuCode());
            orderItemAmountQuery.setSalePrice(productSkuDTO.getSalePrice());
            orderItemAmountQuery.setSaleQuantity(orderItemCommand.getSaleQuantity());

            orderItemQueryList.add(orderItemAmountQuery);
        }
        query.setOrderItemList(orderItemQueryList);

        List<CalculateOrderAmountQuery.OrderAmount> orderAmountQueryList = new ArrayList<>();
        for (CreateOrderCommand.OrderAmount orderAmountCommand : command.getOrderAmounts()) {

            CalculateOrderAmountQuery.OrderAmount orderAmountQuery = new CalculateOrderAmountQuery.OrderAmount();
            orderAmountQuery.setAmountType(orderAmountCommand.getAmountType());
            orderAmountQuery.setAmount(orderAmountCommand.getAmount());

            orderAmountQueryList.add(orderAmountQuery);
        }
        query.setOrderAmountList(orderAmountQueryList);

        // 3. 调用营销服务计算订单价格
        CalculateOrderAmountDTO calculateOrderAmountDTO = marketCalculateGateway.calculateOrderAmount(query);
        if (calculateOrderAmountDTO == null) {
            throw OrderExceptionEnum.CALCULATE_ORDER_AMOUNT_ERROR.msg();
        }
        return calculateOrderAmountDTO;
    }

    private void checkRealPayAmount(CreateOrderCommand command, CalculateOrderAmountDTO calculateDTO) {
        // 前端给的实付金额
        Map<Integer, CreateOrderCommand.OrderAmount> originOrderAmountMap = command.getOrderAmounts().stream()
            .collect(Collectors.toMap(
                CreateOrderCommand.OrderAmount::getAmountType, Function.identity()));

        BigDecimal originRealPayAmount = originOrderAmountMap.get(AmountTypeEnum.REAL_PAY_AMOUNT.getCode()).getAmount();

        // 营销计算出来的实付金额
        Map<Integer, CalculateOrderAmountDTO.OrderAmountDTO> orderAmountMap = calculateDTO.getOrderAmountList().stream()
            .collect(Collectors.toMap(CalculateOrderAmountDTO.OrderAmountDTO::getAmountType, Function.identity()));
        BigDecimal realPayAmount = orderAmountMap.get(AmountTypeEnum.REAL_PAY_AMOUNT.getCode()).getAmount();

        // 订单验价失败
        if (originRealPayAmount.compareTo(realPayAmount) != 0) {
            throw OrderExceptionEnum.ORDER_CHECK_REAL_PAY_AMOUNT_FAIL.msg();
        }
    }

    /**
     * 发送支付订单超时延迟消息，用于支付超时自动关单
     */
    private void sendPayOrderTimeoutEvent(CreateOrderCommand command) {
        PayOrderTimeoutEvent event = new PayOrderTimeoutEvent();
        event.setOrderId(command.getOrderId());
        orderEventPublisher.sendPayOrderTimeoutEvent(event);
    }
}
