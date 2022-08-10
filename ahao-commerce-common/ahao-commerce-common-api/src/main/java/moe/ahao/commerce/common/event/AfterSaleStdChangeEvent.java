package moe.ahao.commerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.ahao.commerce.common.enums.*;

/**
 * 售后单标准变更消息事件
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfterSaleStdChangeEvent {

    /**
     * 接入方业务标识
     */
    private BusinessIdentifierEnum businessIdentifier;

    /**
     * 售后id
     */
    private String afterSaleId;

    /**
     * 订单号
     */
    private String orderId;
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 订单类型 1:一般订单  255:其它
     */
    private OrderTypeEnum orderType;

    /**
     * 售后类型
     */
    private AfterSaleTypeEnum afterSaleType;

    /**
     * 售后类型详情枚举
     */
    private AfterSaleTypeDetailEnum afterSaleTypeDetail;

    /**
     * 售后申请来源
     */
    private AfterSaleApplySourceEnum applySource;

    /**
     * 申请售后时间 yyyy-MM-dd HH:mm:ss
     */
    private String applyTime;

    /**
     * 申请原因
     */
    private AfterSaleReasonEnum applyReason;

    /**
     * 审核时间 yyyy-MM-dd HH:mm:ss
     */
    private String reviewTime;

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
     * 售后单状态变更枚举
     */
    private AfterSaleStatusChangeEnum statusChange;

    /**
     * 申请退款金额
     */
    private Integer applyRefundAmount;

    /**
     * 实际退款金额
     */
    private Integer realRefundAmount;

    /**
     * 备注
     */
    private String remark;

}
