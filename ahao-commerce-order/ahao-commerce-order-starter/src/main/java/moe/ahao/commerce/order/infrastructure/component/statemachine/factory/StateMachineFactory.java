package moe.ahao.commerce.order.infrastructure.component.statemachine.factory;

import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;

/**
 * 状态机工厂
 */
@Component
public class StateMachineFactory {
    private final UntypedStateMachineBuilder orderStateMachineBuilder;
    private final UntypedStateMachineBuilder afterSaleStateMachineBuilder;

    @Autowired
    private ApplicationContext applicationContext;

    // 初始化的逻辑
    private StateMachineFactory() {
        // 1. 创建订单状态机的构造器Builder, 用squirrel框架来驱动自研流程编排框架
        this.orderStateMachineBuilder = OrderStateMachine.builder();
        // 2. 创建售后单状态机的构造器Builder, 用squirrel框架来驱动自研流程编排框架
        this.afterSaleStateMachineBuilder = AfterSaleStateMachine.builder();
    }

    /**
     * 获取订单状态机
     *
     * @param initState 初始状态
     * @return 状态机
     */
    public OrderStateMachine getOrderStateMachine(OrderStatusEnum initState) {
        // 使用状态机构造器Builder构造一个初始状态为initState的状态机, 用于后续状态的扭转
        OrderStateMachine orderStateMachine = this.orderStateMachineBuilder.newUntypedStateMachine(initState);
        // 设置状态扭转的业务逻辑Factory
        orderStateMachine.setApplicationContext(applicationContext);
        return orderStateMachine;
    }

    /**
     * 获取售后状态机
     *
     * @param initState 初始状态
     * @return 状态机
     */
    public AfterSaleStateMachine getAfterSaleStateMachine(AfterSaleStatusEnum initState) {
        AfterSaleStateMachine afterSaleStateMachine = this.afterSaleStateMachineBuilder.newUntypedStateMachine(initState);
        afterSaleStateMachine.setApplicationContext(applicationContext);
        return afterSaleStateMachine;
    }
}
