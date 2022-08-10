package moe.ahao.commerce.order.adapter.mq;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.api.event.OrderEvent;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.fulfill.api.event.OrderDeliveredEvent;
import moe.ahao.commerce.fulfill.api.event.OrderOutStockEvent;
import moe.ahao.commerce.fulfill.api.event.OrderSignedEvent;
import moe.ahao.commerce.order.application.OrderFulFillService;
import moe.ahao.commerce.order.infrastructure.domain.dto.AfterFulfillDTO;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 监听 订单物流配送结果消息
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.ORDER_WMS_SHIP_RESULT_TOPIC,
    consumerGroup = RocketMqConstant.ORDER_WMS_SHIP_RESULT_CONSUMER_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class OrderAfterFulfillEventListener extends AbstractRocketMqListener {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderFulFillService orderFulFillService;

    @Override
    public void onMessage(String message) {
        log.info("接收订单物流配送消息, message:{}", message);
        OrderEvent<?> orderEvent = JSONHelper.parse(message, OrderEvent.class);

        // 1. 解析消息
        AfterFulfillDTO afterFulfillDTO = this.buildWmsShip(orderEvent);

        // 2. 加分布式锁+里面的前置状态校验防止消息重复消费
        String orderId = afterFulfillDTO.getOrderId();
        String lockKey = RedisLockKeyConstants.ORDER_AFTER_FULFILL_EVENT_KEY + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            log.error("order has not acquired lock，cannot inform order after fulfill result, orderId={}", orderId);
            throw OrderExceptionEnum.ORDER_NOT_ALLOW_INFORM_WMS_RESULT.msg();
        }

        // 3. 通知订单物流结果
        try {
            orderFulFillService.informOrderWmsShipResult(afterFulfillDTO);
        } finally {
            lock.unlock();
        }
    }

    private AfterFulfillDTO buildWmsShip(OrderEvent orderEvent) {
        String messageContent = JSONHelper.toString(orderEvent.getMessageContent());
        AfterFulfillDTO wmsShipDTO = new AfterFulfillDTO();
        wmsShipDTO.setOrderId(orderEvent.getOrderId());
        wmsShipDTO.setStatusChange(orderEvent.getOrderStatusChange());
        if (OrderStatusChangeEnum.ORDER_OUT_STOCKED.equals(orderEvent.getOrderStatusChange())) {
            // 订单已出库消息
            OrderOutStockEvent outStockWmsEvent = JSONHelper.parse(messageContent, OrderOutStockEvent.class);
            wmsShipDTO.setOutStockTime(outStockWmsEvent.getOutStockTime());
        } else if (OrderStatusChangeEnum.ORDER_DELIVERED.equals(orderEvent.getOrderStatusChange())) {
            // 订单已配送消息
            OrderDeliveredEvent deliveredWmsEvent = JSONHelper.parse(messageContent, OrderDeliveredEvent.class);
            wmsShipDTO.setDelivererNo(deliveredWmsEvent.getDelivererNo());
            wmsShipDTO.setDelivererName(deliveredWmsEvent.getDelivererName());
            wmsShipDTO.setDelivererPhone(deliveredWmsEvent.getDelivererPhone());
        } else if (OrderStatusChangeEnum.ORDER_SIGNED.equals(orderEvent.getOrderStatusChange())) {
            // 订单已签收消息
            OrderSignedEvent signedWmsEvent = JSONHelper.parse(messageContent, OrderSignedEvent.class);
            wmsShipDTO.setSignedTime(signedWmsEvent.getSignedTime());
        }
        return wmsShipDTO;
    }
}
