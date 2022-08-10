package moe.ahao.commerce.common.infrastructure.event;

import lombok.Data;

/**
 * 订单支付超时自定取消订单延迟消息
 */
@Data
public class PayOrderTimeoutEvent {
    /**
     * 订单id
     */
    private String orderId;
}
