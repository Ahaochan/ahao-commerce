package moe.ahao.commerce.aftersale.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.LackCommand;
import moe.ahao.commerce.aftersale.api.dto.LackDTO;
import moe.ahao.commerce.aftersale.infrastructure.enums.RefundStatusEnum;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.AfterSaleLogDAO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleRefundDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleRefundMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.service.AfterSaleItemMybatisService;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.*;
import moe.ahao.commerce.common.event.ActualRefundEvent;
import moe.ahao.commerce.order.application.GenOrderIdAppService;
import moe.ahao.commerce.order.infrastructure.domain.dto.OrderExtJsonDTO;
import moe.ahao.commerce.order.infrastructure.domain.dto.OrderLackInfoDTO;
import moe.ahao.commerce.order.infrastructure.enums.AccountTypeEnum;
import moe.ahao.commerce.order.infrastructure.enums.OrderIdTypeEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.ProductGateway;
import moe.ahao.commerce.order.infrastructure.publisher.OrderEventPublisher;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderItemDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderItemMapper;
import moe.ahao.commerce.product.api.dto.ProductSkuDTO;
import moe.ahao.commerce.product.api.query.GetProductSkuQuery;
import moe.ahao.commerce.product.api.query.ListProductSkuQuery;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 订单缺品相关service
 */
@Service
@Slf4j
public class OrderLackAppService {
    @Autowired
    private OrderLackAppService _this;
    @Autowired
    private GenOrderIdAppService genOrderIdAppService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;
    @Autowired
    private AfterSaleItemMybatisService afterSaleItemMybatisService;
    @Autowired
    private AfterSaleRefundMapper afterSaleRefundMapper;
    @Autowired
    private AfterSaleLogDAO afterSaleLogDAO;

    @Autowired
    private ProductGateway productGateway;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 订单是否已经发起过缺品
     */
    public boolean isOrderLacked(OrderInfoDO orderInfo) {
        OrderExtJsonDTO orderExtJson = JSONHelper.parse(orderInfo.getExtJson(), OrderExtJsonDTO.class);
        if (null != orderExtJson) {
            return orderExtJson.getLackFlag();
        }
        return false;
    }

    /**
     * 具体的缺品处理
     */
    public LackDTO execute(LackCommand command) {
        // 1. 参数基本校验
        this.check(command);
        String orderId = command.getOrderId();

        // 2. 加分布式锁防并发
        String lockKey = RedisLockKeyConstants.LACK_REQUEST_KEY + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw OrderExceptionEnum.ORDER_NOT_ALLOW_TO_LACK.msg();
        }

