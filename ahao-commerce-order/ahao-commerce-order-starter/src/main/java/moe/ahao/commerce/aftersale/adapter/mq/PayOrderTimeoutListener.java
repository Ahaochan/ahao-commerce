package moe.ahao.commerce.aftersale.adapter.mq;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.application.CancelOrderAppService;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.event.PayOrderTimeoutEvent;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 监听 支付订单超时延迟消息
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.PAY_ORDER_TIMEOUT_DELAY_TOPIC,
    consumerGroup = RocketMqConstant.PAY_ORDER_TIMEOUT_DELAY_CONSUMER_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class PayOrderTimeoutListener extends AbstractRocketMqListener {
    @Autowired
    private CancelOrderAppService cancelOrderAppService;

    @Override
    public void onMessage(String message) {
        PayOrderTimeoutEvent event = JSONHelper.parse(message, PayOrderTimeoutEvent.class);

        String orderId = event.getOrderId();
        cancelOrderAppService.autoCancel(orderId);
    }
}
