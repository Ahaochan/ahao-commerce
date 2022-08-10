package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.afterfulfill;


import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.OrderStateAction;
import moe.ahao.commerce.order.infrastructure.domain.dto.AfterFulfillDTO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Collections;

public abstract class AbstractOrderFulfillAction extends OrderStateAction<AfterFulfillDTO> {
    @Autowired
    protected OrderInfoMapper orderInfoMapper;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    protected String onStateChangeInternal(OrderStatusChangeEnum event, AfterFulfillDTO afterFulfillDTO) {
        // @Transactional无法生效，需要用编程式事务
        return transactionTemplate.execute(transactionStatus -> {
            // 1. 查询订单
            String orderId = afterFulfillDTO.getOrderId();
            OrderInfoDO order = orderInfoMapper.selectOneByOrderId(orderId);
            if (order == null) {
                return null;
            }

            // 2. 校验订单状态
            OrderStatusEnum orderStatus = OrderStatusEnum.getByCode(order.getOrderStatus());
            if (handleStatus() != orderStatus) {
                return null;
            }

            // 3、执行具体的业务逻辑
            this.doExecute(afterFulfillDTO, order);

            // 4. 更新订单状态
            OrderStatusChangeEnum statusChange = event();
            Integer fromStatus = statusChange.getFromStatus().getCode();
            Integer toStatus = statusChange.getToStatus().getCode();
            super.updateOrderStatus(Collections.singletonList(orderId), fromStatus, toStatus);

            // 5. 并插入一条订单变更记录
            order.setOrderStatus(toStatus);
            super.saveOrderOperateLog(order);
            return orderId;
        });
    }

    /**
     * @return 自己需要处理的订单桩体
     */
    protected abstract OrderStatusEnum handleStatus();

    /**
     * 执行具体的业务逻辑
     */
    protected void doExecute(AfterFulfillDTO afterFulfillDTO, OrderInfoDO order) {
        // 默认空实现
    }
}
