package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 订单号状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    NULL(0, "未知"),
    CREATED(10, "已创建"),
    PAID(20, "已支付"),
    FULFILL(30, "已履约"),
    OUT_STOCK(40, "出库"),
    DELIVERY(50, "配送中"),
    SIGNED(60, "已签收"),
    CANCELLED(70, "已取消"),
    REFUSED(100, "已拒收"),
    INVALID(127, "无效订单");
    private final Integer code;
    private final String name;

    public static OrderStatusEnum getByCode(Integer code) {
        for (OrderStatusEnum element : OrderStatusEnum.values()) {
            if (code.equals(element.getCode())) {
                return element;
            }
        }
        return null;
    }

    /**
     * 未出库订单状态
     */
    public static List<Integer> unOutStockStatus() {
        return Arrays.asList(CREATED.code, PAID.code, FULFILL.code);
    }

    /**
     * 可以移除的订单状态
     */
    public static List<Integer> canRemoveStatus() {
        return Arrays.asList(SIGNED.code, CANCELLED.code, REFUSED.code, INVALID.code);
    }

    /**
     * 未支付的订单状态
     */
    public static List<Integer> unPaidStatus() {
        return Arrays.asList(CREATED.code);
    }

    public static Set<Integer> allowableValues() {
        Set<Integer> allowableValues = new HashSet<>(values().length);
        for (OrderStatusEnum orderStatusEnum : values()) {
            allowableValues.add(orderStatusEnum.getCode());
        }
        return allowableValues;
    }

    /**
     * 有效订单状态
     */
    public static Set<Integer> validStatus() {
        Set<Integer> validStatus = allowableValues();
        validStatus.remove(INVALID.code);
        return validStatus;
    }

    /**
     * 可以发起缺品的状态(已出库)
     */
    public static Set<Integer> canLack() {
        Set<Integer> canLackStatus = new HashSet<>();
        canLackStatus.add(OUT_STOCK.code);
        return canLackStatus;
    }
}
