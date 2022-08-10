package moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data;

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
@TableName(AfterSaleRefundDO.TABLE_NAME)
public class AfterSaleRefundDO extends BaseDO {
    public static final String TABLE_NAME = "after_sale_refund";
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 售后退款id
     */
    private String afterSaleRefundId;
    /**
     * 售后id
     */
    private String afterSaleId;
    /**
     * 订单id
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
