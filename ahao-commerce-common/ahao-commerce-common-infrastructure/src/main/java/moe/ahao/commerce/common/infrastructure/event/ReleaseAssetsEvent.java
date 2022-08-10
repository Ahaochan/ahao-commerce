package moe.ahao.commerce.common.infrastructure.event;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 取消订单释放资产
 */
@Data
public class ReleaseAssetsEvent {
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 买家id
     */
    private String userId;
    /**
     * 使用的优惠券id
     */
    private String couponId;
}
