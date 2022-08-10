package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * 售后申请来源枚举
 */
@Getter
@AllArgsConstructor
public enum AfterSaleApplySourceEnum {
    USER_APPLY(10, "用户申请退款"),
    SYSTEM(20, "系统自动退款"),
    CUSTOM_APPLY(30, "客服申请退款"),
    USER_RETURN_GOODS(40, "用户申请退货"),
    FULFILL_RETURN_MONEY(50, "履约申请退款"),
    ;
    private final int code;
    private final String name;

    public static Set<Integer> allowableValues() {
        Set<Integer> allowableValues = new HashSet<>(values().length);
        for (AfterSaleApplySourceEnum sourceEnum : values()) {
            allowableValues.add(sourceEnum.getCode());
        }
        return allowableValues;
    }

    /**
     * 用户主动申请
     */
    public static Set<Integer> userApply() {
        Set<Integer> values = new HashSet<>();
        values.add(USER_APPLY.getCode());
        values.add(USER_RETURN_GOODS.getCode());
        return values;
    }
}
