package com.ruyuan.eshop.order.wms;

import com.ruyuan.eshop.order.dao.OrderDeliveryDetailDAO;
import com.ruyuan.eshop.order.domain.dto.WmsShipDTO;
import com.ruyuan.eshop.order.domain.entity.OrderDeliveryDetailDO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.exception.OrderBizException;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单已签收物流结果处理器
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
public class OrderSignedProcessor extends AbstractWmsShipResultProcessor {

    @Autowired
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

    @Override
    protected boolean checkOrderStatus(OrderInfoDO order) throws OrderBizException {
        OrderStatusEnum orderStatus = OrderStatusEnum.getByCode(order.getOrderStatus());
        if (!OrderStatusEnum.DELIVERY.equals(orderStatus)) {
            return false;
        }
        return true;
    }

    @Override
    protected void doExecute(WmsShipDTO wmsShipDTO, OrderInfoDO order) {
        //增加订单配送表的签收时间
        OrderDeliveryDetailDO deliveryDetail = orderDeliveryDetailDAO.getByOrderId(order.getOrderId());
        orderDeliveryDetailDAO.updateSignedTime(deliveryDetail.getId(), wmsShipDTO.getSignedTime());
    }

}
