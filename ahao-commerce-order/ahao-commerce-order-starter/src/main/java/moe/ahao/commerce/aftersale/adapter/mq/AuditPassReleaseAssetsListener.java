package moe.ahao.commerce.aftersale.adapter.mq;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleItemMapper;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.enums.AfterSaleItemTypeEnum;
import moe.ahao.commerce.common.enums.AfterSaleLastOrderItemEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleTypeEnum;
import moe.ahao.commerce.common.event.ActualRefundEvent;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractMessageListenerConcurrently;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.inventory.api.command.ReleaseProductStockCommand;
import moe.ahao.commerce.inventory.api.event.ReleaseProductStockEvent;
import moe.ahao.commerce.order.api.command.AfterSaleAuditPassReleaseAssetsEvent;
import moe.ahao.commerce.order.infrastructure.publisher.DefaultProducer;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderItemDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderItemMapper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static moe.ahao.commerce.common.constants.RocketMqConstant.CUSTOMER_AUDIT_PASS_RELEASE_ASSETS_CONSUMER_GROUP;
import static moe.ahao.commerce.common.constants.RocketMqConstant.CUSTOMER_AUDIT_PASS_RELEASE_ASSETS_TOPIC;

/**
 * 接收客服审核通过后的 监听 释放资产消息
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.CUSTOMER_AUDIT_PASS_RELEASE_ASSETS_TOPIC,
    consumerGroup = RocketMqConstant.CUSTOMER_AUDIT_PASS_RELEASE_ASSETS_CONSUMER_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class AuditPassReleaseAssetsListener extends AbstractRocketMqListener {
    @Autowired
    private DefaultProducer defaultProducer;

    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private AfterSaleItemMapper afterSaleItemMapper;

    @Override
    public void onMessage(String message) {
        // 1. 消费到释放资产message
        log.info("接收到客服审核通过后释放资产的消息:{}", message);
        AfterSaleAuditPassReleaseAssetsEvent event = JSONHelper.parse(message, AfterSaleAuditPassReleaseAssetsEvent.class);
        String afterSaleId = event.getAfterSaleId();
        List<AfterSaleItemDO> afterSaleItems = afterSaleItemMapper.selectListByAfterSaleId(afterSaleId);

        // 2. 发送释放库存MQ
        this.sendReleaseProductStockEvent(event, afterSaleItems);

        // 3. 发送实际退款
        this.sendRefundEvent(event, afterSaleItems);
    }

    /**
     * 释放库存数据
     */
    private void sendReleaseProductStockEvent(AfterSaleAuditPassReleaseAssetsEvent event, List<AfterSaleItemDO> afterSaleItems) {
        String orderId = event.getOrderId();

        List<ReleaseProductStockCommand.OrderItem> orderItems = new ArrayList<>();
        for (AfterSaleItemDO afterSaleItem : afterSaleItems) {
            // 过滤掉运费和优惠券条目
            if(Objects.equals(afterSaleItem.getAfterSaleItemType(), AfterSaleItemTypeEnum.AFTER_SALE_ORDER_ITEM.getCode())) {
                ReleaseProductStockCommand.OrderItem orderItem = new ReleaseProductStockCommand.OrderItem();
                orderItem.setSkuCode(afterSaleItem.getSkuCode());
                orderItem.setSaleQuantity(afterSaleItem.getReturnQuantity());

                orderItems.add(orderItem);
            }
        }

        ReleaseProductStockCommand releaseProductStockEvent = new ReleaseProductStockCommand();
        releaseProductStockEvent.setOrderId(orderId);
        releaseProductStockEvent.setOrderItems(orderItems);

        String topic = RocketMqConstant.AFTER_SALE_RELEASE_INVENTORY_TOPIC;
        String msg = JSONHelper.toString(releaseProductStockEvent);
        String tags = null;
        String keys = null;
        defaultProducer.sendMessage(topic, msg, -1, tags, keys);
    }

    private void sendRefundEvent(AfterSaleAuditPassReleaseAssetsEvent command, List<AfterSaleItemDO> afterSaleItems) {
        String orderId = command.getOrderId();
        String afterSaleId = command.getAfterSaleId();
        // 实际退款数据
        ActualRefundEvent event = new ActualRefundEvent();
        event.setAfterSaleId(afterSaleId);
        event.setOrderId(orderId);
        event.setAfterSaleType(AfterSaleTypeEnum.RETURN_GOODS.getCode());

        String topic = RocketMqConstant.ACTUAL_REFUND_TOPIC;
        String msg = JSONHelper.toString(event);
        String tags = null;
        String keys = null;
        defaultProducer.sendMessage(topic, msg, -1, tags, keys);
    }
}
