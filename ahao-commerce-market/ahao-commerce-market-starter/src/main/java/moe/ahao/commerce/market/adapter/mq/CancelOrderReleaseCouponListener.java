package moe.ahao.commerce.market.adapter.mq;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.event.ReleaseAssetsEvent;
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
 * 取消订单释放优惠券消费者
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.RELEASE_ASSETS_TOPIC,
    consumerGroup = RocketMqConstant.RELEASE_PROPERTY_CONSUMER_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class CancelOrderReleaseCouponListener extends AbstractRocketMqListener {
    @Autowired
    private ReleaseUserCouponAppService releaseUserCouponAppService;

    @Override
    public void onMessage(String message) {
        log.info("释放优惠券消息监听器收到message:{}", message);
        ReleaseAssetsEvent event = JSONHelper.parse(message, ReleaseAssetsEvent.class);
        ReleaseUserCouponCommand command = this.buildCommand(event);
        boolean result = releaseUserCouponAppService.releaseUserCoupon(command);
        if (!result) {
            throw MarketExceptionEnum.CONSUME_MQ_FAILED.msg();
        }
    }

    private ReleaseUserCouponCommand buildCommand(ReleaseAssetsEvent event) {
        ReleaseUserCouponCommand command = new ReleaseUserCouponCommand();
        command.setUserId(event.getUserId());
        command.setCouponId(event.getCouponId());
        command.setOrderId(event.getOrderId());
        command.setAfterSaleId(null);
        return command;
    }
}
