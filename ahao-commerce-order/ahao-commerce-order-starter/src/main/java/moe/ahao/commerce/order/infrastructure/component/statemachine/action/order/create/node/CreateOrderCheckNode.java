package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.create.node;

import moe.ahao.commerce.common.enums.AmountTypeEnum;
import moe.ahao.commerce.common.enums.BusinessIdentifierEnum;
import moe.ahao.commerce.common.enums.OrderTypeEnum;
import moe.ahao.commerce.common.enums.PayTypeEnum;
import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.infrastructure.enums.AccountTypeEnum;
import moe.ahao.commerce.order.infrastructure.enums.DeliveryTypeEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.exception.CommonBizExceptionEnum;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.StandardProcessor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建订单 检查参数节点
 */
@Component
public class CreateOrderCheckNode extends StandardProcessor {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Override
    protected void processInternal(ProcessContext processContext) {
        CreateOrderCommand command = processContext.get("createOrderCommand");
        if (command == null) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }

        // 订单id
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
        if (businessIdentifier == null) {
            throw OrderExceptionEnum.BUSINESS_IDENTIFIER_IS_NULL.msg();
        }
        BusinessIdentifierEnum businessIdentifierEnum = BusinessIdentifierEnum.getByCode(businessIdentifier);
        if (businessIdentifierEnum == null) {
            throw OrderExceptionEnum.BUSINESS_IDENTIFIER_ERROR.msg();
        }

        // 用户ID
        String userId = command.getUserId();
        if (StringUtils.isEmpty(userId)) {
            throw OrderExceptionEnum.USER_ID_IS_NULL.msg();
        }

        // 订单类型
        Integer orderType = command.getOrderType();
        if (orderType == null) {
            throw OrderExceptionEnum.ORDER_TYPE_IS_NULL.msg();
        }
        OrderTypeEnum orderTypeEnum = OrderTypeEnum.getByCode(orderType);
        if (OrderTypeEnum.UNKNOWN == orderTypeEnum) {
            throw OrderExceptionEnum.ORDER_TYPE_ERROR.msg();
        }

        // 卖家ID
        String sellerId = command.getSellerId();
        if (StringUtils.isEmpty(sellerId)) {
            throw OrderExceptionEnum.SELLER_ID_IS_NULL.msg();
        }

        // 配送类型
        Integer deliveryType = command.getDeliveryType();
        if (deliveryType == null) {
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
        if (StringUtils.isAnyEmpty(province, city, area, street)) {
            throw OrderExceptionEnum.USER_ADDRESS_ERROR.msg();
        }

        // 区域ID
        String regionId = command.getRegionId();
        if (StringUtils.isEmpty(regionId)) {
            throw OrderExceptionEnum.REGION_ID_IS_NULL.msg();
        }

        // 经纬度
        BigDecimal lon = command.getLon();
        BigDecimal lat = command.getLat();
        if (lon == null || lat == null) {
            throw OrderExceptionEnum.USER_LOCATION_IS_NULL.msg();
        }

        // 收货人信息
        String receiverName = command.getReceiverName();
        String receiverPhone = command.getReceiverPhone();
        if (StringUtils.isAnyEmpty(receiverName, receiverPhone)) {
            throw OrderExceptionEnum.ORDER_RECEIVER_IS_NULL.msg();
        }

        // 客户端设备信息
        String clientIp = command.getClientIp();
        if (StringUtils.isEmpty(clientIp)) {
            throw OrderExceptionEnum.CLIENT_IP_IS_NULL.msg();
        }

        // 商品条目信息
        List<CreateOrderCommand.OrderItem> orderItems = command.getOrderItems();
        if (CollectionUtils.isEmpty(orderItems)) {
            throw OrderExceptionEnum.ORDER_ITEM_IS_NULL.msg();
        }
        for (CreateOrderCommand.OrderItem orderItem : orderItems) {
            Integer productType = orderItem.getProductType();
            BigDecimal saleQuantity = orderItem.getSaleQuantity();
            String skuCode = orderItem.getSkuCode();
            if (productType == null || saleQuantity == null || StringUtils.isEmpty(skuCode)) {
                throw OrderExceptionEnum.ORDER_ITEM_PARAM_ERROR.msg();
            }
        }

        // 订单费用信息
        List<CreateOrderCommand.OrderAmount> orderAmounts = command.getOrderAmounts();
        if (CollectionUtils.isEmpty(orderAmounts)) {
            throw OrderExceptionEnum.ORDER_AMOUNT_IS_NULL.msg();
        }
        Map<AmountTypeEnum, BigDecimal> orderAmountMap = new HashMap<>();
        for (CreateOrderCommand.OrderAmount orderAmount : orderAmounts) {
            Integer amountType = orderAmount.getAmountType();
            BigDecimal amount = orderAmount.getAmount();
            if (amountType == null) {
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
        if (CollectionUtils.isEmpty(orderPayments)) {
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
}
