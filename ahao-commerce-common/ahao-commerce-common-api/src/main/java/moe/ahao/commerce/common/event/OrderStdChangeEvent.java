package moe.ahao.commerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.ahao.commerce.common.enums.BusinessIdentifierEnum;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderTypeEnum;

import java.math.BigDecimal;

/**
 * 订单标准变更消息事件
 */
@Data
public class OrderStdChangeEvent {
    /**
     * 订单编号
     */
    private String orderId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 交易支付金额
     */
    private BigDecimal payAmount;
    /**
     * 订单状态变更枚举
     */
    private OrderStatusChangeEnum statusChange;
}
