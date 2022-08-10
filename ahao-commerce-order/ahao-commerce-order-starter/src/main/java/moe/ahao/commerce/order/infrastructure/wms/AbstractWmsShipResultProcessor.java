package moe.ahao.commerce.order.infrastructure.wms;


import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.order.infrastructure.domain.dto.AfterFulfillDTO;
import moe.ahao.commerce.order.infrastructure.exception.OrderException;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.OrderOperateLogRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractWmsShipResultProcessor implements OrderWmsShipResultProcessor {
    @Autowired
    protected OrderInfoMapper orderInfoMapper;
    @Autowired
    protected OrderOperateLogRepository orderOperateLogRepository;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void execute(AfterFulfillDTO wmsShipDTO) throws OrderException {
        // 1. 查询订单
        OrderInfoDO order = orderInfoMapper.selectOneByOrderId(wmsShipDTO.getOrderId());
        if (order == null) {
            return;
        }

        // 2. 校验订单状态
        if (!this.checkOrderStatus(order)) {
            return;
        }

        // 3. 执行具体的业务逻辑
        this.doExecute(wmsShipDTO, order);

        // 4. 更新订单状态
        OrderStatusChangeEnum statusChange = wmsShipDTO.getStatusChange();
        Integer formStatus = statusChange.getFromStatus().getCode();
        Integer toStatus = statusChange.getToStatus().getCode();
        orderInfoMapper.updateOrderStatusByOrderId(order.getOrderId(), formStatus, toStatus);

        // 5. 增加操作日志
        orderOperateLogRepository.save(order, wmsShipDTO.getStatusChange());
    }

    /**
     * 校验订单状态
     */
    protected abstract boolean checkOrderStatus(OrderInfoDO order);

    /**
     * 执行具体的业务逻辑
     */
    protected abstract void doExecute(AfterFulfillDTO wmsShipDTO, OrderInfoDO order);
}
