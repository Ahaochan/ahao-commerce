package moe.ahao.commerce.order.infrastructure.component.statemachine.action.aftersale;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.RefundOrderCallbackCommand;
import moe.ahao.commerce.aftersale.infrastructure.enums.RefundStatusEnum;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleLogDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleRefundDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleLogMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleRefundMapper;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.AfterSaleOperateTypeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.AfterSaleStateAction;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.exception.CommonBizExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * 售后支付回调退款Action
 */
@Component
@Slf4j
public class RefundPayCallbackAction extends AfterSaleStateAction<RefundOrderCallbackCommand> {
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;
    @Autowired
    private AfterSaleRefundMapper afterSaleRefundMapper;
    @Autowired
    private AfterSaleLogMapper afterSaleLogMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public AfterSaleStatusChangeEnum event() {
        return AfterSaleStatusChangeEnum.AFTER_SALE_REFUNDED;
    }

    @Override
    protected String onStateChangeInternal(AfterSaleStatusChangeEnum event, RefundOrderCallbackCommand command) {
        // 1. 入参校验
        this.check(command);

        // 2. 加分布式锁
        String afterSaleId = command.getAfterSaleId();
        String lockKey = RedisLockKeyConstants.REFUND_KEY + afterSaleId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw OrderExceptionEnum.PROCESS_PAY_REFUND_CALLBACK_REPEAT.msg();
        }

