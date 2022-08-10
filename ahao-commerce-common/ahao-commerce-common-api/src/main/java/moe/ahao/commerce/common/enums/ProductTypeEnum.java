package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品类型枚举
 */
@Getter
@AllArgsConstructor
public enum ProductTypeEnum {
    NORMAL(1, "普通商品"),
    VIRTUAL(2, "虚拟商品"),
    PRE_SALE(3, "预售商品"),
    UNKNOWN(127, "其他");
    ;
    private final Integer code;
    private final String name;

    public static ProductTypeEnum getByCode(Integer code) {
        for (ProductTypeEnum element : ProductTypeEnum.values()) {
            if (code.equals(element.getCode())) {
                return element;
            }
        }
        return null;
    }
}
