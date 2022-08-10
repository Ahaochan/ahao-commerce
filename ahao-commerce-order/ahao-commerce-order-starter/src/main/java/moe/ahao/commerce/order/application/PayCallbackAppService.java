package moe.ahao.commerce.order.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.order.api.command.PayCallbackCommand;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.OrderStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PayCallbackAppService {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    public void payCallback(PayCallbackCommand command) {
        // 状态机流转
        OrderStatusChangeEnum event = OrderStatusChangeEnum.ORDER_PAID;
        OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(event.getFromStatus());
        orderStateMachine.fire(event, command);
    }
}
