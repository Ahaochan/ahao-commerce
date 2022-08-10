package moe.ahao.commerce.fulfill.infrastructure.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 履约单状态
 */
@Getter
@AllArgsConstructor
public enum OrderFulfillStatusEnum {
    NULL(0, "未知"),
    FULFILL(20, "已履约"),
    OUT_STOCK(30, "出库"),
    DELIVERY(40, "配送中"),
    SIGNED(50, "已签收"),
    CANCELLED(100, "已取消"),
    ;
    private final int code;
    private final String msg; // TODO 统一为name

    /**
     * 不能取消履约的状态
     *
     * @return
     */
    public static Set<Integer> notCancelStatus() {
        Set<Integer> set = new HashSet<>(values().length);
        set.add(OUT_STOCK.code);
        set.add(DELIVERY.code);
        set.add(SIGNED.code);
        set.add(CANCELLED.code);
        return set;
    }
}
