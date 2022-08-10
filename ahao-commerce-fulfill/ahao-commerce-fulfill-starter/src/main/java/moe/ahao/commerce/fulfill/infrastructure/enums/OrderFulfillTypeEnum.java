package moe.ahao.commerce.fulfill.infrastructure.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import moe.ahao.commerce.common.enums.OrderTypeEnum;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 履约单类型枚举
 */
@Getter
@AllArgsConstructor
public enum OrderFulfillTypeEnum {
    NORMAL(1, "一般履约单"),
    PRE_SALE(2, "预售履约单"),
    UNKNOWN(127, "其他");
    private final int code;
    private final String msg; // TODO 统一为name

    public static OrderFulfillTypeEnum getByCode(Integer code) {
        for (OrderFulfillTypeEnum element : OrderFulfillTypeEnum.values()) {
            if (code.equals(element.getCode())) {
                return element;
            }
        }
        return null;
    }

    public static OrderFulfillTypeEnum getByOrderType(Integer orderType) {
        OrderTypeEnum orderTypeEnum = OrderTypeEnum.getByCode(orderType);
        if (OrderTypeEnum.NORMAL.equals(orderTypeEnum)) {
            return OrderFulfillTypeEnum.NORMAL;
        } else if (OrderTypeEnum.PRE_SALE.equals(orderTypeEnum)) {
            return OrderFulfillTypeEnum.PRE_SALE;
        } else {
            return OrderFulfillTypeEnum.NORMAL;
        }
    }
}
