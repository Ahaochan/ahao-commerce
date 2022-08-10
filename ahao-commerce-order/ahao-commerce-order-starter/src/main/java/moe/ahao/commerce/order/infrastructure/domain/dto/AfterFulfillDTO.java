package moe.ahao.commerce.order.infrastructure.domain.dto;

import lombok.Data;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;

import java.util.Date;

/**
 * 物流配送结果请求
 */
@Data
public class AfterFulfillDTO {
    /**
     * 订单编号
     */
    private String orderId;
    /**
     * 订单状态变更
     */
    private OrderStatusChangeEnum statusChange;
    /**
     * 出库时间
     */
    private Date outStockTime;
    /**
     * 签收时间
     */
    private Date signedTime;
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
