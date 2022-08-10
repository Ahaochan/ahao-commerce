package moe.ahao.commerce.order.api.enums;

/**
 * 订单项查询枚举
 */
public enum OrderQueryDataTypeEnums {
    /**
     * 订单主信息
     **/
    ORDER,
    /**
     * 订单条目
     **/
    ORDER_ITEM,
    /**
     * 订单费用明细
     **/
    ORDER_AMOUNT_DETAIL,
    /**
     * 订单支付
     **/
    PAYMENT,
    /**
     * 订单配送信息
     **/
    DELIVERY,
    /**
     * 费用
     **/
    AMOUNT,
    /**
     * 操作日志
     **/
    OPERATE_LOG,
    /**
     * 订单快照
     **/
    SNAPSHOT,
    /**
     * 订单缺品
     **/
    LACK_ITEM;
}
