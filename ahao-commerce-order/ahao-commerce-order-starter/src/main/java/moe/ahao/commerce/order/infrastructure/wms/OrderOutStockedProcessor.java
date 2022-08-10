package moe.ahao.commerce.order.infrastructure.wms;

import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.domain.dto.AfterFulfillDTO;
import moe.ahao.commerce.order.infrastructure.exception.OrderException;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderDeliveryDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单已出库物流结果处理器
 */
@Component
public class OrderOutStockedProcessor extends AbstractWmsShipResultProcessor {

    @Autowired
    private OrderDeliveryDetailMapper orderDeliveryDetailMapper;

    @Override
    protected boolean checkOrderStatus(OrderInfoDO order) throws OrderException {
        OrderStatusEnum orderStatus = OrderStatusEnum.getByCode(order.getOrderStatus());
        if (!OrderStatusEnum.FULFILL.equals(orderStatus)) {
            return false;
        }
        return true;
    }

    @Override
    protected void doExecute(AfterFulfillDTO wmsShipDTO, OrderInfoDO orderInfo) {
        // 增加订单配送表的出库时间
        orderDeliveryDetailMapper.updateOutStockTimeByOrderId(orderInfo.getOrderId(), wmsShipDTO.getOutStockTime());
    }
}
