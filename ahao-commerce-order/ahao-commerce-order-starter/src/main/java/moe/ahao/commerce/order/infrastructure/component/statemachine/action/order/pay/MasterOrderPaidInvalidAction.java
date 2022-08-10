package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.pay;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.OrderOperateTypeEnum;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.OrderStateAction;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 主订单已支付失效Action
 */
@Slf4j
@Component
public class MasterOrderPaidInvalidAction extends OrderStateAction<OrderInfoDO> {

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_PAID_INVALID;
    }

    @Override
    protected String onStateChangeInternal(OrderStatusChangeEnum event, OrderInfoDO orderInfo) {
        String orderId = orderInfo.getOrderId();
        Integer fromStatus = orderInfo.getOrderStatus();
        Integer toStatus = OrderStatusEnum.INVALID.getCode();

        // 1. 将主订单状态设置为无效订单
        List<String> orderIdList = Collections.singletonList(orderId);
        super.updateOrderStatus(orderIdList, fromStatus, toStatus);

        // 2. 新增订单状态变更日志
        OrderOperateTypeEnum operateType = OrderOperateTypeEnum.ORDER_PAID_INVALID;
        String remark = "订单支付回调操作，主订单状态变更" + fromStatus + "-" + toStatus;
        super.saveOrderOperateLog(orderId, operateType, fromStatus, toStatus, remark);

        return orderInfo.getOrderId();
    }

}
