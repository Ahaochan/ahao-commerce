package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.create.node;

import moe.ahao.commerce.common.infrastructure.event.PayOrderTimeoutEvent;
import moe.ahao.commerce.order.infrastructure.component.OrderDataBuilder;
import moe.ahao.commerce.order.infrastructure.publisher.OrderEventPublisher;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.StandardProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateOrderSendPayOrderTimeoutEventNode extends StandardProcessor {

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Override
    protected void processInternal(ProcessContext processContext) {
        OrderDataBuilder.OrderData orderData = processContext.get("orderData");
        String orderId = orderData.getOrderInfo().getOrderId();

        PayOrderTimeoutEvent event = new PayOrderTimeoutEvent();
        event.setOrderId(orderId);
        orderEventPublisher.sendPayOrderTimeoutEvent(event);
    }
}
