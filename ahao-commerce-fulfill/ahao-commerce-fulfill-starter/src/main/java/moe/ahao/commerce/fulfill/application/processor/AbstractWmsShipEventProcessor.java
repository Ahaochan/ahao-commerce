package moe.ahao.commerce.fulfill.application.processor;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.api.event.OrderEvent;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.infrastructure.rocketmq.MQMessage;
import moe.ahao.commerce.fulfill.api.event.TriggerOrderWmsShipEvent;
import moe.ahao.commerce.fulfill.infrastructure.publisher.DefaultProducer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.List;


@Slf4j
public abstract class AbstractWmsShipEventProcessor implements OrderWmsShipEventProcessor {
    @Autowired
    private DefaultProducer defaultProducer;

    @Override
    public void execute(TriggerOrderWmsShipEvent event) {
        // 1. 执行业务流程
        this.doBizProcess(event);

        // 2. 构造消息体
        String body = this.buildMsgBody(event);

        // 3. 发送消息
        this.sendMessage(body, event.getOrderId());
    }

    protected abstract void doBizProcess(TriggerOrderWmsShipEvent event);

    protected abstract String buildMsgBody(TriggerOrderWmsShipEvent event);

    private void sendMessage(String body, String orderId) {
        if (StringUtils.isBlank(body)) {
            return;
        }
        Message message = new MQMessage();
        message.setTopic(RocketMqConstant.ORDER_WMS_SHIP_RESULT_TOPIC);
        message.setBody(body.getBytes(StandardCharsets.UTF_8));
        try {
            DefaultMQProducer defaultMQProducer = defaultProducer.getProducer();
            SendResult sendResult = defaultMQProducer.send(message, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message message, Object arg) {
                    // 根据订单id选择发送queue
                    String orderId = (String) arg;
                    // 解决取模可能为负数的情况
                    long index = (orderId.hashCode() & Integer.MAX_VALUE) % mqs.size();
                    return mqs.get((int) index);
                }
            }, orderId);
            log.info("send order wms ship result message finished，SendResult status:{}, queueId:{}, body:{}", sendResult.getSendStatus(),
                sendResult.getMessageQueue().getQueueId(), body);
        } catch (Exception e) {
            log.error("send order wms ship result message error,orderId={},err={}", orderId, e.getMessage(), e);
        }
    }

    protected <T> OrderEvent<T> buildOrderEvent(String orderId, OrderStatusChangeEnum orderStatusChange, T messaheContent, Class<T> clazz) {
        OrderEvent<T> orderEvent = new OrderEvent<>();
        orderEvent.setOrderId(orderId);
        orderEvent.setBusinessIdentifier(1); // TODO 业务线枚举
        orderEvent.setOrderType(1); // TODO 订单类型枚举
        orderEvent.setOrderStatusChange(orderStatusChange);
        orderEvent.setMessageContent(messaheContent);
        return orderEvent;
    }
}
