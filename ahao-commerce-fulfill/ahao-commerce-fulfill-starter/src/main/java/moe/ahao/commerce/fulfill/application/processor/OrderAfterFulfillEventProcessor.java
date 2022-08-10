package moe.ahao.commerce.fulfill.application.processor;

import moe.ahao.commerce.fulfill.api.event.TriggerOrderAfterFulfillEvent;

/**
 * 订单物流配送结果处理器
 */
public interface OrderAfterFulfillEventProcessor {
    /**
     * 执行
     */
    void execute(TriggerOrderAfterFulfillEvent event);
}
