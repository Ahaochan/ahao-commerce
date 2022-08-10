package moe.ahao.commerce.order.infrastructure.component.statemachine.factory;

import lombok.Setter;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.OrderStateAction;
import org.springframework.context.ApplicationContext;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;
import org.squirrelframework.foundation.fsm.annotation.StateMachineParameters;

import java.util.Map;

/**
 * 订单状态机
 */
@StateMachineParameters(stateType = OrderStatusEnum.class, eventType = OrderStatusChangeEnum.class, contextType = Object.class)
public class OrderStateMachine extends AbstractStateMachine<OrderStatusEnum, OrderStatusChangeEnum> {
    @Setter
    private ApplicationContext applicationContext;

    @Override
    public void onStateChangeInternal(OrderStatusEnum fromStatus, OrderStatusEnum toState, OrderStatusChangeEnum event, Object context) {
        Map<String, OrderStateAction> actionMap = applicationContext.getBeansOfType(OrderStateAction.class);
        for (OrderStateAction<?> action : actionMap.values()) {
            if (action.event() == null) {
                throw new IllegalArgumentException("event 返回值不能为空：" + action.getClass().getSimpleName());
            }
            if (action.event().equals(event)) {
                action.onStateChange(event, context);
                return;
            }
        }
    }

    public static UntypedStateMachineBuilder builder() {
        // 1. 创建状态机的构造器Builder, 用squirrel框架来驱动自研流程编排框架
        UntypedStateMachineBuilder stateMachineBuilder = StateMachineBuilderFactory.create(OrderStateMachine.class);

        // 2. 定义状态机的状态扭转配置: 由event事件触发, 将状态从from扭转为to, 并回调onStateChange方法
        //    OrderStatusChangeEnum包含了订单状态流转所有的事件，每个事件都会导致订单状态流转
        for (OrderStatusChangeEnum event : OrderStatusChangeEnum.values()) {
            stateMachineBuilder.externalTransition()
                .from(event.getFromStatus())
                .to(event.getToStatus())
                .on(event)
                .callMethod(CALL_METHOD);
        }

        return stateMachineBuilder;
    }
}
