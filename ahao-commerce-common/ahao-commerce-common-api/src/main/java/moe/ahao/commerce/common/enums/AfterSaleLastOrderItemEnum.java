package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 售后订单尾笔条目枚举
 */
@Getter
@AllArgsConstructor
public enum AfterSaleLastOrderItemEnum {
    NOT_LAST_ORDER_ITEM(10, "非尾笔订单条目"),
    LAST_ORDER_ITEM(20, "尾笔订单条目");
    private final int code;
    private final String msg; // TODO 统一为name
}
