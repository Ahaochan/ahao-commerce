package moe.ahao.commerce.order.infrastructure.component.statemachine.action.aftersale;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.infrastructure.enums.OrderCancelTypeEnum;
import moe.ahao.commerce.aftersale.infrastructure.enums.RefundStatusEnum;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleLogDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleRefundDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleLogMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleRefundMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.service.AfterSaleItemMybatisService;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.*;
import moe.ahao.commerce.common.event.ActualRefundEvent;
import moe.ahao.commerce.order.application.GenOrderIdAppService;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.AfterSaleStateAction;
import moe.ahao.commerce.order.infrastructure.enums.AccountTypeEnum;
import moe.ahao.commerce.order.infrastructure.enums.OrderIdTypeEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.publisher.OrderEventPublisher;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderItemDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderPaymentDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderItemMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderPaymentDetailMapper;
import moe.ahao.util.commons.lang.RandomHelper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 取消订单创建售后信息Action
 */
@Component
@Slf4j
public class CancelOrderCreatedInfoAction extends AfterSaleStateAction<String> {
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
    private AfterSaleLogMapper afterSaleLogMapper;
    @Autowired
    private OrderPaymentDetailMapper orderPaymentDetailMapper;
    @Autowired
    private AfterSaleRefundMapper afterSaleRefundMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Override
    public AfterSaleStatusChangeEnum event() {
        return AfterSaleStatusChangeEnum.CANCEL_AFTER_SALE_CREATED;
    }

