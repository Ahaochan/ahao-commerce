package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import moe.ahao.domain.entity.BaseDO;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单支付明细表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(OrderPaymentDetailDO.TABLE_NAME)
@NoArgsConstructor
public class OrderPaymentDetailDO extends BaseDO {
    public static final String TABLE_NAME = "order_payment_detail";
    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 账户类型
     */
    private Integer accountType;
    /**
     * 支付类型  10:微信支付, 20:支付宝支付
     */
    private Integer payType;
    /**
     * 支付状态 10:未支付,20:已支付
     */
    private Integer payStatus;
    /**
     * 支付金额
     */
    private BigDecimal payAmount;
    /**
     * 支付时间
     */
    private Date payTime;
    /**
     * 支付流水号
     */
    private String outTradeNo;
    /**
     * 支付备注信息
     */
    private String payRemark;

    public OrderPaymentDetailDO(OrderPaymentDetailDO that) {
        this.setId(that.id);
        this.setOrderId(that.orderId);
        this.setAccountType(that.accountType);
        this.setPayType(that.payType);
        this.setPayStatus(that.payStatus);
        this.setPayAmount(that.payAmount);
        this.setPayTime(that.payTime);
        this.setOutTradeNo(that.outTradeNo);
        this.setPayRemark(that.payRemark);
        this.setCreateBy(that.getCreateBy());
        this.setUpdateBy(that.getUpdateBy());
        this.setCreateTime(that.getCreateTime());
        this.setUpdateTime(that.getUpdateTime());
    }
}
