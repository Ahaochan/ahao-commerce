package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.cancel;

import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import org.springframework.stereotype.Component;

/**
 * 订单履约手动取消Action
 */
@Component
public class OrderFulfilledManualCancelAction extends AbstractOrderCancelAction {
    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_FULFILLED_MANUAL_CANCELLED;
    }
}
