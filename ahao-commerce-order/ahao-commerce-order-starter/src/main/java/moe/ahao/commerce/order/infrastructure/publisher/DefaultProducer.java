package moe.ahao.commerce.order.infrastructure.publisher;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.rocketmq.MQMessage;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DefaultProducer {
    private final DefaultMQProducer producer;
    public DefaultProducer(RocketMQProperties rocketMQProperties) {
        producer = new TransactionMQProducer(RocketMqConstant.ORDER_DEFAULT_PRODUCER_GROUP);
        producer.setNamesrvAddr(rocketMQProperties.getNameServer());
        start();
    }

    /**
     * 对象在使用之前必须要调用一次，只能初始化一次
     */
    private void start() {
        try {
            this.producer.start();
        } catch (MQClientException e) {
            log.error("producer start error", e);
        }
    }

    /**
     * 一般在应用上下文，使用上下文监听器，进行关闭
     */
    public void shutdown() {
        this.producer.shutdown();
    }

    /**
     * 发送消息
     *
     * @param topic   topic
     * @param message 消息
     */
    public void sendMessage(String topic, String message, String tags, String keys) {
        this.sendMessage(topic, message, -1, tags, keys, null, null);
    }

    /**
     * 发送消息
     *
     * @param topic   topic
     * @param message 消息
     */
    public void sendMessage(String topic, String message, Integer delayTimeLevel, String tags, String keys) {
        this.sendMessage(topic, message, delayTimeLevel, tags, keys, null, null);
    }

    /**
     * 发送消息
     *
     * @param topic   topic
     * @param message 消息
     */
    public void sendMessage(String topic, String message, Integer delayTimeLevel, String tags, String keys, MessageQueueSelector selector, Object arg) {
        Message msg = new MQMessage(topic, tags, keys, message.getBytes(StandardCharsets.UTF_8));
        try {
            if (delayTimeLevel > 0) {
                msg.setDelayTimeLevel(delayTimeLevel);
            }
            SendResult send;
            if (selector == null) {
                send = producer.send(msg);
            } else {
                send = producer.send(msg, selector, arg);
            }
            if (SendStatus.SEND_OK == send.getSendStatus()) {
                log.info("发送MQ消息成功, message:{}", message);
            } else {
                throw OrderExceptionEnum.SEND_MQ_FAILED.msg();
            }
        } catch (Exception e) {
            log.error("发送MQ消息失败：", e);
            throw OrderExceptionEnum.SEND_MQ_FAILED.msg();
        }
    }

    public DefaultMQProducer getProducer() {
        return producer;
    }
}
