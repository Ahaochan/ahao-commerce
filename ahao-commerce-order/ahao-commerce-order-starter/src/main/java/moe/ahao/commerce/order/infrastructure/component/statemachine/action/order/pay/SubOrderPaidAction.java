package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.pay;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.OrderOperateTypeEnum;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.OrderStateAction;
import moe.ahao.commerce.order.infrastructure.enums.PayStatusEnum;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 子订单已支付Action
 */
@Slf4j
@Component
public class SubOrderPaidAction extends OrderStateAction<OrderInfoDO> {
    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.SUB_ORDER_PAID;
    }

    @Override
    protected String onStateChangeInternal(OrderStatusChangeEnum event, OrderInfoDO subOrderInfo) {
        Date payTime = subOrderInfo.getPayTime();

        // 1. 更新子订单状态和支付时间
        Integer fromStatus = subOrderInfo.getOrderStatus();
        Integer toStatus = OrderStatusEnum.PAID.getCode();
        String subOrderId = subOrderInfo.getOrderId();
        List<String> subOrderIdList = Collections.singletonList(subOrderId);
        super.updateOrderStatusAndPayTime(subOrderIdList, fromStatus, toStatus, payTime);

        // 2. 更新子订单的支付明细
        super.updatePaymentStatusAndPayTime(subOrderIdList, PayStatusEnum.PAID.getCode(), payTime);

        // 3. 保存子订单操作日志
        OrderOperateTypeEnum operateType = OrderOperateTypeEnum.PAID_ORDER;
        String remark = "订单支付回调操作，子订单状态变更" + fromStatus + "-" + toStatus;
        super.saveOrderOperateLog(subOrderId, operateType, fromStatus, toStatus, remark);

        return subOrderId;
    }
}
