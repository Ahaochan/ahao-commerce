package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.cancel;

import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import org.springframework.stereotype.Component;

/**
 * 订单未支付超时自动取消Action
 */
@Component
public class OrderUnPaidAutoTimeoutCancelAction extends AbstractOrderCancelAction {
    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_UN_PAID_AUTO_TIMEOUT_CANCELLED;
    }
}