        try {
            // 3. 执行缺品逻辑
            LackDTO lackDTO = _this.doExecute(command);
            return lackDTO;
        } finally {
            lock.unlock();
        }
    }

    private void check(LackCommand command) {
        String orderId = command.getOrderId();
        if (StringUtils.isEmpty(orderId)) {
            throw OrderExceptionEnum.ORDER_ID_IS_NULL.msg();
        }
        if (CollectionUtils.isEmpty(command.getLackItems())) {
            throw OrderExceptionEnum.LACK_ITEM_IS_NULL.msg();
        }

        for (LackCommand.LackItem lackItem : command.getLackItems()) {
            if (StringUtils.isEmpty(lackItem.getSkuCode())) {
                throw OrderExceptionEnum.SKU_CODE_IS_NULL.msg();
            }
            if (lackItem.getLackNum() == null) {
                throw OrderExceptionEnum.LACK_NUM_IS_LT_0.msg();
            }
            if (lackItem.getLackNum().compareTo(BigDecimal.ONE) < 0) {
                throw OrderExceptionEnum.LACK_NUM_IS_LT_0.msg();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public LackDTO doExecute(LackCommand command) {
        String orderId = command.getOrderId();
        // 1. 校验
        OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
        if (orderInfo == null) {
            throw OrderExceptionEnum.ORDER_NOT_FOUND.msg();
        }
        // 校验订单是否可以发起缺品, 可以发起缺品的前置条件:
        // 1.1. 订单的状态为："已出库"
        // 1.2. 订单未发起过缺品
        // 1.3. 普通订单类型
        // 解释一下为啥是"已出库"状态才能发起缺品:
        // 缺品的业务逻辑是这样的, 当订单支付后, 进入履约流程, 仓库人员捡货当时候发现现有商品无法满足下单所需, 即"缺品"了
        // 仓库人员首先会将通知订单系统将订单的状态变为"已出库", 然后会再来调用这个缺品的接口
        boolean orderLacked = this.isOrderLacked(orderInfo);
        boolean statusCanLack = OrderStatusEnum.canLack().contains(orderInfo.getOrderStatus());
        boolean typeCanLack = !OrderTypeEnum.canLack().contains(orderInfo.getOrderType());
        if (!statusCanLack || orderLacked || !typeCanLack) {
            throw OrderExceptionEnum.ORDER_NOT_ALLOW_TO_LACK.msg();
        }

        List<OrderItemDO> orderItems = orderItemMapper.selectListByOrderId(orderId);
        if (CollectionUtils.isEmpty(orderItems)) {
            throw OrderExceptionEnum.ORDER_ITEM_IS_NULL.msg();
        }
        Map<String, OrderItemDO> orderItemMap = orderItems.stream().collect(Collectors.toMap(OrderItemDO::getSkuCode, Function.identity()));

        List<String> skuCodeList = command.getLackItems().stream().map(LackCommand.LackItem::getSkuCode).collect(Collectors.toList());
        ListProductSkuQuery listProductSkuQuery = new ListProductSkuQuery();
        listProductSkuQuery.setSellerId(orderInfo.getSellerId());
        listProductSkuQuery.setSkuCodeList(skuCodeList);
        Map<String, ProductSkuDTO> productSkuMap = productGateway.listBySkuCodes(listProductSkuQuery).stream()
            .collect(Collectors.toMap(ProductSkuDTO::getSkuCode, Function.identity()));

        for (LackCommand.LackItem lackItem : command.getLackItems()) {
            String skuCode = lackItem.getSkuCode();
            ProductSkuDTO productSkuDTO = productSkuMap.get(skuCode);
            if (productSkuDTO == null) {
                throw OrderExceptionEnum.PRODUCT_SKU_CODE_ERROR.msg(skuCode);
            }
            OrderItemDO orderItem = orderItemMap.get(skuCode);
            if (orderItem == null) {
                throw OrderExceptionEnum.LACK_ITEM_NOT_IN_ORDER.msg(skuCode);
            }
            // 缺品商品数量不能>=下单商品数量
            if (orderItem.getSaleQuantity().compareTo(lackItem.getLackNum()) <= 0) {
                throw OrderExceptionEnum.LACK_NUM_IS_GE_SKU_ORDER_ITEM_SIZE.msg();
            }
        }

        // 2. 生成缺品售后单
        String userId = orderInfo.getUserId();
        String afterSaleId = genOrderIdAppService.generate(OrderIdTypeEnum.AFTER_SALE, userId);
        AfterSaleInfoDO afterSaleInfo = this.buildLackAfterSaleInfo(orderInfo, afterSaleId);

        // 3. 生成缺品售后单条目
        List<AfterSaleItemDO> afterSaleItems = this.buildLackAfterSaleItem(afterSaleId, orderItems, command.getLackItems());

        // 4. 补充缺品售后单, 计算申请退款金额、实际退款金额
        BigDecimal applyRefundAmount = BigDecimal.ZERO;
        BigDecimal realRefundAmount = BigDecimal.ZERO;
        for (AfterSaleItemDO afterSaleItem : afterSaleItems) {
            applyRefundAmount = applyRefundAmount.add(afterSaleItem.getApplyRefundAmount());
            realRefundAmount = realRefundAmount.add(afterSaleItem.getRealRefundAmount());
        }
        afterSaleInfo.setApplyRefundAmount(applyRefundAmount);
        afterSaleInfo.setRealRefundAmount(realRefundAmount);

        // 5. 构造售后退款单
        AfterSaleRefundDO afterSaleRefund = this.buildLackAfterSaleRefund(orderInfo, afterSaleInfo);

        // 6. 构造订单缺品扩展信息
        OrderExtJsonDTO lackExtJson = this.buildOrderLackExtJson(command, afterSaleInfo);

        // 6、存储售后单,item和退款单，日志
        afterSaleInfoMapper.insert(afterSaleInfo);
        afterSaleItemMybatisService.saveBatch(afterSaleItems);
        afterSaleRefundMapper.insert(afterSaleRefund);
        afterSaleLogDAO.save(afterSaleInfo, AfterSaleStatusChangeEnum.LACK_AFTER_SALE_CREATED);

        // 7、更新订单扩展信息
        String json = JSONHelper.toString(lackExtJson);
        orderInfoMapper.updateExtJsonByOrderId(orderId, json);

        // 8、发送缺品退款的消息
        ActualRefundEvent actualRefundEvent = new ActualRefundEvent();
        actualRefundEvent.setOrderId(afterSaleInfo.getOrderId());
        actualRefundEvent.setAfterSaleId(afterSaleInfo.getAfterSaleId());
        orderEventPublisher.sendLackItemRefundMessage(actualRefundEvent);

        return new LackDTO(orderInfo.getOrderId(), afterSaleId);
    }

    /**
     * 构造缺品售后单
     */
    private AfterSaleInfoDO buildLackAfterSaleInfo(OrderInfoDO order, String afterSaleId) {
        AfterSaleInfoDO afterSaleInfoDO = new AfterSaleInfoDO();
        afterSaleInfoDO.setAfterSaleId(afterSaleId);
        afterSaleInfoDO.setBusinessIdentifier(order.getBusinessIdentifier());
        afterSaleInfoDO.setOrderId(order.getOrderId());
        afterSaleInfoDO.setUserId(order.getUserId());
        afterSaleInfoDO.setOrderType(order.getOrderType());
        // 申请售后来源是系统自动退款
        afterSaleInfoDO.setApplySource(AfterSaleApplySourceEnum.SYSTEM.getCode());
        afterSaleInfoDO.setApplyTime(new Date());
        // afterSaleInfoDO.setApplyReasonCode();
        // afterSaleInfoDO.setApplyReason();
        afterSaleInfoDO.setReviewTime(new Date());
        // afterSaleInfoDO.setReviewSource();
        // afterSaleInfoDO.setReviewReasonCode();
        // afterSaleInfoDO.setReviewReason();
        // 售后类型是缺品退款, 并无需审核, 自动审核通过
        afterSaleInfoDO.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());
        afterSaleInfoDO.setAfterSaleTypeDetail(AfterSaleTypeDetailEnum.LACK_REFUND.getCode());
        afterSaleInfoDO.setAfterSaleStatus(AfterSaleStatusEnum.REVIEW_PASS.getCode());
        // 申请退款金额 和 实际退款金额 在后面计算
        // afterSaleInfoDO.setApplyRefundAmount();
        // afterSaleInfoDO.setRealRefundAmount();
        // afterSaleInfoDO.setRemark();

        return afterSaleInfoDO;
    }

    /**
     * 构造缺品售后支付单条目
     */
    private List<AfterSaleItemDO> buildLackAfterSaleItem(String afterSaleId, List<OrderItemDO> orderItems, Set<LackCommand.LackItem> lackItems) {
        Map<String, OrderItemDO> orderItemMap = orderItems.stream().collect(Collectors.toMap(OrderItemDO::getSkuCode, Function.identity()));

        List<AfterSaleItemDO> list = new ArrayList<>();
        for (LackCommand.LackItem lackItem : lackItems) {
            String skuCode = lackItem.getSkuCode();
            BigDecimal lackNum = lackItem.getLackNum();

            // 1. 参数校验
            if (StringUtils.isEmpty(skuCode)) {
                throw OrderExceptionEnum.SKU_CODE_IS_NULL.msg();
            }
            if (lackNum == null || lackNum.compareTo(BigDecimal.ONE) < 0) {
                throw OrderExceptionEnum.LACK_NUM_IS_LT_0.msg();
            }

            // 2. 找到item中对应的缺品sku item
            OrderItemDO orderItem = orderItemMap.get(skuCode);
            if (orderItem == null) {
                throw OrderExceptionEnum.LACK_ITEM_NOT_IN_ORDER.msg(skuCode);
            }

            // 3. 缺品商品数量不能>=下单商品数量
            if (orderItem.getSaleQuantity().compareTo(lackItem.getLackNum()) <= 0) {
                throw OrderExceptionEnum.LACK_NUM_IS_GE_SKU_ORDER_ITEM_SIZE.msg();
            }

            // 4. 查询商品sku
            ProductSkuDTO productSku = productGateway.getBySkuCode(new GetProductSkuQuery(skuCode, orderItem.getSellerId()));
            if (productSku == null) {
                throw OrderExceptionEnum.PRODUCT_SKU_CODE_ERROR.msg(skuCode);
            }

            // 5. 构造售后单条目
            AfterSaleItemDO afterSaleItemDO = new AfterSaleItemDO();
            afterSaleItemDO.setAfterSaleId(afterSaleId);
            afterSaleItemDO.setOrderId(orderItem.getOrderId());
            afterSaleItemDO.setSkuCode(productSku.getSkuCode());
            afterSaleItemDO.setProductName(productSku.getProductName());
            afterSaleItemDO.setProductImg(orderItem.getProductImg());
            afterSaleItemDO.setReturnQuantity(lackNum);
            // 原始金额
            afterSaleItemDO.setOriginAmount(orderItem.getOriginAmount());
            // 申请退款金额 = 单价 * 缺品数量
            BigDecimal applyRefundAmount = orderItem.getSalePrice().multiply(lackNum);
            afterSaleItemDO.setApplyRefundAmount(applyRefundAmount);
            // 实际退款金额 = (缺品数量/下单数量) * 原付款金额
            BigDecimal realRefundAmount = this.calculateOrderItemLackRealRefundAmount(orderItem, lackNum);
            afterSaleItemDO.setRealRefundAmount(realRefundAmount);

            list.add(afterSaleItemDO);
        }
        return list;
    }

    /**
     * 构造缺品售后支付单
     */
    private AfterSaleRefundDO buildLackAfterSaleRefund(OrderInfoDO order, AfterSaleInfoDO afterSaleInfo) {
        // 1. 构造售后单
        AfterSaleRefundDO AfterSaleRefundDO = new AfterSaleRefundDO();
        // AfterSaleRefundDO.setAfterSaleRefundId();
        AfterSaleRefundDO.setAfterSaleId(afterSaleInfo.getAfterSaleId());
        AfterSaleRefundDO.setOrderId(afterSaleInfo.getOrderId());
        // AfterSaleRefundDO.setAfterSaleBatchNo();
        AfterSaleRefundDO.setAccountType(AccountTypeEnum.THIRD.getCode());
        AfterSaleRefundDO.setPayType(order.getPayType());
        AfterSaleRefundDO.setRefundStatus(RefundStatusEnum.UN_REFUND.getCode());
        AfterSaleRefundDO.setRefundAmount(afterSaleInfo.getRealRefundAmount());
        // AfterSaleRefundDO.setRefundPayTime();
        // AfterSaleRefundDO.setOutTradeNo();
        // AfterSaleRefundDO.setRemark();

        return AfterSaleRefundDO;
    }

    /**
     * 构造订单缺品扩展信息
     */
    private OrderExtJsonDTO buildOrderLackExtJson(LackCommand command, AfterSaleInfoDO afterSaleInfo) {
        OrderExtJsonDTO orderExtJson = new OrderExtJsonDTO();
        orderExtJson.setLackFlag(true);

        OrderLackInfoDTO lackInfo = new OrderLackInfoDTO();
        lackInfo.setOrderId(afterSaleInfo.getOrderId());
        lackInfo.setApplyRefundAmount(afterSaleInfo.getApplyRefundAmount());
        lackInfo.setRealRefundAmount(afterSaleInfo.getRealRefundAmount());

        List<OrderLackInfoDTO.LackItem> lackItemDTOList = command.getLackItems().stream()
            .map(i -> new OrderLackInfoDTO.LackItem(i.getSkuCode(), i.getLackNum()))
            .collect(Collectors.toList());
        lackInfo.setLackItems(lackItemDTOList);

        orderExtJson.setLackInfo(lackInfo);
        return orderExtJson;
    }

    private BigDecimal calculateOrderItemLackRealRefundAmount(OrderItemDO orderItem, BigDecimal lackNum) {
        BigDecimal rate = lackNum.divide(orderItem.getSaleQuantity(), 6, RoundingMode.HALF_UP);
        // 金额向上取整
        BigDecimal itemRefundAmount = orderItem.getPayAmount().multiply(rate).setScale(6, RoundingMode.DOWN);
        return itemRefundAmount;
    }
}
