package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.afterfulfill;

import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.domain.dto.AfterFulfillDTO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderDeliveryDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单已配送Action
 */
@Component
public class OrderDeliveredAction extends AbstractOrderFulfillAction {
    @Autowired
    private OrderDeliveryDetailMapper orderDeliveryDetailMapper;

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_DELIVERED;
    }

    @Override
    protected OrderStatusEnum handleStatus() {
        return OrderStatusEnum.OUT_STOCK;
    }

    @Override
    protected void doExecute(AfterFulfillDTO afterFulfillDTO, OrderInfoDO order) {
        String orderId = order.getOrderId();
        // 增加订单配送表的配送员信息
        orderDeliveryDetailMapper.updateDelivererByOrderId(orderId, afterFulfillDTO.getDelivererNo(), afterFulfillDTO.getDelivererName(), afterFulfillDTO.getDelivererPhone());
    }

}
