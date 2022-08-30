package com.ruyuan.eshop.order.statemachine;

import com.ruyuan.eshop.common.enums.AfterSaleStateMachineChangeEnum;
import com.ruyuan.eshop.common.enums.AfterSaleStatusEnum;
import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.common.enums.OrderStatusEnum;
import com.ruyuan.eshop.common.exception.BaseBizException;
import com.ruyuan.eshop.order.statemachine.action.AfterSaleActionFactory;
import com.ruyuan.eshop.order.statemachine.action.AfterSaleStateAction;
import com.ruyuan.eshop.order.statemachine.action.OrderActionFactory;
import com.ruyuan.eshop.order.statemachine.action.OrderStateAction;
import org.springframework.stereotype.Component;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;
import org.squirrelframework.foundation.fsm.annotation.StateMachineParameters;
import org.squirrelframework.foundation.fsm.impl.AbstractUntypedStateMachine;

import javax.annotation.Resource;

/**
 * 状态机工厂
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
public class StateMachineFactory {

    private final UntypedStateMachineBuilder orderStateMachineBuilder;

    private final UntypedStateMachineBuilder afterSaleStateMachineBuilder;

    @Resource
    private OrderActionFactory orderActionFactory;

    @Resource
    private AfterSaleActionFactory afterSaleActionFactory;

    private StateMachineFactory() {
        this.orderStateMachineBuilder = StateMachineBuilderFactory.create(OrderStateMachine.class);
        for (OrderStatusChangeEnum event : OrderStatusChangeEnum.values()) {
            this.orderStateMachineBuilder.externalTransition().from(event.getFromStatus())
                    .to(event.getToStatus()).on(event).callMethod("onStateChange");
        }
        this.afterSaleStateMachineBuilder = StateMachineBuilderFactory.create(AfterSaleStateMachine.class);
        for (AfterSaleStateMachineChangeEnum event : AfterSaleStateMachineChangeEnum.values()) {
            this.afterSaleStateMachineBuilder.externalTransition().from(event.getFromStatus())
                    .to(event.getToStatus()).on(event).callMethod("onStateChange");
        }
    }

    /**
     * 获取订单状态机
     *
     * @param initState 初始状态
     * @return 状态机
     */
    public OrderStateMachine getOrderStateMachine(OrderStatusEnum initState) {
        StateMachineFactory.OrderStateMachine orderStateMachine = this.orderStateMachineBuilder.newUntypedStateMachine(initState);
        orderStateMachine.setOrderActionFactory(orderActionFactory);
        return orderStateMachine;
    }

    /**
     * 获取售后状态机
     *
     * @param initState 初始状态
     * @return 状态机
     */
    public AfterSaleStateMachine getAfterSaleStateMachine(AfterSaleStatusEnum initState) {
        StateMachineFactory.AfterSaleStateMachine afterSaleStateMachine = this.afterSaleStateMachineBuilder.newUntypedStateMachine(initState);
        afterSaleStateMachine.setOrderActionFactory(afterSaleActionFactory);
        return afterSaleStateMachine;
    }

    /**
     * 状态机父类
     */
    public static abstract class BaseStateMachine<S, E> extends AbstractUntypedStateMachine {
        private final ThreadLocal<Exception> exceptionThreadLocal = new ThreadLocal<>();

        /**
         * 状态流传
         */
        public void onStateChange(S fromStatus, S toState, E event, Object context) {
            try {
                onStateChangeInternal(fromStatus, toState, event, context);
            } catch (Exception e) {
                exceptionThreadLocal.set(e);
            }
        }

        /**
         * 正常情况下状态机：调用fire(event,context) 方法，会调用onStateChange方法。
         * <p>
         * 假如onStateChange方法抛出业务异常，这里会被状态机接管，然后使用一个Squirrel-Foundation内部的异常
         * TransitionException对我们的业务异常进行包装。然后抛出TransitionException异常。
         * <p>
         * 我们一般的情景：在SpringBoot中调用状态机开始状态流转，调用了fire方法，接着得到一个TransitionException异常，
         * 显然不是我们想要的结果。我们希望onStateChange方法抛出的如果是业务异常BaseBizException，则fire方法抛出的也是业务异常。
         * <p>
         * 所以这里采用了一种方式，在onStateChange方法中使用ThreadLocal将状态保存起来，
         * 那么fire方法就无法检测到我们实际业务代码是否抛出了异常，此时等fire方法返回的时候，我们再判断ThreadLocal中是否有异常，
         * 如果有就直接抛出，这样就可以实现我们所需要的结果。
         */
        @Override
        public void fire(Object event, Object context) {
            super.fire(event, context);
            Exception exception = exceptionThreadLocal.get();
            if (exception != null) {
                exceptionThreadLocal.remove();
                if (exception instanceof BaseBizException) {
                    throw (BaseBizException) exception;
                } else {
                    throw new RuntimeException(exception);
                }
            }
        }

        /**
         * 状态机装填流转核心逻辑
         */
        protected abstract void onStateChangeInternal(S fromStatus, S toState, E event, Object context);

    }

    /**
     * 订单状态机
     */
    @StateMachineParameters(stateType = OrderStatusEnum.class, eventType = OrderStatusChangeEnum.class, contextType = Object.class)
    public static class OrderStateMachine extends BaseStateMachine<OrderStatusEnum, OrderStatusChangeEnum> {
        private OrderActionFactory orderActionFactory;

        public void setOrderActionFactory(OrderActionFactory orderActionFactory) {
            this.orderActionFactory = orderActionFactory;
        }

        @Override
        public void onStateChange(OrderStatusEnum fromStatus, OrderStatusEnum toState, OrderStatusChangeEnum event, Object context) {
            super.onStateChange(fromStatus, toState, event, context);
        }

        @Override
        public void onStateChangeInternal(OrderStatusEnum fromStatus, OrderStatusEnum toState, OrderStatusChangeEnum event, Object context) {
            OrderStateAction<?> action = orderActionFactory.getAction(event);
            if (action != null) {
                action.onStateChange(event, context);
            }
        }
    }

    /**
     * 售后状态机
     */
    @StateMachineParameters(stateType = AfterSaleStatusEnum.class, eventType = AfterSaleStateMachineChangeEnum.class, contextType = Object.class)
    public static class AfterSaleStateMachine extends BaseStateMachine<AfterSaleStatusEnum, AfterSaleStateMachineChangeEnum> {
        private AfterSaleActionFactory afterSaleActionFactory;

        public void setOrderActionFactory(AfterSaleActionFactory afterSaleActionFactory) {
            this.afterSaleActionFactory = afterSaleActionFactory;
        }

        @Override
        public void onStateChange(AfterSaleStatusEnum fromStatus, AfterSaleStatusEnum toState, AfterSaleStateMachineChangeEnum event, Object context) {
            super.onStateChange(fromStatus, toState, event, context);
        }

        @Override
        public void onStateChangeInternal(AfterSaleStatusEnum fromStatus, AfterSaleStatusEnum toState, AfterSaleStateMachineChangeEnum event, Object context) {
            AfterSaleStateAction<?> action = afterSaleActionFactory.getAction(event);
            if (action != null) {
                action.onStateChange(event, context);
            }
        }
    }
}
