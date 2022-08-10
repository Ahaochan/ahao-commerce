package moe.ahao.commerce.order.adapter.mq;

import com.alibaba.otter.canal.protocol.FlatMessage;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractRocketMqListener;
import moe.ahao.commerce.order.adapter.mq.handler.EsOrderSyncHandler;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderDeliveryDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderPaymentDetailDO;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在canal.mq.dynamicTopic指定到这个topic上
 * 用于接收canal监听到的订单表日志变化
 * 目前正向只监听了3张表：
 * order_info、order_delivery_detail、order_payment_detail
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.ORDER_FORWARD_TOPIC,
    consumerGroup = RocketMqConstant.ORDER_FORWARD_GROUP,
    selectorExpression = "*",
    consumeMode = ConsumeMode.ORDERLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMax = 1
)
public class OrderCanalConsumer extends AbstractRocketMqListener {
    public static final String UPDATE = "UPDATE";
    public static final String INSERT = "INSERT";
    public static final String DELETE = "DELETE";
    public static final String ORDER_ID = "order_id";

    @Value("${canal.binlog.consumer.enable:false}")
    private Boolean enable;

    @Autowired
    private EsOrderSyncHandler esOrderSyncHandler;

    /**
     * 已测试
     * DefaultRocketMQListenerContainer#DefaultMessageListenerOrderly
     * 在发生错误时候 会返回ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT
     *
     * @param msg canal监听到的binlog日志
     */
    @Override
    public void onMessage(String msg) {
        // 1. 校验和提取数据
        log.info("enable={}，canal order forward 接收到消息 -> {}", enable, msg);
        if (!enable) {
            log.info("binlog消费未启用！！");
            return;
        }
        FlatMessage flatMessage = JSONHelper.parse(msg, FlatMessage.class);
        if (Boolean.TRUE.equals(flatMessage.getIsDdl())) {
            // 如果是ddl的binlog，直接忽略
            return;
        }
        // 变更的订单id
        List<String> orderIds = parseOrderIds(flatMessage.getData());
        if (orderIds.isEmpty()) {
            return;
        }
        // 变更的表名
        String table = flatMessage.getTable();
        // 变更类型, INSERT或者UPDATE
        String type = flatMessage.getType();
        // 变更时间
        long timestamp = flatMessage.getTs();

        // 2. 按表和操作类型, 进行分发处理
        if (StringUtils.startsWith(table, OrderInfoDO.TABLE_NAME)) {
            // 处理订单表order_info表
            this.processOrderInfo(type, orderIds, timestamp);
        } else if (StringUtils.startsWith(table, OrderDeliveryDetailDO.TABLE_NAME)) {
            // 处理订单配送表order_delivery_detail表
            this.processOrderDeliveryDetail(type, orderIds, timestamp);
        } else if (StringUtils.startsWith(table, OrderPaymentDetailDO.TABLE_NAME)) {
            // 处理订单支付表order_payment_detail表
            this.processOrderPaymentDetail(type, orderIds, timestamp);
        }
    }

    /**
     * 处理订单binlog日志
     */
    private void processOrderInfo(String type, List<String> orderIds, long timestamp) {
        if (StringUtils.equalsIgnoreCase(INSERT, type)) {
            // 处理订单新增binlog日志
            esOrderSyncHandler.syncFullDataByOrderIds(orderIds);
        } else if (StringUtils.equalsIgnoreCase(UPDATE, type)) {
            // 处理订单更新binlog日志
            esOrderSyncHandler.syncOrderInfos(orderIds, timestamp);
        }
    }


    /**
     * 处理订单配送信息binlog日志
     */
    private void processOrderDeliveryDetail(String type, List<String> orderIds, long timestamp) {
        if (StringUtils.equalsIgnoreCase(UPDATE, type)) {
            // 只需要处理订单配送信息更新binlog日志
            esOrderSyncHandler.syncOrderDeliveryDetails(orderIds, timestamp);
        }
    }


    /**
     * 处理订单支付信息binlog日志
     */
    private void processOrderPaymentDetail(String type, List<String> orderIds, long timestamp) {
        if (StringUtils.equalsIgnoreCase(UPDATE, type)) {
            // 只需要处理订单支付明细更新binlog日志
            esOrderSyncHandler.syncOrderPaymentDetails(orderIds, timestamp);
        }
    }

    /**
     * 解析出orderIds
     */
    private List<String> parseOrderIds(List<Map<String, String>> rows) {
        return rows.stream()
            .map(row -> row.get(ORDER_ID))
            .collect(Collectors.toList());
    }
}
