package moe.ahao.commerce.customer.api.command;

import lombok.Data;

/**
 * 客服审核退货申请入参
 */
@Data
public class CustomerReviewReturnGoodsCommand {
    /**
     * 售后id
     */
    private String afterSaleId;
    /**
     * 客服id
     */
    private String customerId;
    /**
     * 审核结果 1 审核通过  2 审核拒绝
     */
    private Integer auditResult;
    /**
     * 客服审核结果描述信息
     */
    private String auditResultDesc;
}
