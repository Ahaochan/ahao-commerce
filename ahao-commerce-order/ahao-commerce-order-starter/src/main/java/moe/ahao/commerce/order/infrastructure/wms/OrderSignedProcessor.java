package moe.ahao.commerce.order.infrastructure.wms;

import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.domain.dto.WmsShipDTO;
import moe.ahao.commerce.order.infrastructure.exception.OrderException;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderDeliveryDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单已签收物流结果处理器
 */
@Component
public class OrderSignedProcessor extends AbstractWmsShipResultProcessor {

    @Autowired
    private OrderDeliveryDetailMapper orderDeliveryDetailMapper;

    @Override
    protected boolean checkOrderStatus(OrderInfoDO order) throws OrderException {
        OrderStatusEnum orderStatus = OrderStatusEnum.getByCode(order.getOrderStatus());
        if (!OrderStatusEnum.DELIVERY.equals(orderStatus)) {
            return false;
        }
        return true;
    }

    @Override
    protected void doExecute(WmsShipDTO wmsShipDTO, OrderInfoDO orderInfo) {
        // 增加订单配送表的签收时间
        orderDeliveryDetailMapper.updateSignedTimeByOrderId(orderInfo.getOrderId(), wmsShipDTO.getSignedTime());
    }
}
