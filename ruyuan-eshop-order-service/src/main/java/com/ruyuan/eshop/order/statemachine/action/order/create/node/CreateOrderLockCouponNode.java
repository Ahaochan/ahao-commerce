package com.ruyuan.eshop.order.statemachine.action.order.create.node;

import com.ruyuan.consistency.annotation.ConsistencyTask;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.market.domain.request.LockUserCouponRequest;
import com.ruyuan.eshop.market.domain.request.ReleaseUserCouponRequest;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.domain.request.CreateOrderRequest;
import com.ruyuan.eshop.order.remote.MarketRemote;
import com.ruyuan.process.engine.process.ProcessContext;
import com.ruyuan.process.engine.process.RollbackProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 创建订单锁定优惠券节点
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
public class CreateOrderLockCouponNode extends RollbackProcessor {

    @Autowired
    private OrderConverter orderConverter;

    @Autowired
    private MarketRemote marketRemote;

    @Override
    protected void rollback(ProcessContext processContext) {
        CreateOrderRequest createOrderRequest = processContext.get("createOrderRequest");
        ReleaseUserCouponRequest releaseUserCouponRequest = new ReleaseUserCouponRequest();
        releaseUserCouponRequest.setCouponId(createOrderRequest.getCouponId());
        releaseUserCouponRequest.setUserId(createOrderRequest.getUserId());
        releaseUserCouponRequest.setOrderId(createOrderRequest.getOrderId());

        doRollback(releaseUserCouponRequest);
    }

    @Override
    protected void processInternal(ProcessContext processContext) {
        CreateOrderRequest createOrderRequest = processContext.get("createOrderRequest");
        ParamCheckUtil.checkObjectNonNull(createOrderRequest);
        String couponId = createOrderRequest.getCouponId();
        if (StringUtils.isEmpty(couponId)) {
            return;
        }
        LockUserCouponRequest lockUserCouponRequest = orderConverter.convertLockUserCouponRequest(createOrderRequest);
        // 调用营销服务锁定用户优惠券
        marketRemote.lockUserCoupon(lockUserCouponRequest);
    }

    /**
     * 一致性框架只能拦截public方法
     */
    @ConsistencyTask(id = "rollbackLockCoupon", alertActionBeanName = "tendConsistencyAlerter")
    public void doRollback(ReleaseUserCouponRequest releaseUserCouponRequest) {
        marketRemote.releaseUserCoupon(releaseUserCouponRequest);
    }
}
