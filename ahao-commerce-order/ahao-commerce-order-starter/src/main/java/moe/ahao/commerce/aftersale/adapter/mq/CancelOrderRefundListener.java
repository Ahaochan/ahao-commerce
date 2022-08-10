package moe.ahao.commerce.aftersale.adapter.mq;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.application.CancelOrderRefundAppService;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.event.ReleaseAssetsEvent;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.RELEASE_ASSETS_TOPIC,
    consumerGroup = RocketMqConstant.REQUEST_CONSUMER_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class CancelOrderRefundListener extends AbstractRocketMqListener {
    @Autowired
    private CancelOrderRefundAppService cancelOrderRefundAppService;

    @Override
    public void onMessage(String message) {
        ReleaseAssetsEvent event = JSONHelper.parse(message, ReleaseAssetsEvent.class);
        log.info("接收到取消订单退款消息:{}", message);

        String orderId = event.getOrderId();
        boolean success = cancelOrderRefundAppService.handler(orderId);
        if (!success) {
            throw OrderExceptionEnum.CONSUME_MQ_FAILED.msg();
        }
    }
}
