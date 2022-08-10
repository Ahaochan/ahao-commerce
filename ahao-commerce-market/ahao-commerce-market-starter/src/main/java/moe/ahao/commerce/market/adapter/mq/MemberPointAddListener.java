package moe.ahao.commerce.market.adapter.mq;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.event.OrderStdChangeEvent;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.market.api.command.MemberPointIncreaseCommand;
import moe.ahao.commerce.market.application.MemberPointIncreaseAppService;
import moe.ahao.commerce.market.infrastructure.exception.MarketExceptionEnum;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.data.MemberPointDO;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.data.MemberPointDetailDO;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.mapper.MemberPointDetailMapper;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.mapper.MemberPointMapper;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 会员积分增加消费者
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.ORDER_STD_CHANGE_EVENT_TOPIC,
    consumerGroup = RocketMqConstant.MARKET_ORDER_STD_CHANGE_EVENT_CONSUMER_GROUP,
    selectorExpression = "paid || sub_paid",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class MemberPointAddListener extends AbstractRocketMqListener {
    private static final String tags = OrderStatusChangeEnum.ORDER_PAID.getTags() + " || " + OrderStatusChangeEnum.SUB_ORDER_PAID.getTags();
    private static final BigDecimal RATE = new BigDecimal("0.1");

    @Autowired
    private MemberPointIncreaseAppService memberPointIncreaseAppService;

    @Override
    public void onMessage(String message) {
        log.info("会员积分增加监听器:{}", message);

        OrderStdChangeEvent event = JSONHelper.parse(message, OrderStdChangeEvent.class);
        OrderStatusChangeEnum statusChange = event.getStatusChange();
        boolean isPaid = OrderStatusChangeEnum.ORDER_PAID.equals(statusChange) || OrderStatusChangeEnum.SUB_ORDER_PAID.equals(statusChange);
        if (!isPaid) {
            // 只有支付成功时才增加积分
            return;
        }

        String userId = event.getUserId();
        BigDecimal payAmount = event.getPayAmount();
        Integer increasedPoint = payAmount.multiply(RATE).setScale(0, RoundingMode.CEILING).intValue();

        MemberPointIncreaseCommand command = new MemberPointIncreaseCommand();
        command.setUserId(userId);
        command.setIncreasedPoint(increasedPoint);
        memberPointIncreaseAppService.increase(command);
    }
}
