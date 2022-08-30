package com.ruyuan.eshop.order.statemachine.action.order.afterfulfill;

import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.common.enums.OrderStatusEnum;
import com.ruyuan.eshop.order.dao.OrderDeliveryDetailDAO;
import com.ruyuan.eshop.order.domain.dto.AfterFulfillDTO;
import com.ruyuan.eshop.order.domain.entity.OrderDeliveryDetailDO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订单已配送Action
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
public class OrderDeliveredAction extends AbstractAfterFulfillResultAction {

    @Resource
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

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
        // 增加订单配送表的配送员信息
        OrderDeliveryDetailDO deliveryDetail = orderDeliveryDetailDAO.getByOrderId(order.getOrderId());
        orderDeliveryDetailDAO.updateDeliverer(deliveryDetail.getOrderId()
                , afterFulfillDTO.getDelivererNo()
                , afterFulfillDTO.getDelivererName(),
                afterFulfillDTO.getDelivererPhone());
    }

}
