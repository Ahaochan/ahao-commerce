package moe.ahao.commerce.fulfill.application.processor;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.api.event.OrderEvent;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.fulfill.api.event.OrderDeliveredEvent;
import moe.ahao.commerce.fulfill.api.event.TriggerOrderAfterFulfillEvent;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillOperateTypeEnum;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillStatusEnum;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillLogDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillLogMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillMapper;
import moe.ahao.util.commons.io.JSONHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 订单已配送事件处理器
 */
@Slf4j
@Service
public class OrderDeliveredEventProcessor extends AbstractAfterFulfillEventProcessor {
    @Autowired
    private OrderFulfillMapper orderFulfillMapper;
    @Autowired
    private OrderFulfillLogMapper orderFulfillLogMapper;

    @Override
    protected boolean doBizProcess(TriggerOrderAfterFulfillEvent event) {
        String fulfillId = event.getFulfillId();
        OrderFulfillDO orderFulfill = orderFulfillMapper.selectOneByFulfillId(fulfillId);
        if (!Objects.equals(OrderFulfillStatusEnum.OUT_STOCK.getCode(), orderFulfill.getStatus())) {
            log.info("履约单无法配送! orderId={}", orderFulfill.getOrderId());
            return false;
        }

        // 更新履约单状态
        OrderFulfillOperateTypeEnum operateTypeEnum = OrderFulfillOperateTypeEnum.DELIVER_ORDER;
        int fromStatus = operateTypeEnum.getFromStatus().getCode();
        int toStatus = operateTypeEnum.getToStatus().getCode();
        String remark = operateTypeEnum.getMsg();
        orderFulfillMapper.updateFulfillStatusByFulfillId(fulfillId, fromStatus, toStatus);

        // 记录履约单信息
        OrderFulfillLogDO log = new OrderFulfillLogDO();
        log.setOrderId(orderFulfill.getOrderId());
        log.setFulfillId(orderFulfill.getFulfillId());
        log.setOperateType(operateTypeEnum.getCode());
        log.setPreStatus(fromStatus);
        log.setCurrentStatus(toStatus);
        log.setRemark(remark);
        orderFulfillLogMapper.insert(log);

        OrderDeliveredEvent deliveredWmsEvent = (OrderDeliveredEvent) event.getWmsEvent();
        // 更新配送员信息
        orderFulfillMapper.updateDelivererInfoByFulfillId(fulfillId,
            deliveredWmsEvent.getDelivererNo(),
            deliveredWmsEvent.getDelivererName(), deliveredWmsEvent.getDelivererPhone());
        return true;
    }

    @Override
    protected String buildMsgBody(TriggerOrderAfterFulfillEvent event) {
        String orderId = event.getOrderId();
        // 订单只需要发送一次已配送消息
        // 预售单在创建履约单的时候可能会发生拆单情况, 即创建多个履约单，
        // 每个履约单进行履约调度的时候都会尝试发送订单履约后的消息
        // 如果两个履约单都属于同一笔订单的话, 那么只有在第一笔履约单进行履约调度单时候, 会发送消息
        // 由于doBizProcess()会先执行，所以至少会插入一条日志
        List<OrderFulfillLogDO> logs = orderFulfillLogMapper.selectListByOrderIdAndStatus(orderId, OrderFulfillStatusEnum.DELIVERY.getCode());
        if (logs.size() > 1) {
            return null;
        }

        // 订单已配送事件
        OrderDeliveredEvent deliveredWmsEvent = (OrderDeliveredEvent) event.getWmsEvent();
        deliveredWmsEvent.setOrderId(orderId);

        // 构建订单已配送消息体
        OrderEvent<OrderDeliveredEvent> orderEvent = buildOrderEvent(orderId, OrderStatusChangeEnum.ORDER_DELIVERED,
                deliveredWmsEvent, OrderDeliveredEvent.class);
        return JSONHelper.toString(orderEvent);
    }
}
