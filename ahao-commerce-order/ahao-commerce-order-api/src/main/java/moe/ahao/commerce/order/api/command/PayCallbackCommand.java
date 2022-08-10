package moe.ahao.commerce.order.api.command;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付系统回调请求对象
 */
@Data
public class PayCallbackCommand {
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 支付账户
     */
    private String payAccount;
    /**
     * 支付金额
     */
    private BigDecimal payAmount;
    /**
     * 支付系统交易单号
     */
    private String outTradeNo;
    /**
     * 支付方式
     */
    private Integer payType;
    /**
     * 支付时间
     */
    private Date payTime;
    /**
     * 商户号
     */
    private String merchantId;
    /**
     * 支付渠道
     */
    private String payChannel;
    /**
     * 微信平台 appid
     */
    private String appid;
}
