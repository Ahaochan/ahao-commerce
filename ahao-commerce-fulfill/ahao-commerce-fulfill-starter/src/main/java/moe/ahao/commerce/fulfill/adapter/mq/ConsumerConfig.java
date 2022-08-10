package moe.ahao.commerce.fulfill.adapter.mq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static moe.ahao.commerce.common.constants.RocketMqConstant.TRIGGER_ORDER_FULFILL_CONSUMER_GROUP;
import static moe.ahao.commerce.common.constants.RocketMqConstant.TRIGGER_ORDER_FULFILL_TOPIC;


@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(RocketMQProperties.class)
public class ConsumerConfig {
    @Autowired
    private RocketMQProperties rocketMQProperties;

    /**
     * 触发订单履约消息消费者
     */
    @Bean("triggerOrderFulfillConsumer")
    public DefaultMQPushConsumer triggerOrderFulfillConsumer(TriggerOrderFulfillTopicListener triggerOrderFulfillTopicListener)
            throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(TRIGGER_ORDER_FULFILL_CONSUMER_GROUP);
        consumer.setNamesrvAddr(rocketMQProperties.getNameServer());
        consumer.subscribe(TRIGGER_ORDER_FULFILL_TOPIC, "*");
        consumer.registerMessageListener(triggerOrderFulfillTopicListener);
        consumer.start();
        return consumer;
    }

}
