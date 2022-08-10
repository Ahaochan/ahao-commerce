package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 售后条目类型枚举
 */
@Getter
@AllArgsConstructor
public enum AfterSaleItemTypeEnum {
    AFTER_SALE_ORDER_ITEM(10, "售后订单条目"),
    AFTER_SALE_COUPON(20, "尾笔条目退优惠券"),
    AFTER_SALE_FREIGHT(30, "尾笔条目退运费");

    private final int code;
    private final String name;
}
