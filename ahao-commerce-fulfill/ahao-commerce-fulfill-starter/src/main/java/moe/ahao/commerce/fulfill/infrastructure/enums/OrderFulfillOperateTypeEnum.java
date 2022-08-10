package moe.ahao.commerce.fulfill.infrastructure.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillStatusEnum.*;

/**
 * 履约单操作类型枚举值
 */
@Getter
@AllArgsConstructor
public enum OrderFulfillOperateTypeEnum {
    NEW_ORDER(10, "新建履约单", NULL, FULFILL),
    OUT_STOCK_ORDER(20, "履约单出库", FULFILL, OUT_STOCK),
    DELIVER_ORDER(30, "配送履约单", OUT_STOCK, DELIVERY),
    SIGN_ORDER(40, "签收履约单", DELIVERY, SIGNED),
    CANCEL_ORDER(50, "取消履约单", FULFILL, CANCELLED),
    ;
    private final int code;
    private final String msg; // TODO 统一为name
    private final OrderFulfillStatusEnum fromStatus;
    private final OrderFulfillStatusEnum toStatus;
}
