package moe.ahao.commerce.aftersale.api.command;

import lombok.Data;

/**
 * 取消订单入参
 */
@Data
public class CancelOrderCommand {
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 订单取消类型 0-手动取消 1-超时未支付
     */
    private Integer cancelType;
}
