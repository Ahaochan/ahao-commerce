package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.afterfulfill;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import org.springframework.stereotype.Component;

/**
 * 订单已履约Action
 */
@Slf4j
@Component
public class OrderFulFillAction extends AbstractOrderFulfillAction {

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_FULFILLED;
    }

    @Override
    protected OrderStatusEnum handleStatus() {
        return OrderStatusEnum.PAID;
    }
}
