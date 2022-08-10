package moe.ahao.commerce.common.event;

import lombok.Data;

@Data
public class ActualRefundEvent {
    /**
     * 售后id
     */
    private String afterSaleId;
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 区分执行实际退款的消息类型: 1：取消订单整笔退款 or 2.发起售后退货
     */
    private Integer afterSaleType;
}
