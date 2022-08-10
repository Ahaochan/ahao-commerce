package moe.ahao.commerce.market.api.command;

import lombok.Data;

/**
 * 释放优惠券入参
 */
@Data
public class ReleaseUserCouponCommand {
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 优惠券ID
     */
    private String couponId;
    /**
     * 订单ID
     */
    private String orderId;
    /**
     * 售后单ID
     */
    private String afterSaleId;

}
