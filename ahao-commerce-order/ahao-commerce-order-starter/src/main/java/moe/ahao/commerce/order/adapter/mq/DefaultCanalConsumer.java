package moe.ahao.commerce.order.adapter.mq;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * canal.mq.topic=default-order-topic
 * 在canal.mq.dynamicTopic找不到的情况下, 才投递到这个topic进行兜底
 * <p>
 * 用于接收canal监听到的订单表日志变化
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.DEFAULT_ORDER_BINLOG,
    consumerGroup = RocketMqConstant.DEFAULT_ORDER_BINLOG_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.ORDERLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class DefaultCanalConsumer extends AbstractRocketMqListener {
    @Autowired
    private OrderCanalConsumer orderInfoBinlogConsumer;
    @Autowired
    private AfterSaleCanalConsumer reverseOrderInfoBinlogConsumer;

    /**
     * 已测试
     * DefaultRocketMQListenerContainer#DefaultMessageListenerOrderly
     * 在发生错误时候 会返回ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT
     *
     * @param msg canal监听到的binlog日志
     */
    @Override
    public void onMessage(String msg) {
        log.info("canal order default 接收到消息 -> {}", msg);
        orderInfoBinlogConsumer.onMessage(msg);
        reverseOrderInfoBinlogConsumer.onMessage(msg);
    }
}
