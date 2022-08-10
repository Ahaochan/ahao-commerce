package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.afterfulfill;

import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.domain.dto.AfterFulfillDTO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderDeliveryDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订单已签收结果Action
 */
@Component
public class OrderSignedAction extends AbstractOrderFulfillAction {
    @Autowired
    private OrderDeliveryDetailMapper orderDeliveryDetailMapper;

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_SIGNED;
    }

    @Override
    protected OrderStatusEnum handleStatus() {
        return OrderStatusEnum.DELIVERY;
    }

    @Override
    protected void doExecute(AfterFulfillDTO afterFulfillDTO, OrderInfoDO order) {
        String orderId = order.getOrderId();
        //增加订单配送表的签收时间
        orderDeliveryDetailMapper.updateSignedTimeByOrderId(orderId, afterFulfillDTO.getSignedTime());
    }
}
