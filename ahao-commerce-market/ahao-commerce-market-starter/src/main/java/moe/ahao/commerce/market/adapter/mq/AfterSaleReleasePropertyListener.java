package moe.ahao.commerce.market.adapter.mq;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.market.api.command.ReleaseUserCouponCommand;
import moe.ahao.commerce.market.application.ReleaseUserCouponAppService;
import moe.ahao.commerce.market.infrastructure.exception.MarketExceptionEnum;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 手动售后释放优惠券费者
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.AFTER_SALE_RELEASE_PROPERTY_TOPIC,
    consumerGroup = RocketMqConstant.AFTER_SALE_RELEASE_PROPERTY_CONSUMER_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class AfterSaleReleasePropertyListener extends AbstractRocketMqListener {
    @Autowired
    private ReleaseUserCouponAppService releaseUserCouponAppService;
    @Override
    public void onMessage(String message) {
        log.info("释放优惠券消息监听器收到message:{}", message);
        ReleaseUserCouponCommand event = JSONHelper.parse(message, ReleaseUserCouponCommand.class);
        // 释放优惠券
        Boolean result = releaseUserCouponAppService.releaseUserCoupon(event);
        if (Boolean.FALSE.equals(result)) {
            throw MarketExceptionEnum.CONSUME_MQ_FAILED.msg();
        }
    }
}
