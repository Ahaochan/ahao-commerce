package moe.ahao.commerce.fulfill.api.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 订单已签收物流结果消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderSignedEvent extends BaseAfterFulfillEvent {
    /**
     * 签收事件
     */
    private Date signedTime;
    /**
     * 履约id
     */
    private String fulfillId;
}
