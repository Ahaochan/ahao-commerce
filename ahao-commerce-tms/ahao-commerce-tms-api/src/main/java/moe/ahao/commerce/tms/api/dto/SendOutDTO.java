package moe.ahao.commerce.tms.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发货结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOutDTO {
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 物流单号
     */
    private String logisticsCode;

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
