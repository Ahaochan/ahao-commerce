package moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import moe.ahao.domain.entity.BaseDO;

import java.math.BigDecimal;
import java.util.Date;

import static moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO.TABLE_NAME;

/**
 * 订单售后表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(AfterSaleInfoDO.TABLE_NAME)
public class AfterSaleInfoDO extends BaseDO {
    public static final String TABLE_NAME = "after_sale_info";
    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 售后id
     */
    private String afterSaleId;
    /**
     * 接入方业务标识
     */
    private Integer businessIdentifier;
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 购买用户id
     */
    private String userId;
    /**
     * 订单类型
     */
    private Integer orderType;
    /**
     * 申请售后来源
     */
    private Integer applySource;
    /**
     * 申请售后时间
     */
    private Date applyTime;
    /**
     * 申请原因编码
     */
    private Integer applyReasonCode;
    /**
     * 申请原因
     */
    private String applyReason;
    /**
     * 审核时间
     */
    private Date reviewTime;
    /**
     * 客服审核来源
     */
    private Integer reviewSource;
    /**
     * 客服审核结果编码
     */
    private Integer reviewReasonCode;
    /**
     * 客服审核结果
     */
    private String reviewReason;
    /**
     * 售后类型
     */
    private Integer afterSaleType;
    /**
     * 售后类型详情枚举
     */
    private Integer afterSaleTypeDetail;
    /**
     * 售后单状态
     */
    private Integer afterSaleStatus;
    /**
     * 申请退款金额
     */
    private BigDecimal applyRefundAmount;
    /**
     * 实际退款金额
     */
    private BigDecimal realRefundAmount;
    /**
     * 备注
     */
    private String remark;
}
