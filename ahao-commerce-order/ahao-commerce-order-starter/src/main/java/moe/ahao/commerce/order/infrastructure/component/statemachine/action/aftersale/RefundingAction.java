package moe.ahao.commerce.order.infrastructure.component.statemachine.action.aftersale;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleLogDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleRefundDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleItemMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleLogMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleRefundMapper;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.AfterSaleItemTypeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleTypeEnum;
import moe.ahao.commerce.common.event.ActualRefundEvent;
import moe.ahao.commerce.market.api.command.ReleaseUserCouponCommand;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.AfterSaleStateAction;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.PayGateway;
import moe.ahao.commerce.order.infrastructure.publisher.OrderEventPublisher;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderPaymentDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderPaymentDetailMapper;
import moe.ahao.commerce.pay.api.command.RefundOrderCommand;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 实际退款Action
 */
@Component
@Slf4j
public class RefundingAction extends AfterSaleStateAction<ActualRefundEvent> {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderPaymentDetailMapper orderPaymentDetailMapper;
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;
    @Autowired
    private AfterSaleItemMapper afterSaleItemMapper;
    @Autowired
    private AfterSaleRefundMapper afterSaleRefundMapper;
    @Autowired
    private AfterSaleLogMapper afterSaleLogMapper;

    @Autowired
    private PayGateway payGateway;

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private OrderEventPublisher orderEventPublisher;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public AfterSaleStatusChangeEnum event() {
        return AfterSaleStatusChangeEnum.AFTER_SALE_REFUNDING;
    }

    @Override
    protected String onStateChangeInternal(AfterSaleStatusChangeEnum event, ActualRefundEvent actualRefundEvent) {
        // 1. 加分布式锁
        String afterSaleId = actualRefundEvent.getAfterSaleId();
        String lockKey = RedisLockKeyConstants.REFUND_KEY + afterSaleId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw OrderExceptionEnum.REFUND_MONEY_REPEAT.msg();
        }
        try {
            // 2. 调用支付服务进行退款, 并更新售后单状态为退款中
            this.refund(event, actualRefundEvent);
            return afterSaleId;
        } finally {
            lock.unlock();
        }
    }

    private String refund(AfterSaleStatusChangeEnum event, ActualRefundEvent actualRefundEvent) {
        // @Transactional无法生效，需要用编程式事务
        return transactionTemplate.execute(transactionStatus -> {
            String afterSaleId = actualRefundEvent.getAfterSaleId();
            String orderId = actualRefundEvent.getOrderId();
            Integer afterSaleType = actualRefundEvent.getAfterSaleType();

            AfterSaleInfoDO afterSaleInfo = afterSaleInfoMapper.selectOneByAfterSaleId(afterSaleId);
            AfterSaleRefundDO afterSaleRefund = afterSaleRefundMapper.selectOneByAfterSaleId(afterSaleId);
            OrderPaymentDetailDO orderPaymentDetail = orderPaymentDetailMapper.selectOneByOrderId(orderId);

            // 1. 调用支付服务进行退款
            RefundOrderCommand command = new RefundOrderCommand();
            command.setOrderId(orderId);
            command.setRefundAmount(afterSaleRefund.getRefundAmount());
            command.setAfterSaleId(afterSaleId);
            command.setOutTradeNo(orderPaymentDetail.getOutTradeNo());
            payGateway.executeRefund(command);

            // 2. 取消订单流程, 更新售后单状态后流程结束. 这里不需要释放优惠券, 因为取消订单的时候已经释放过一次了
            if (AfterSaleTypeEnum.RETURN_MONEY.getCode().equals(afterSaleType)) {
                this.updateAfterSaleStatus(afterSaleInfo, event);
                return afterSaleId;
            }

            // 4. 手动售后某一个sku, 如果这个sku是最后一笔, 会标记为退优惠券类型
            AfterSaleItemDO couponAfterSaleItem = afterSaleItemMapper.selectLastOne(orderId, afterSaleId, AfterSaleItemTypeEnum.AFTER_SALE_COUPON.getCode());
            if (couponAfterSaleItem == null) {
                // 没有使用优惠券, 说明这笔售后不是尾笔, 更新售后单状态后流程结束
                this.updateAfterSaleStatus(afterSaleInfo, event);
                return afterSaleId;
            }

            // 5. 手动售后某一个sku, 如果是退优惠券类型的售后单, 在更新完状态后, 发送一条释放优惠券的消息
            this.updateAfterSaleStatus(afterSaleInfo, event);
            this.sendReleaseCouponMq(afterSaleInfo);
            return afterSaleId;
        });
    }

    /**
     * 更新售后单状态
     */
    public void updateAfterSaleStatus(AfterSaleInfoDO afterSaleInfo, AfterSaleStatusChangeEnum event) {
        String afterSaleId = afterSaleInfo.getAfterSaleId();
        int fromStatus = event.getFromStatus().getCode();
        int toStatus = event.getToStatus().getCode();
        // 更新 订单售后表
        afterSaleInfoMapper.updateAfterSaleStatusByAfterSaleId(afterSaleId, fromStatus, toStatus);

        // 新增 售后单变更表
        AfterSaleLogDO afterSaleLog = new AfterSaleLogDO();
        afterSaleLog.setAfterSaleId(afterSaleId);
        afterSaleLog.setOrderId(afterSaleInfo.getOrderId());
        afterSaleLog.setPreStatus(fromStatus);
        afterSaleLog.setCurrentStatus(toStatus);
        afterSaleLog.setRemark(event.getOperateType().getMsg());
        log.info("保存售后变更记录,售后单号:{},fromStatus:{}, toStatus:{}", afterSaleId, fromStatus, toStatus);
        afterSaleLogMapper.insert(afterSaleLog);
    }

    private void sendReleaseCouponMq(AfterSaleInfoDO afterSaleInfo) {
        String orderId = afterSaleInfo.getOrderId();
        String afterSaleId = afterSaleInfo.getAfterSaleId();
        OrderInfoDO orderInfoDO = orderInfoMapper.selectOneByOrderId(orderId);

        ReleaseUserCouponCommand command = new ReleaseUserCouponCommand();
        command.setCouponId(orderInfoDO.getCouponId());
        command.setUserId(orderInfoDO.getUserId());
        command.setAfterSaleId(afterSaleId);

        orderEventPublisher.sendAfterSaleReleaseCouponMessage(command);
    }
}
