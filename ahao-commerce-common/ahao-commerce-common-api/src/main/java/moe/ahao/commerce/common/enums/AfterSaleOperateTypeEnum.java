package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 售后单操作类型枚举值
 */
@Getter
@AllArgsConstructor
public enum AfterSaleOperateTypeEnum {
    NEW_AFTER_SALE(10, "新建售后单"),
    NEW_LACK_AFTER_SALE(20, "新建缺品售后单"),
    NEW_CANCEL_AFTER_SALE(25, "新建取消售后单"),
    REVIEW_AFTER_SALE_PASS(30, "售后单审核通过"),
    REVIEW_AFTER_SALE_REJECTION(40, "售后单审核拒绝"),
    REVOKE_AFTER_SALE(50, "撤销售后单"),
    AFTER_SALE_REFUNDING(60, "售后单退款中"),
    AFTER_SALE_REFUNDED(70, "售后单退款成功"),
    AFTER_SALE_REFUND_FAIL(80, "售后单退款失败"),
    ;
    private final int code;
    private final String msg; // TODO 统一为name
}