        try {
            // 3. 进行退款逻辑处理
            this.doRefundCallback(command);

            // 非退款成功 流程结束
            if (!Objects.equals(RefundStatusEnum.REFUND_SUCCESS.getCode(), command.getRefundStatus())) {
                return afterSaleId;
            }

            // 4. 发短信
            this.sendRefundMobileMessage(afterSaleId);

            // 5. 发APP通知
            this.sendRefundAppMessage(afterSaleId);

            return afterSaleId;
        } catch (Exception e) {
            log.error("退款回调处理失败", e);
            throw OrderExceptionEnum.PROCESS_PAY_REFUND_CALLBACK_FAILED.msg();
        } finally {
            lock.unlock();
        }
    }

    private void check(RefundOrderCallbackCommand command) {
        if (command == null) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }

        // 未退款
        if (Objects.equals(RefundStatusEnum.UN_REFUND.getCode(), command.getRefundStatus())) {
            throw OrderExceptionEnum.PAY_REFUND_CALLBACK_STATUS_FAILED.msg();
        }

        String orderId = command.getOrderId();
        if (StringUtils.isEmpty(orderId)) {
            throw OrderExceptionEnum.CANCEL_ORDER_ID_IS_NULL.msg();
        }

        String batchNo = command.getBatchNo();
        if (StringUtils.isEmpty(batchNo)) {
            throw OrderExceptionEnum.PROCESS_PAY_REFUND_CALLBACK_BATCH_NO_IS_NULL.msg();
        }

        Integer refundStatus = command.getRefundStatus();
        if (refundStatus == null) {
            throw OrderExceptionEnum.PROCESS_PAY_REFUND_CALLBACK_STATUS_NO_IS_NULL.msg();
        }

        BigDecimal refundFee = command.getRefundFee();
        if (refundFee == null) {
            throw OrderExceptionEnum.PROCESS_PAY_REFUND_CALLBACK_FEE_NO_IS_NULL.msg();
        }

        BigDecimal totalFee = command.getTotalFee();
        if (totalFee == null) {
            throw OrderExceptionEnum.PROCESS_PAY_REFUND_CALLBACK_TOTAL_FEE_NO_IS_NULL.msg();
        }

        String sign = command.getSign();
        if (StringUtils.isEmpty(sign)) {
            throw OrderExceptionEnum.PROCESS_PAY_REFUND_CALLBACK_SIGN_NO_IS_NULL.msg();
        }

        String tradeNo = command.getTradeNo();
        if (StringUtils.isEmpty(tradeNo)) {
            throw OrderExceptionEnum.PROCESS_PAY_REFUND_CALLBACK_TRADE_NO_IS_NULL.msg();
        }

        String afterSaleId = command.getAfterSaleId();
        if (StringUtils.isEmpty(afterSaleId)) {
            throw OrderExceptionEnum.PROCESS_PAY_REFUND_CALLBACK_AFTER_SALE_ID_IS_NULL.msg();
        }

        Date refundTime = command.getRefundTime();
        if (refundTime == null) {
            throw OrderExceptionEnum.PROCESS_PAY_REFUND_CALLBACK_AFTER_SALE_REFUND_TIME_IS_NULL.msg();
        }
    }

    public Boolean doRefundCallback(RefundOrderCallbackCommand command) {
        // @Transactional无法生效，需要用编程式事务
        return transactionTemplate.execute(transactionStatus -> {
            String orderId = command.getOrderId();
            String afterSaleId = command.getAfterSaleId();

            // 1. 数据库中当前售后单不是未退款状态, 表示已经退款成功 or 失败, 那么本次就不能再执行支付回调退款
            AfterSaleRefundDO afterSaleByDatabase = afterSaleRefundMapper.selectOneByAfterSaleId(afterSaleId);
            if (!Objects.equals(RefundStatusEnum.UN_REFUND.getCode(), afterSaleByDatabase.getRefundStatus())) {
                throw OrderExceptionEnum.REPEAT_CALLBACK.msg();
            }

            // 2. 更新售后记录，支付退款回调更新售后信息
            this.updatePaymentRefundCallbackAfterSale(command);

            return true;
        });
    }

    /**
     * 更新支付退款回调售后信息
     */
    public void updatePaymentRefundCallbackAfterSale(RefundOrderCallbackCommand command) {
        String afterSaleId = command.getAfterSaleId();
        AfterSaleInfoDO afterSaleInfoDO = afterSaleInfoMapper.selectOneByAfterSaleId(afterSaleId);

        // 1. 解析回调参数, 准备更新的数据
        int toStatus;
        RefundStatusEnum refundStatusEnum;
        AfterSaleOperateTypeEnum operateTypeEnum;
        if (Objects.equals(RefundStatusEnum.REFUND_SUCCESS.getCode(), command.getRefundStatus())) {
            toStatus = AfterSaleStatusEnum.REFUNDED.getCode();
            refundStatusEnum = RefundStatusEnum.REFUND_SUCCESS;
            operateTypeEnum = AfterSaleOperateTypeEnum.AFTER_SALE_REFUNDED;
        } else {
            toStatus = AfterSaleStatusEnum.FAILED.getCode();
            refundStatusEnum = RefundStatusEnum.REFUND_FAIL;
            operateTypeEnum = AfterSaleOperateTypeEnum.AFTER_SALE_REFUND_FAIL;
        }

        // 1. 更新 订单售后表
        int fromStatus = AfterSaleStatusEnum.REFUNDING.getCode();
        afterSaleInfoMapper.updateAfterSaleStatusByAfterSaleId(afterSaleId, fromStatus, toStatus);

        // 2. 新增 售后单变更表
        AfterSaleLogDO afterSaleLogDO = new AfterSaleLogDO();
        afterSaleLogDO.setAfterSaleId(afterSaleId);
        afterSaleLogDO.setOrderId(afterSaleInfoDO.getOrderId());
        afterSaleLogDO.setPreStatus(fromStatus);
        afterSaleLogDO.setCurrentStatus(toStatus);
        afterSaleLogDO.setRemark(operateTypeEnum.getMsg());
        afterSaleLogMapper.insert(afterSaleLogDO);

        // 3. 更新 售后退款单表
        afterSaleRefundMapper.updateRefundInfoByAfterSaleId(afterSaleId, refundStatusEnum.getCode(), command.getRefundTime(), refundStatusEnum.getName());
    }

    /**
     * 发送退款短信
     */
    public void sendRefundMobileMessage(String orderId) {
        log.info("发退款通知短信, 订单号:{}", orderId);
    }

    /**
     * 发送退款短信
     */
    public void sendRefundAppMessage(String orderId) {
        log.info("发退款通知APP信息, 订单号:{}", orderId);
    }
}
