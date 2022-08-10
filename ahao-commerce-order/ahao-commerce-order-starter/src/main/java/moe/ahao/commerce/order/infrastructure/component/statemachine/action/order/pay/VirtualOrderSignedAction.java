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
 * 虚拟订单已签收Action
 */
@Slf4j
@Component
public class VirtualOrderSignedAction extends OrderStateAction<OrderInfoDO> {
    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.VIRTUAL_ORDER_SIGNED;
    }

    @Override
    protected String onStateChangeInternal(OrderStatusChangeEnum event, OrderInfoDO orderInfo) {
        // 1. 更新订单状态
        Integer fromStatus = OrderStatusEnum.PAID.getCode();
        Integer toStatus = OrderStatusEnum.SIGNED.getCode();
        String orderId = orderInfo.getOrderId();
        List<String> orderIdList = Collections.singletonList(orderId);
        super.updateOrderStatus(orderIdList, fromStatus, toStatus);

        // 2. 新增订单状态变更日志
        OrderOperateTypeEnum operateType = OrderOperateTypeEnum.ORDER_SIGNED;
        String remark = "虚拟订单已签收" + fromStatus + "-" + toStatus;
        super.saveOrderOperateLog(orderId, operateType, fromStatus, toStatus, remark);

        return orderId;
    }
}
