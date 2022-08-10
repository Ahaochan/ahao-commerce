package moe.ahao.commerce.fulfill.api.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 订单已出库物流结果消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderOutStockEvent extends BaseAfterFulfillEvent {
    /**
     * 出库时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date outStockTime;
}
