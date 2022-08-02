package com.ruyuan.eshop.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单类型枚举
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public enum OrderTypeEnum {

    NORMAL(1, "一般订单"),
    VIRTUAL(2, "虚拟订单"),
    PRE_SALE(3, "预售订单"),
    UNKNOWN(127, "其他");


    private Integer code;
    private String msg;

    public static Map<Integer, String> toMap() {
        Map<Integer, String> map = new HashMap<>(16);
        for (OrderTypeEnum element : OrderTypeEnum.values()) {
            map.put(element.getCode(), element.getMsg());
        }
        return map;
    }

    public static OrderTypeEnum getByCode(Integer code) {
        for (OrderTypeEnum element : OrderTypeEnum.values()) {
            if (code.equals(element.getCode())) {
                return element;
            }
        }
        return null;
    }

    public static Set<Integer> allowableValues() {
        Set<Integer> allowableValues = new HashSet<>(values().length);
        for (OrderTypeEnum orderTypeEnum : values()) {
            allowableValues.add(orderTypeEnum.getCode());
        }
        return allowableValues;
    }

    /**
     * 可以进行订单履约的订单类型
     */
    public static Set<Integer> canFulfillTypes() {
        Set<Integer> canFulfillTypes = new HashSet<>(values().length);
        canFulfillTypes.add(NORMAL.code);
        canFulfillTypes.add(PRE_SALE.code);
        return canFulfillTypes;
    }

    /**
     * 可以发起缺品的订单类型
     */
    public static Set<Integer> canLack() {
        Set<Integer> canLackStatus = new HashSet<>();
        canLackStatus.add(NORMAL.code);
        return canLackStatus;
    }

}