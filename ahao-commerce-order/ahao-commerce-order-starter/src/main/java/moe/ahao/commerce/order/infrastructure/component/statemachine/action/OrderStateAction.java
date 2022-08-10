package moe.ahao.commerce.order.infrastructure.component.statemachine.action;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.OrderOperateTypeEnum;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.order.infrastructure.publisher.OrderEventPublisher;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.OrderOperateLogRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderPaymentDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

@Slf4j
public abstract class OrderStateAction<T> extends AbstractStateAction<T, String, OrderStatusChangeEnum> {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderPaymentDetailMapper orderPaymentDetailMapper;
    @Autowired
    private OrderOperateLogRepository orderOperateLogRepository;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Override
    protected void postStateChange(OrderStatusChangeEnum event, String orderId) {
        if (orderId == null) {
            return;
        }
        if (event.isSendEvent()) {
            // 发送订单标准状态变更消息
            // 这里其实是有基于消息总线，message bus，把订单所有的状态变更作为一个消息都推送出去到mq里去
            // 订单他自己，或者是其他的系统，但凡是关注订单事件变更的，都可以去关注topic，通用型的消息总线的效果
            orderEventPublisher.sendStandardOrderStatusChangeMessage(orderId, event);
        }
    }

    /**
     * 更新订单状态
     */
    protected void updateOrderStatus(List<String> orderIdList, Integer fromStatus, Integer toStatus) {
        if (orderIdList.size() == 1) {
            orderInfoMapper.updateOrderStatusByOrderId(orderIdList.get(0), fromStatus, toStatus);
        } else {
            orderInfoMapper.updateOrderStatusByOrderIds(orderIdList, fromStatus, toStatus);
        }
    }

    /**
     * 保存订单操作日志
     *
     * @param orderId     订单ID
     * @param operateType 操作类型
     * @param fromStatus  前一个状态
     * @param toStatus    后一个状态
     * @param remark      备注
     */
    protected void saveOrderOperateLog(String orderId, OrderOperateTypeEnum operateType, Integer fromStatus, Integer toStatus, String remark) {
        orderOperateLogRepository.save(orderId, operateType, fromStatus, toStatus, remark);
    }

    protected void saveOrderOperateLog(OrderInfoDO orderInfo) {
        orderOperateLogRepository.save(orderInfo, event());
    }

    /**
     * 更新订单支付时间和状态
     */
    protected void updateOrderStatusAndPayTime(List<String> orderIdList, Integer fromStatus, Integer toStatus, Date payTime) {
        if (orderIdList.size() == 1) {
            orderInfoMapper.updateOrderStatusAndPayTimeByOrderId(orderIdList.get(0), fromStatus, toStatus, payTime);
        } else {
            orderInfoMapper.updateOrderStatusAndPayTimeByOrderIds(orderIdList, fromStatus, toStatus, payTime);
        }
    }

    /**
     * 更新订单支付状态和时间
     */
    protected void updatePaymentStatusAndPayTime(List<String> orderIdList, Integer payStatus, Date payTime) {
        if (orderIdList.size() == 1) {
            orderPaymentDetailMapper.updatePayStatusAndPayTimeByOrderId(orderIdList.get(0), payStatus, payTime);
        } else {
            orderPaymentDetailMapper.updatePayStatusAndPayTimeByOrderIds(orderIdList, payStatus, payTime);
        }
    }
}
