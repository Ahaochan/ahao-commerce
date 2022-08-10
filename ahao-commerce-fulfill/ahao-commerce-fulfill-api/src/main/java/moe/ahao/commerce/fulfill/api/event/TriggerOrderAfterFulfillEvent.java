package moe.ahao.commerce.fulfill.api.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;

/**
 * 触发订单物流配送结果事件请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TriggerOrderAfterFulfillEvent {
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 履约单id
     */
    private String fulfillId;
    /**
     * 订单状态变更
     */
    private OrderStatusChangeEnum orderStatusChange;
    /**
     * 物流配送结果事件消息体
     */
    private BaseAfterFulfillEvent wmsEvent;
}
