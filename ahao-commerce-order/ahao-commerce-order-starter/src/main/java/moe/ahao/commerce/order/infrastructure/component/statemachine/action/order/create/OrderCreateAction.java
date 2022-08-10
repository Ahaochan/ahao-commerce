package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.create;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.OrderStateAction;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.wrapper.model.ProcessContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 创建订单Action
 */
@Slf4j
@Component
public class OrderCreateAction extends OrderStateAction<CreateOrderCommand> {
    @Autowired
    private ProcessContextFactory processContextFactory;

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_CREATED;
    }

    @Override
    protected String onStateChangeInternal(OrderStatusChangeEnum event, CreateOrderCommand command) {
        // 1. 通过流程编排引擎masterOrderCreateProcess去执行生单流程
        ProcessContext masterOrderCreateProcess = processContextFactory.getContext("masterOrderCreateProcess");
        masterOrderCreateProcess.set("createOrderCommand", command);
        masterOrderCreateProcess.start();

        // 2. 流程执行完之后, 获取返回参数
        return command.getOrderId();
    }

    @Override
    protected void postStateChange(OrderStatusChangeEnum event, String orderId) {
        // 3. 发送主单的状态变更消息
        super.postStateChange(event, orderId);

        // 拆单流程不应该通过状态机这里去执行
        // 应该通过最终一致性框架发送一个主单已生成的MQ消息出去, 异步拆单, 通过MQ的ACK机制保证拆单一定成功
    }
}
