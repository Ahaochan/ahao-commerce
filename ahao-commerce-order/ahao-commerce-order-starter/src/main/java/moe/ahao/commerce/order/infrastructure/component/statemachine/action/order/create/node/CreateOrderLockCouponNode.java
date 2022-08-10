package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.create.node;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.market.api.command.LockUserCouponCommand;
import moe.ahao.commerce.market.api.command.ReleaseUserCouponCommand;
import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.infrastructure.gateway.CouponGateway;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.RollbackProcessor;
import moe.ahao.tend.consistency.core.annotation.ConsistencyTask;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 创建订单锁定优惠券节点
 */
@Slf4j
@Component
public class CreateOrderLockCouponNode extends RollbackProcessor {
    @Autowired
    private CouponGateway couponGateway;

    @Override
    protected void processInternal(ProcessContext processContext) {
        CreateOrderCommand command = processContext.get("createOrderCommand");
        this.lockUserCoupon(command);
    }

    private void lockUserCoupon(CreateOrderCommand createOrderCommand) {
        String userId = createOrderCommand.getUserId();
        String orderId = createOrderCommand.getOrderId();
        String couponId = createOrderCommand.getCouponId();
        if (StringUtils.isEmpty(couponId)) {
            log.info("下单流程, 用户{}的订单编号{}没有使用优惠券{}, 不锁定优惠券", userId, orderId, couponId);
            return;
        }

        LockUserCouponCommand command = new LockUserCouponCommand();
        command.setBusinessIdentifier(command.getBusinessIdentifier());
        command.setOrderId(orderId);
        command.setUserId(userId);
        command.setSellerId(command.getSellerId());
        command.setCouponId(couponId);
        couponGateway.lock(command);
    }

    @Override
    protected void rollback(ProcessContext processContext) {
        CreateOrderCommand createOrderCommand = processContext.get("createOrderCommand");
        String userId = createOrderCommand.getUserId();
        String orderId = createOrderCommand.getOrderId();
        String couponId = createOrderCommand.getCouponId();
        if (StringUtils.isEmpty(couponId)) {
            log.info("下单流程, 用户{}的订单编号{}没有使用优惠券{}, 不释放优惠券", userId, orderId, couponId);
            return;
        }

        ReleaseUserCouponCommand command = new ReleaseUserCouponCommand();
        command.setUserId(userId);
        command.setCouponId(couponId);
        command.setOrderId(orderId);
        command.setAfterSaleId(null);

        this.releaseUserCoupon(command);
    }

    /**
     * 一致性框架只能拦截public方法
     */
    @ConsistencyTask(id = "rollbackLockCoupon", alertActionBeanName = "tendConsistencyAlerter")
    public void releaseUserCoupon(ReleaseUserCouponCommand command) {
        couponGateway.releaseUserCoupon(command);
    }
}
