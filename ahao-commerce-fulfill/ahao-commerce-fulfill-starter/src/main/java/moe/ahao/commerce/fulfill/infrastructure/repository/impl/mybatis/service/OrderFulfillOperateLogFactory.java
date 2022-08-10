package moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.service;

import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillOperateTypeEnum;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillLogDO;
import org.springframework.stereotype.Component;

/**
 * 履约单操作日志工厂
 */
@Component
public class OrderFulfillOperateLogFactory {

    /**
     * 获取履约单操作日志
     */
    public OrderFulfillLogDO get(OrderFulfillDO orderFulfill, OrderFulfillOperateTypeEnum operateType) {
        Integer fromStatus = operateType.getFromStatus().getCode();
        Integer toStatus = operateType.getToStatus().getCode();
        return create(orderFulfill, operateType, fromStatus, toStatus, operateType.getMsg());
    }

    /**
     * 创建履约单操作日志
     */
    private OrderFulfillLogDO create(OrderFulfillDO orderFulfill,
                                     OrderFulfillOperateTypeEnum operateType, int preStatus, int currentStatus, String operateRemark) {
        OrderFulfillLogDO log = new OrderFulfillLogDO();
        log.setOrderId(orderFulfill.getOrderId());
        log.setFulfillId(orderFulfill.getFulfillId());
        log.setOperateType(operateType.getCode());
        log.setPreStatus(preStatus);
        log.setCurrentStatus(currentStatus);
        log.setRemark(operateRemark);
        return log;
    }


}
