package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * 售后类型枚举
 */
@Getter
@AllArgsConstructor
public enum AfterSaleTypeEnum {
    RETURN_MONEY(1, "整笔退款"),
    RETURN_GOODS(2, "售后退货");
    private final Integer code;
    private final String name;

    public static Set<Integer> allowableValues() {
        Set<Integer> allowableValues = new HashSet<>(values().length);
        for (AfterSaleTypeEnum typeEnum : values()) {
            allowableValues.add(typeEnum.getCode());
        }
        return allowableValues;
    }
}
