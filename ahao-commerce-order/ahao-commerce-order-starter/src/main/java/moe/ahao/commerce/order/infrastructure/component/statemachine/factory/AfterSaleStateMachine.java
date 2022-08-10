package moe.ahao.commerce.order.infrastructure.component.statemachine.factory;

import lombok.Setter;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.AfterSaleStateAction;
import org.springframework.context.ApplicationContext;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;
import org.squirrelframework.foundation.fsm.annotation.StateMachineParameters;

import java.util.Map;

/**
 * 售后状态机
 */
@StateMachineParameters(stateType = AfterSaleStatusEnum.class, eventType = AfterSaleStatusChangeEnum.class, contextType = Object.class)
public class AfterSaleStateMachine extends AbstractStateMachine<AfterSaleStatusEnum, AfterSaleStatusChangeEnum> {
    @Setter
    private ApplicationContext applicationContext;

    @Override
    public void onStateChangeInternal(AfterSaleStatusEnum fromStatus, AfterSaleStatusEnum toState, AfterSaleStatusChangeEnum event, Object context) {
        Map<String, AfterSaleStateAction> actionMap = applicationContext.getBeansOfType(AfterSaleStateAction.class);
        for (AfterSaleStateAction<?> action : actionMap.values()) {
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
        UntypedStateMachineBuilder stateMachineBuilder = StateMachineBuilderFactory.create(AfterSaleStateMachine.class);

        // 2. 定义状态机的状态扭转配置: 由event事件触发, 将状态从from扭转为to, 并回调onStateChange方法
        //    AfterSaleStatusChangeEnum包含了售后单状态流转所有的事件，每个事件都会导致售后单状态流转
        for (AfterSaleStatusChangeEnum event : AfterSaleStatusChangeEnum.values()) {
            stateMachineBuilder.externalTransition()
                .from(event.getFromStatus())
                .to(event.getToStatus())
                .on(event)
                .callMethod(AbstractStateMachine.CALL_METHOD);
        }

        return stateMachineBuilder;
    }
}
