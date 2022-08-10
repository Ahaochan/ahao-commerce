package moe.ahao.commerce.order.api.query;

import lombok.Data;
import moe.ahao.commerce.order.api.enums.OrderQueryDataTypeEnums;

import java.io.Serializable;

/**
 * 订单详情请求
 */
@Data
public class OrderDetailQuery {
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 订单项查询枚举
     */
    private OrderQueryDataTypeEnums[] queryDataTypes;
}
