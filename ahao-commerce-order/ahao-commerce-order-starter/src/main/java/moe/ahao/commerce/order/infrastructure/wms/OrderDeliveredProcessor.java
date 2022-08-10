package moe.ahao.commerce.order.infrastructure.wms;

import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.domain.dto.AfterFulfillDTO;
import moe.ahao.commerce.order.infrastructure.exception.OrderException;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderDeliveryDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单已配送物流结果处理器
 */
@Component
public class OrderDeliveredProcessor extends AbstractWmsShipResultProcessor {
    @Autowired
    private OrderDeliveryDetailMapper orderDeliveryDetailMapper;

    @Override
    protected boolean checkOrderStatus(OrderInfoDO order) throws OrderException {
        OrderStatusEnum orderStatus = OrderStatusEnum.getByCode(order.getOrderStatus());
        if (!OrderStatusEnum.OUT_STOCK.equals(orderStatus)) {
            return false;
        }
        return true;
    }

    @Override
    protected void doExecute(AfterFulfillDTO wmsShipDTO, OrderInfoDO order) {
        // 增加订单配送表的配送员信息
        orderDeliveryDetailMapper.updateDelivererByOrderId(order.getOrderId(),
            wmsShipDTO.getDelivererNo(), wmsShipDTO.getDelivererName(), wmsShipDTO.getDelivererPhone());
    }
}
