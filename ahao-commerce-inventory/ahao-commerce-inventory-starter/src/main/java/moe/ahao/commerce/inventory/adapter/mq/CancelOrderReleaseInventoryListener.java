package moe.ahao.commerce.inventory.adapter.mq;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.event.ReleaseAssetsEvent;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractMessageListenerConcurrently;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.inventory.api.command.ReleaseProductStockCommand;
import moe.ahao.commerce.inventory.application.ReleaseProductStockAppService;
import moe.ahao.commerce.inventory.infrastructure.exception.InventoryExceptionEnum;
import moe.ahao.commerce.inventory.infrastructure.gateway.feign.OrderQueryFeignClient;
import moe.ahao.commerce.order.api.dto.OrderDetailDTO;
import moe.ahao.domain.entity.Result;
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
    topic = RocketMqConstant.RELEASE_ASSETS_TOPIC,
    consumerGroup = RocketMqConstant.RELEASE_INVENTORY_CONSUMER_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class CancelOrderReleaseInventoryListener extends AbstractRocketMqListener {
    @Autowired
    private ReleaseProductStockAppService releaseProductStockAppService;
    @Autowired
    private OrderQueryFeignClient orderQueryFeignClient;

    @Override
    public void onMessage(String message) {
        log.info("释放库存消息监听器收到message:{}", message);
        ReleaseAssetsEvent event = JSONHelper.parse(message, ReleaseAssetsEvent.class);
        ReleaseProductStockCommand releaseProductStockCommand = this.buildCommand(event);
        boolean success = releaseProductStockAppService.releaseProductStock(releaseProductStockCommand);
        if (!success) {
            throw InventoryExceptionEnum.CONSUME_MQ_FAILED.msg();
        }
    }

    private ReleaseProductStockCommand buildCommand(ReleaseAssetsEvent event) {
        // 1. 查询订单条目
        String orderId = event.getOrderId();
        Result<List<OrderDetailDTO.OrderItemDTO>> result = orderQueryFeignClient.orderItemDetail(orderId);
        List<OrderDetailDTO.OrderItemDTO> orderItemDTOList = result.getObj();

        // 2. 拼装参数
        List<ReleaseProductStockCommand.OrderItem> orderItems = new ArrayList<>();
        for (OrderDetailDTO.OrderItemDTO orderItemDTO : orderItemDTOList) {
            ReleaseProductStockCommand.OrderItem orderItem = new ReleaseProductStockCommand.OrderItem();
            orderItem.setSkuCode(orderItemDTO.getSkuCode());
            orderItem.setSaleQuantity(orderItemDTO.getSaleQuantity());
            orderItems.add(orderItem);
        }
        ReleaseProductStockCommand command = new ReleaseProductStockCommand();
        command.setOrderId(orderId);
        command.setOrderItems(orderItems);
        return command;
    }
}