    @Override
    protected String onStateChangeInternal(AfterSaleStatusChangeEnum event, String orderId) {
        // 1. 加分布式锁
        String lockKey = RedisLockKeyConstants.REFUND_KEY + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw OrderExceptionEnum.PROCESS_REFUND_REPEAT.msg();
        }
        try {
            // 2. 执行本地事务
            String afterSaleId = this.insertAfterSaleInfoWithTx(orderId);

            // 3. 取消订单发送实际退款事务MQ
            ActualRefundEvent actualRefundEvent = new ActualRefundEvent();
            actualRefundEvent.setOrderId(orderId);
            actualRefundEvent.setAfterSaleId(afterSaleId);
            actualRefundEvent.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());
            orderEventPublisher.sendCancelOrderRefundMessage(actualRefundEvent);
            return afterSaleId;
        } finally {
            lock.unlock();
        }
    }

    private String insertAfterSaleInfoWithTx(String orderId) {
        // @Transactional无法生效，需要用编程式事务
        return transactionTemplate.execute(status -> {
            // 1. 未支付的订单不需要进入取消退款流程, 未付款不记售后单
            OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
            if (orderInfo.getOrderStatus() <= OrderStatusEnum.CREATED.getCode()) {
                return null;
            }

            // 2. 新增售后信息
            String afterSaleId = this.insertAfterSaleInfo(orderInfo);
            return afterSaleId;
        });
    }

    private String insertAfterSaleInfo(OrderInfoDO orderInfo) {
        String orderId = orderInfo.getOrderId();

        // 1. 生成售后单id
        String afterSaleId = genOrderIdAppService.generate(OrderIdTypeEnum.AFTER_SALE, orderInfo.getUserId());

        // 2. 新增售后订单表
        Integer fromStatus = AfterSaleStatusEnum.UN_CREATED.getCode();
        Integer toStatus = AfterSaleStatusEnum.REVIEW_PASS.getCode();
        AfterSaleInfoDO afterSaleInfoDO = this.buildAfterSaleInfo(orderInfo, afterSaleId, toStatus);
        afterSaleInfoMapper.insert(afterSaleInfoDO);
        log.info("新增订单售后记录, 订单号:{}, 售后单号:{}, 订单售后状态:{}", orderId, afterSaleId, afterSaleInfoDO.getAfterSaleStatus());

        // 3. 新增售后条目表
        List<OrderItemDO> orderItems = orderItemMapper.selectListByOrderId(orderId);
        List<AfterSaleItemDO> afterSaleItems = this.buildAfterSaleItems(afterSaleId, orderItems);
        afterSaleItemMybatisService.saveBatch(afterSaleItems);

        // 4. 新增售后变更表
        Integer cancelType = orderInfo.getCancelType();
        AfterSaleLogDO afterSaleLogDO = this.buildAfterSaleLog(afterSaleId, fromStatus, toStatus, cancelType);
        afterSaleLogMapper.insert(afterSaleLogDO);
        log.info("新增售后单变更信息, 订单号:{}, 售后单号:{}, preStatus:{}, currentStatus:{}", orderId, afterSaleId, afterSaleLogDO.getPreStatus(), afterSaleLogDO.getCurrentStatus());

        // 5. 新增售后支付表
        AfterSaleRefundDO afterSaleRefundDO = this.buildAfterSaleRefund(afterSaleInfoDO);
        afterSaleRefundMapper.insert(afterSaleRefundDO);
        log.info("新增售后支付信息,订单号:{},售后单号:{},状态:{}", orderId, afterSaleId, afterSaleRefundDO.getRefundStatus());

        return afterSaleId;
    }

    private AfterSaleInfoDO buildAfterSaleInfo(OrderInfoDO orderInfoDO, String afterSaleId, Integer cancelOrderAfterSaleStatus) {
        AfterSaleInfoDO afterSaleInfoDO = new AfterSaleInfoDO();
        afterSaleInfoDO.setAfterSaleId(afterSaleId);
        afterSaleInfoDO.setBusinessIdentifier(orderInfoDO.getBusinessIdentifier());
        afterSaleInfoDO.setOrderId(orderInfoDO.getOrderId());
        afterSaleInfoDO.setUserId(orderInfoDO.getUserId());
        afterSaleInfoDO.setOrderType(orderInfoDO.getOrderType());
        afterSaleInfoDO.setApplySource(AfterSaleApplySourceEnum.SYSTEM.getCode());
        afterSaleInfoDO.setApplyTime(new Date());
        afterSaleInfoDO.setApplyReasonCode(AfterSaleReasonEnum.CANCEL.getCode());
        afterSaleInfoDO.setApplyReason(AfterSaleReasonEnum.CANCEL.getName());
        afterSaleInfoDO.setReviewTime(new Date());
        // 取消订单不需要审核, 自动审核通过
        // afterSaleInfoDO.setReviewSource();
        // afterSaleInfoDO.setReviewReasonCode();
        // afterSaleInfoDO.setReviewReason();
        //  取消订单 整笔退款
        afterSaleInfoDO.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());
        // afterSaleInfoDO.setAfterSaleTypeDetail();
        afterSaleInfoDO.setAfterSaleStatus(cancelOrderAfterSaleStatus);
        // 取消订单过程中的 申请退款金额 和 实际退款金额 都是实付退款金额 金额相同
        afterSaleInfoDO.setApplyRefundAmount(orderInfoDO.getPayAmount());
        afterSaleInfoDO.setRealRefundAmount(orderInfoDO.getPayAmount());
        // afterSaleInfoDO.setRemark();

        Integer cancelType = orderInfoDO.getCancelType();
        if (Objects.equals(OrderCancelTypeEnum.TIMEOUT_CANCELED.getCode(), cancelType)) {
            afterSaleInfoDO.setAfterSaleTypeDetail(AfterSaleTypeDetailEnum.TIMEOUT_NO_PAY.getCode());
            afterSaleInfoDO.setRemark("超时未支付自动取消");
        }
        if (Objects.equals(OrderCancelTypeEnum.USER_CANCELED.getCode(), cancelType)) {
            afterSaleInfoDO.setAfterSaleTypeDetail(AfterSaleTypeDetailEnum.USER_CANCEL.getCode());
            afterSaleInfoDO.setRemark("用户手动取消");
        }
        return afterSaleInfoDO;
    }

    private List<AfterSaleItemDO> buildAfterSaleItems(String afterSaleId, List<OrderItemDO> orderItems) {
        List<AfterSaleItemDO> afterSaleItems = new ArrayList<>(orderItems.size());
        for (OrderItemDO orderItem : orderItems) {
            AfterSaleItemDO afterSaleItem = new AfterSaleItemDO();
            afterSaleItem.setAfterSaleId(afterSaleId);
            afterSaleItem.setOrderId(orderItem.getOrderId());
            afterSaleItem.setSkuCode(orderItem.getSkuCode());
            afterSaleItem.setProductName(orderItem.getProductName());
            afterSaleItem.setProductImg(orderItem.getProductImg());
            afterSaleItem.setReturnQuantity(orderItem.getSaleQuantity());
            afterSaleItem.setOriginAmount(orderItem.getOriginAmount());
            afterSaleItem.setApplyRefundAmount(orderItem.getOriginAmount());
            afterSaleItem.setRealRefundAmount(orderItem.getPayAmount());
            //  取消订单 条目中的sku全部退货
            afterSaleItem.setReturnCompletionMark(AfterSaleReturnCompletionMarkEnum.ALL_RETURN_GOODS.getCode());
            afterSaleItem.setAfterSaleItemType(AfterSaleItemTypeEnum.AFTER_SALE_ORDER_ITEM.getCode());

            afterSaleItems.add(afterSaleItem);
        }
        return afterSaleItems;
    }

    private AfterSaleLogDO buildAfterSaleLog(String afterSaleId, Integer fromStatus, Integer toStatus, Integer cancelType) {
        AfterSaleLogDO afterSaleLogDO = new AfterSaleLogDO();
        afterSaleLogDO.setAfterSaleId(afterSaleId);
        afterSaleLogDO.setPreStatus(fromStatus);
        afterSaleLogDO.setCurrentStatus(toStatus);

        // 取消订单类型
        OrderCancelTypeEnum orderCancelTypeEnum = OrderCancelTypeEnum.getByCode(cancelType);
        if (orderCancelTypeEnum != null) {
            afterSaleLogDO.setRemark(orderCancelTypeEnum.getName());
        }
        return afterSaleLogDO;
    }

    private AfterSaleRefundDO buildAfterSaleRefund(AfterSaleInfoDO afterSaleInfo) {
        String orderId = afterSaleInfo.getOrderId();

        AfterSaleRefundDO afterSaleRefundDO = new AfterSaleRefundDO();
        afterSaleRefundDO.setAfterSaleId(afterSaleInfo.getAfterSaleId());
        afterSaleRefundDO.setOrderId(orderId);
        afterSaleRefundDO.setAccountType(AccountTypeEnum.THIRD.getCode());
        afterSaleRefundDO.setRefundStatus(RefundStatusEnum.UN_REFUND.getCode());
        afterSaleRefundDO.setRemark(RefundStatusEnum.UN_REFUND.getName());
        afterSaleRefundDO.setRefundAmount(afterSaleInfo.getRealRefundAmount());
        afterSaleRefundDO.setAfterSaleBatchNo(orderId + RandomHelper.getString(10, RandomHelper.DIST_NUMBER));

        OrderPaymentDetailDO paymentDetail = orderPaymentDetailMapper.selectOneByOrderId(orderId);
        if (paymentDetail != null) {
            afterSaleRefundDO.setOutTradeNo(paymentDetail.getOutTradeNo());
            afterSaleRefundDO.setPayType(paymentDetail.getPayType());
        }

        return afterSaleRefundDO;
    }
}
