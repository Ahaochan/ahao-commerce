package com.ruyuan.eshop.order.domain.request;

import com.ruyuan.eshop.order.domain.dto.AfterSaleInfoDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Data
public class CustomerAuditAssembleRequest implements Serializable {
    private static final long serialVersionUID = -2641506633636395602L;
    /**
     * 售后id
     */
    private String afterSaleId;
    /**
     * 订单id
     */
    private String orderId;

    /**
     * 客服审核时间
     */
    private Date reviewTime;

    /**
     * 客服审核来源
     */
    private Integer reviewSource;

    /**
     * 审核结果编码
     */
    private Integer reviewReasonCode;

    /**
     * 审核结果
     */
    private String reviewReason;

    /**
     * 客服审核结果描述信息
     */
    private String auditResultDesc;

    /**
     * 售后的skuCode
     */
    private String skuCode;

    /**
     * 售后单DTO
     */
    private AfterSaleInfoDTO afterSaleInfoDTO;
}
