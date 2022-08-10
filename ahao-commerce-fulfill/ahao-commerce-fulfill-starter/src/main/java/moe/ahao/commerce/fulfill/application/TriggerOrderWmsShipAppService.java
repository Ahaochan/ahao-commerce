package moe.ahao.commerce.fulfill.application;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.fulfill.api.event.TriggerOrderAfterFulfillEvent;
import moe.ahao.commerce.fulfill.application.processor.OrderDeliveredEventProcessor;
import moe.ahao.commerce.fulfill.application.processor.OrderOutStockEventProcessor;
import moe.ahao.commerce.fulfill.application.processor.OrderSignedEventProcessor;
import moe.ahao.commerce.fulfill.application.processor.OrderAfterFulfillEventProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TriggerOrderWmsShipAppService implements ApplicationContextAware {
    @Setter
    private ApplicationContext applicationContext;

    public boolean trigger(TriggerOrderAfterFulfillEvent event) {
        // 1. 获取处理器
        OrderStatusChangeEnum orderStatusChange = event.getOrderStatusChange();
        OrderAfterFulfillEventProcessor processor = this.getWmsShipEventProcessor(orderStatusChange);

        // 2. 执行
        if (processor != null) {
            processor.execute(event);
        }

        return true;
    }

    /**
     * 订单物流配送结果处理器
     */
    private OrderAfterFulfillEventProcessor getWmsShipEventProcessor(OrderStatusChangeEnum orderStatusChange) {
        if (OrderStatusChangeEnum.ORDER_OUT_STOCKED.equals(orderStatusChange)) {
            // 订单已出库事件
            return applicationContext.getBean(OrderOutStockEventProcessor.class);
        } else if (OrderStatusChangeEnum.ORDER_DELIVERED.equals(orderStatusChange)) {
            // 订单已配送事件
            return applicationContext.getBean(OrderDeliveredEventProcessor.class);
        } else if (OrderStatusChangeEnum.ORDER_SIGNED.equals(orderStatusChange)) {
            // 订单已签收事件
            return applicationContext.getBean(OrderSignedEventProcessor.class);
        }
        return null;
    }
}
