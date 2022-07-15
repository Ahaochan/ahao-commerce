package com.ruyuan.eshop.order.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import moe.ahao.domain.entity.BaseDO;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 售后退款单表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("after_sale_refund")
public class AfterSaleRefundDO extends BaseDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 售后退款单号
     */
    private String afterSaleRefundId;
    /**
     * 售后单号
     */
    private String afterSaleId;
    /**
     * 订单号
     */
    private String orderId;
    /**
     * 售后批次号
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
