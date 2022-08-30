package com.ruyuan.eshop.order.mq.consumer.listener;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruyuan.eshop.common.constants.RocketMqConstant;
import com.ruyuan.eshop.order.constants.BinlogTableConstant;
import com.ruyuan.eshop.order.elasticsearch.handler.aftersale.EsAfterSaleInsertHandler;
import com.ruyuan.eshop.order.elasticsearch.handler.aftersale.EsAfterSaleItemUpdateHandler;
import com.ruyuan.eshop.order.elasticsearch.handler.aftersale.EsAfterSaleRefundUpdateHandler;
import com.ruyuan.eshop.order.elasticsearch.handler.aftersale.EsAfterSaleUpdateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于接收canal监听到的订单表日志变化
 * <p>
 * 目前逆向只监听了两张表：
 * after_sale_info、after_sale_item、after_sale_refund
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = RocketMqConstant.ORDER_REVERSE,
        consumerGroup = RocketMqConstant.ORDER_REVERSE_GROUP,
        selectorExpression = "*",
        consumeMode = ConsumeMode.ORDERLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadMax = 1
)
public class ReverseOrderInfoBinlogConsumer implements RocketMQListener<String> {

    public static final String UPDATE = "UPDATE";
    public static final String INSERT = "INSERT";
    public static final String DELETE = "DELETE";

    public static final String TABLE = "table";
    public static final String TYPE = "type";
    public static final String DATA = "data";
    public static final String OLD = "old";
    public static final String TIME_STAMP = "ts";
    public static final String IS_DDL = "isDdl";
    public static final String AFTER_SALE_ID = "after_sale_id";

    @Autowired
    private EsAfterSaleInsertHandler esAfterSaleInsertHandler;

    @Autowired
    private EsAfterSaleUpdateHandler esAfterSaleUpdateHandler;

    @Autowired
    private EsAfterSaleItemUpdateHandler esAfterSaleItemUpdateHandler;

    @Autowired
    private EsAfterSaleRefundUpdateHandler esAfterSaleRefundUpdateHandler;

    @Value("${canal.binlog.consumer.enable}")
    private Boolean enable;

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

        log.info("enable={}，canal order reverse 接收到消息 -> {}", enable,msg);



        JSONObject jsonObject = JSONObject.parseObject(msg);
        String table = jsonObject.getString(TABLE);
        String type = jsonObject.getString(TYPE);
        long timestamp = jsonObject.getLong(TIME_STAMP);
        JSONArray dataArray = jsonObject.getJSONArray(DATA);

        if (jsonObject.getBoolean(IS_DDL)) {
            // 如果是ddl的binlog，直接忽略
            return;
        }

        if (StringUtils.startsWith(table, BinlogTableConstant.AFTER_SALE_INFO)) {
            // 处理售后单binlog日志
            processAfterSaleInfoLog(type, dataArray, timestamp);
        } else if (StringUtils.startsWith(table, BinlogTableConstant.AFTER_SALE_ITEM)) {
            // 处理售后单条目binlog日志
            processAfterSaleItemLog(type, dataArray, timestamp);
        } else if (StringUtils.startsWith(table, BinlogTableConstant.AFTER_SALE_REFUND)) {
            // 处理售后退款单binlog日志
            processAfterSaleRefundLog(type, dataArray, timestamp);
        }
    }

    /**
     * 处理售后单binlog日志
     */
    private void processAfterSaleInfoLog(String type, JSONArray dataArray, long timestamp) throws Exception {
        // 解析出售后单ID
        List<String> afterSaleIds = parseAfterSaleIds(dataArray);
        if (afterSaleIds.isEmpty()) {
            return;
        }

        if (StringUtils.equals(INSERT, type)) {
            // 处理订单新增binlog日志
            esAfterSaleInsertHandler.sync(afterSaleIds, timestamp);
        } else if (StringUtils.equals(UPDATE, type)) {
            // 处理订单更新binlog日志
            esAfterSaleUpdateHandler.sync(afterSaleIds, timestamp);
        }
    }


    /**
     * 处理售后条目binlog日志
     */
    private void processAfterSaleItemLog(String type, JSONArray dataArray, long timestamp) throws Exception {
        // 解析出售后单ID
        List<String> afterSaleIds = parseAfterSaleIds(dataArray);
        if (afterSaleIds.isEmpty()) {
            return;
        }

        if (StringUtils.equals(UPDATE, type)) {
            // 只需要处理售后单条目更新binlog日志
            esAfterSaleItemUpdateHandler.sync(afterSaleIds, timestamp);
        }
    }


    /**
     * 处理售后退款单binlog日志
     */
    private void processAfterSaleRefundLog(String type, JSONArray dataArray, long timestamp) throws Exception {
        // 解析出售后单ID
        List<String> afterSaleIds = parseAfterSaleIds(dataArray);
        if (afterSaleIds.isEmpty()) {
            return;
        }

        if (StringUtils.equals(UPDATE, type)) {
            // 只需要处理售后退款单更新binlog日志
            esAfterSaleRefundUpdateHandler.sync(afterSaleIds, timestamp);
        }
    }

    /**
     * 解析出afterSaleIds
     */
    private List<String> parseAfterSaleIds(JSONArray dataArray) {
        List<String> afterSaleIds = new ArrayList<>();
        for (Object o : dataArray) {
            JSONObject jsonObject = (JSONObject) o;
            afterSaleIds.add(jsonObject.getString(AFTER_SALE_ID));
        }
        return afterSaleIds;
    }

}