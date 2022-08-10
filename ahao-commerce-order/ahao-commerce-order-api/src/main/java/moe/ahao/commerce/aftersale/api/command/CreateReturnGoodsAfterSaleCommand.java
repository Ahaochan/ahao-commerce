package moe.ahao.commerce.aftersale.api.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 售后退货入参
 */
@Data
public class CreateReturnGoodsAfterSaleCommand {
    /**
     * 订单号
     */
    private String orderId;
    /**
     * 接入方业务线标识  1, "自营商城"
     */
    private Integer businessIdentifier;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 退货原因选项
     */
    private Integer returnGoodsCode;
    /**
     * 退货原因说明
     */
    private String returnGoodsDesc;
    /**
     * 退货sku编号
     */
    private String skuCode;
    /**
     * 退货数量
     */
    private BigDecimal returnQuantity;
}
