package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 售后申请原因枚举
 */
@Getter
@AllArgsConstructor
public enum AfterSaleReasonEnum {
    ITEM_NUM(10, "商品数量原因"),
    ITEM_QUALITY(20, "商品质量原因"),
    ITEM_PACKAGE(30, "商品包装原因"),
    LOGISTICS(40, "物流原因"),
    DELIVERY(50, "快递员原因"),
    USER(60, "用户自己原因"),
    ITEM_PRICE(70, "商品价格原因"),
    CANCEL(80, "取消订单"),
    FORCED_CANCELLATION(90, "平台强制取消订单"),
    DISHONOR(100, "拒收"),
    OTHER(200, "其他"),
    ;
    private final int code;
    private final String name;
}
