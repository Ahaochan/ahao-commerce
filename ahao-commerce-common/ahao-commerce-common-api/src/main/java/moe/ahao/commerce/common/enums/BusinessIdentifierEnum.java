package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * 接入方业务线枚举
 */
@Getter
@AllArgsConstructor
public enum BusinessIdentifierEnum {
    SELF_MALL(1, "自营商城");
    ;
    private final Integer code;
    private final String name;

    public static BusinessIdentifierEnum getByCode(Integer code) {
        for (BusinessIdentifierEnum element : BusinessIdentifierEnum.values()) {
            if (code.equals(element.getCode())) {
                return element;
            }
        }
        return null;
    }

    public static Set<Integer> allowableValues() {
        Set<Integer> allowableValues = new HashSet<>(values().length);
        for (BusinessIdentifierEnum businessIdentifierEnum : values()) {
            allowableValues.add(businessIdentifierEnum.getCode());
        }
        return allowableValues;
    }
}
