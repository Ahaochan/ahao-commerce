package moe.ahao.commerce.customer.adapter.mq;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractMessageListenerConcurrently;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.customer.api.event.CustomerReceiveAfterSaleEvent;
import moe.ahao.commerce.customer.application.ReceivableAfterSaleAppService;
import moe.ahao.commerce.customer.infrastructure.exception.CustomerExceptionEnum;
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

/**
 * 接收订单系统售后审核申请
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.AFTER_SALE_CUSTOMER_AUDIT_TOPIC,
    consumerGroup = RocketMqConstant.AFTER_SALE_CUSTOMER_AUDIT_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class AfterSaleCustomerAuditTopicListener extends AbstractRocketMqListener {

    @Autowired
    private ReceivableAfterSaleAppService receivableAfterSaleAppService;

    @Override
    public void onMessage(String message) {
        log.info("AfterSaleCustomerAuditTopicListener message:{}", message);
        CustomerReceiveAfterSaleEvent event = JSONHelper.parse(message, CustomerReceiveAfterSaleEvent.class);
        //  客服接收订单系统的售后申请
        boolean result = receivableAfterSaleAppService.handler(event);
        if (!result) {
            throw CustomerExceptionEnum.PROCESS_RECEIVE_AFTER_SALE.msg();
        }
    }
}
