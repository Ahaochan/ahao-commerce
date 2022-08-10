package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * 售后状态枚举
 */
@Getter
@AllArgsConstructor
public enum AfterSaleStatusEnum {
    UN_CREATED(0, "未创建"),
    COMMITTED(10, "提交申请"),
    REVIEW_PASS(20, "审核通过"),
    REVIEW_REJECTED(30, "审核拒绝"),
    REFUNDING(40, "退款中"),
    REFUNDED(50, "退款成功"),
    FAILED(60, "退款失败"),
    CLOSED(70, "已关闭"),
    REOPEN(100, "重新提交申请"),
    REVOKE(127, "撤销成功"),
    ;
    private final int code;
    private final String name; // TODO 统一为name

    public static Set<Integer> allowableValues() {
        Set<Integer> allowableValues = new HashSet<>(values().length);
        for (AfterSaleStatusEnum statusEnum : values()) {
            allowableValues.add(statusEnum.getCode());
        }
        return allowableValues;
    }
}
