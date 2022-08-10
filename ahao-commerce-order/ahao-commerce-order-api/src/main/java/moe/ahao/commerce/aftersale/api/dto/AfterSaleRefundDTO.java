package moe.ahao.commerce.aftersale.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 售后支付表
 */
@Data
public class AfterSaleRefundDTO {
    /**
     * 售后单id
     */
    private String afterSaleId;
    /**
     * 售后支付单id
     */
    private String afterSaleRefundId;
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 售后批次编号
     */
    private String afterSaleBatchNo;
    /**
     * 账户类型
     */
    private Integer accountType;
    /**
     * 支付类型
     */
    private Integer payType;
    /**
     * 退款状态
     */
    private Integer refundStatus;
    /**
     * 退款金额
     */
    private BigDecimal refundAmount;
    /**
     * 退款支付时间
     */
    private Date refundPayTime;
    /**
     * 交易单号
     */
    private String outTradeNo;
    /**
     * 备注
     */
    private String remark;
}
