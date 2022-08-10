package moe.ahao.commerce.fulfill.api.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单已配送物流结果消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDeliveredEvent extends BaseAfterFulfillEvent {
    /**
     * 配送员code
     */
    private String delivererNo;
    /**
     * 配送员姓名
     */
    private String delivererName;
    /**
     * 配送员手机号
     */
    private String delivererPhone;
}
