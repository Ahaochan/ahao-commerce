package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.cancel;

import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import org.springframework.stereotype.Component;

/**
 * 订单已支付手动取消Action
 */
@Component
public class OrderPaidManualCancelAction extends AbstractOrderCancelAction {

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_PAID_MANUAL_CANCELLED;
    }
}
