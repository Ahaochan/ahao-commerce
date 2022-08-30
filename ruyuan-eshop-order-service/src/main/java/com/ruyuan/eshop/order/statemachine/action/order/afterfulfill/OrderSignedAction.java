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
 * 订单已签收结果Action
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
public class OrderSignedAction extends AbstractAfterFulfillResultAction {

    @Resource
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

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
        //增加订单配送表的签收时间
        OrderDeliveryDetailDO deliveryDetail = orderDeliveryDetailDAO.getByOrderId(order.getOrderId());
        orderDeliveryDetailDAO.updateSignedTime(deliveryDetail.getOrderId(), afterFulfillDTO.getSignedTime());
    }

}
