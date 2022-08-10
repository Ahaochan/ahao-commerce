package moe.ahao.commerce.pay.api.command;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class RefundCallbackCommand {
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 退款批次号
     */
    private Integer batchNo;
    /**
     * 支付接口返回来的退款结果状态 10 未退款  20 退款成功 30 退款失败
     */
    private Integer refundStatus;
    /**
     * 退款费用
     */
    private BigDecimal refundFee;
    /**
     * 退款总额
     */
    private BigDecimal totalFee;
    /**
     * 支付退款签名
     */
    private String sign;
    /**
     * 交易流水号
     */
    private String tradeNo;
    /**
     * 支付退款回调时间
     */
    private Date refundTime;
}
