package com.ruyuan.eshop.order.mq.consumer.listener;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruyuan.eshop.common.constants.RocketMqConstant;
import com.ruyuan.eshop.order.constants.BinlogTableConstant;
import com.ruyuan.eshop.order.elasticsearch.handler.order.EsOrderDeliveryDetailUpdateHandler;
import com.ruyuan.eshop.order.elasticsearch.handler.order.EsOrderInfoInsertHandler;
import com.ruyuan.eshop.order.elasticsearch.handler.order.EsOrderPaymentDetailUpdateHandler;
import com.ruyuan.eshop.order.elasticsearch.handler.order.EsOrderUpdateHandler;
import com.ruyuan.eshop.order.mq.consumer.AbstractRocketMqListener;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于接收canal监听到的订单表日志变化
 * <p>
 * 目前正向只监听了三张表：
 * order_info、order_delivery_detail、order_payment_detail
 *
 * @author zhonghuashishan
 * @version 1.0
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
public class OrderInfoBinlogConsumer extends AbstractRocketMqListener {

    public static final String UPDATE = "UPDATE";
    public static final String INSERT = "INSERT";
    public static final String DELETE = "DELETE";

    public static final String TABLE = "table";
    public static final String TYPE = "type";
    public static final String DATA = "data";
    public static final String TIME_STAMP = "ts";
    public static final String OLD = "old";
    public static final String IS_DDL = "isDdl";
    public static final String ORDER_ID = "order_id";

    @Value("${canal.binlog.consumer.enable}")
    private Boolean enable;

    @Autowired
    private EsOrderInfoInsertHandler esOrderInfoInsertHandler;

    @Autowired
    private EsOrderUpdateHandler esOrderUpdateHandler;

    @Autowired
    private EsOrderDeliveryDetailUpdateHandler esOrderDeliveryDetailUpdateService;

    @Autowired
    private EsOrderPaymentDetailUpdateHandler esOrderPaymentDetailUpdateHandler;

    /**
     * 已测试
     * DefaultRocketMQListenerContainer#DefaultMessageListenerOrderly
     * 在发生错误时候 会返回ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT
     *
     * @param msg canal监听到的binlog日志
     */
    @SneakyThrows
    @Override
    public void onMessage(String msg) {
        if(!enable) {
            // log.info("binlog消费未启用！！");
            return;
        }
        log.info("enable={}，canal order forward 接收到消息 -> {}", enable,msg);


        JSONObject jsonObject = JSONObject.parseObject(msg);
        String table = jsonObject.getString(TABLE);
        String type = jsonObject.getString(TYPE);
        long timestamp = jsonObject.getLong(TIME_STAMP);
        JSONArray dataArray = jsonObject.getJSONArray(DATA);

        if (jsonObject.getBoolean(IS_DDL)) {
            // 如果是ddl的binlog，直接忽略
            return;
        }

        if (StringUtils.startsWith(table, BinlogTableConstant.ORDER_INFO)) {
            // 处理订单binlog日志
            processOrderInfoLog(type, dataArray, timestamp);
        } else if (StringUtils.startsWith(table, BinlogTableConstant.ORDER_DELIVERY_DETAIL)) {
            // 处理订单配送信息binlog日志
            processOrderDeliveryDetailLog(type, dataArray, timestamp);
        } else if (StringUtils.startsWith(table, BinlogTableConstant.ORDER_PAYMENT_DETAIL)) {
            // 处理订单支付信息binlog日志
            processOrderPaymentDetailLog(type, dataArray, timestamp);
        }

    }

    /**
     * 处理订单binlog日志
     */
    private void processOrderInfoLog(String type, JSONArray dataArray, long timestamp) throws Exception {
        // 解析出订单ID
        List<String> orderIds = parseOrderIds(dataArray);
        if (orderIds.isEmpty()) {
            return;
        }

        if (StringUtils.equals(INSERT, type)) {
            // 处理订单新增binlog日志
            esOrderInfoInsertHandler.sync(orderIds,timestamp);
        } else if (StringUtils.equals(UPDATE, type)) {
            // 处理订单更新binlog日志
            esOrderUpdateHandler.sync(orderIds,timestamp);
        }
    }


    /**
     * 处理订单配送信息binlog日志
     */
    private void processOrderDeliveryDetailLog(String type, JSONArray dataArray, long timestamp) throws Exception {

        // 解析出订单ID
        List<String> orderIds = parseOrderIds(dataArray);
        if (orderIds.isEmpty()) {
            return;
        }

        if (StringUtils.equals(UPDATE, type)) {
            // 只需要处理订单配送信息更新binlog日志
            esOrderDeliveryDetailUpdateService.sync(orderIds, timestamp);
        }
    }


    /**
     * 处理订单支付信息binlog日志
     */
    private void processOrderPaymentDetailLog(String type, JSONArray dataArray, long timestamp) throws Exception {

        // 解析出订单ID
        List<String> orderIds = parseOrderIds(dataArray);
        if (orderIds.isEmpty()) {
            return;
        }

        if (StringUtils.equals(UPDATE, type)) {
            // 只需要处理订单支付明细更新binlog日志
            esOrderPaymentDetailUpdateHandler.sync(orderIds, timestamp);
        }
    }


    /**
     * 解析出orderIds
     */
    private List<String> parseOrderIds(JSONArray dataArray) {
        List<String> orderIds = new ArrayList<>();
        for (Object o : dataArray) {
            JSONObject jsonObject = (JSONObject) o;
            orderIds.add(jsonObject.getString(ORDER_ID));
        }
        return orderIds;
    }


}