package moe.ahao.commerce.order.adapter.mq;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.event.PaidOrderSuccessEvent;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractMessageListenerConcurrently;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.order.application.PaidOrderSuccessAppService;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

import static moe.ahao.commerce.common.constants.RocketMqConstant.PAID_ORDER_SUCCESS_CONSUMER_GROUP;
import static moe.ahao.commerce.common.constants.RocketMqConstant.PAID_ORDER_SUCCESS_TOPIC;

/**
 * 监听订单支付成功后的消息
 */
@Slf4j
@Component
@RocketMQMessageListener(
    // 订阅某个Topic
    topic = RocketMqConstant.ORDER_STD_CHANGE_EVENT_TOPIC,
    // 指定消费者组
    consumerGroup = RocketMqConstant.ORDER_STD_CHANGE_EVENT_CONSUMER_GROUP,
    // 筛选topic里指定的tags的消息
    selectorExpression = "paid || sub_paid", // 专门从指定的topic里，监听paied类型的消息
    // 消费模式并发消费
    consumeMode = ConsumeMode.CONCURRENTLY,
    // 消息模型是分散消费, 不是广播消费
    messageModel = MessageModel.CLUSTERING,
    // 消费线程最多只有1个
    consumeThreadMax = 1
)
public class PaidOrderSuccessListener extends AbstractRocketMqListener {
    @Autowired
    private PaidOrderSuccessAppService paidOrderSuccessAppService;

    @Override
    public void onMessage(String message) {
        PaidOrderSuccessEvent event = JSONHelper.parse(message, PaidOrderSuccessEvent.class);
        paidOrderSuccessAppService.consumer(event);
    }
}
