package moe.ahao.commerce.aftersale.adapter.mq;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.application.AfterSaleActualRefundAppService;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.event.ActualRefundEvent;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractMessageListenerConcurrently;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
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

import static moe.ahao.commerce.common.constants.RocketMqConstant.ACTUAL_REFUND_CONSUMER_GROUP;
import static moe.ahao.commerce.common.constants.RocketMqConstant.ACTUAL_REFUND_TOPIC;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.ACTUAL_REFUND_TOPIC,
    consumerGroup = RocketMqConstant.ACTUAL_REFUND_CONSUMER_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class ActualRefundListener extends AbstractRocketMqListener {
    @Autowired
    private AfterSaleActualRefundAppService afterSaleActualRefundAppService;

    @Override
    public void onMessage(String message) {
        log.info("接收到实际退款消息:{}", message);
        ActualRefundEvent event = JSONObject.parseObject(message, ActualRefundEvent.class);

        boolean success = afterSaleActualRefundAppService.refundMoney(event);
        if (!success) {
            throw OrderExceptionEnum.CONSUME_MQ_FAILED.msg();
        }
    }
}
