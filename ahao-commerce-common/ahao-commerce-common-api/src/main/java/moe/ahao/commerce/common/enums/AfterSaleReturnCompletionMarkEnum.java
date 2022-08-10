package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 售后订单尾笔条目枚举
 * <p>
 * 退货完成标记ReturnCompletionMark的说明：初始默认是10,此条目全部退完更新为20
 * 例如：当前条目一共有10个sku
 * 第一次退1个, mark是10
 * 第二次退2个, mark是10
 * 第三次退7个, 条目退完, mark是20
 */
@Getter
@AllArgsConstructor
public enum AfterSaleReturnCompletionMarkEnum {
    NOT_ALL_RETURN_GOODS(10, "购买的sku未全部退货"),
    ALL_RETURN_GOODS(20, "购买的sku已全部退货");
    private final int code;
    private final String name;
}
