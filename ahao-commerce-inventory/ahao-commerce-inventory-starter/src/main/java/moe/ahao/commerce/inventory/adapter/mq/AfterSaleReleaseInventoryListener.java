package moe.ahao.commerce.inventory.adapter.mq;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractMessageListenerConcurrently;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.inventory.api.command.ReleaseProductStockCommand;
import moe.ahao.commerce.inventory.application.ReleaseProductStockAppService;
import moe.ahao.commerce.inventory.infrastructure.exception.InventoryExceptionEnum;
import moe.ahao.commerce.inventory.infrastructure.gateway.feign.AfterSaleQueryFeignClient;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 监听释放库存消息
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.AFTER_SALE_RELEASE_INVENTORY_TOPIC,
    consumerGroup = RocketMqConstant.AFTER_SALE_RELEASE_INVENTORY_CONSUMER_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class AfterSaleReleaseInventoryListener extends AbstractRocketMqListener {

    @Autowired
    private ReleaseProductStockAppService releaseProductStockAppService;

    @Override
    public void onMessage(String message) {
        log.info("释放库存消息监听器收到message:{}", message);
        // 封装释放库存参数
        ReleaseProductStockCommand command = JSONHelper.parse(message, ReleaseProductStockCommand.class);
        // 释放库存
        boolean success = releaseProductStockAppService.releaseProductStock(command);
        if (!success) {
            throw InventoryExceptionEnum.CONSUME_MQ_FAILED.msg();
        }
    }
}
