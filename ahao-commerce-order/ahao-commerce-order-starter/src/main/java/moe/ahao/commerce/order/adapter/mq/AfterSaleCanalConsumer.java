package moe.ahao.commerce.order.adapter.mq;

import com.alibaba.otter.canal.protocol.FlatMessage;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleRefundDO;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.order.adapter.mq.handler.EsAfterSaleSyncHandler;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在canal.mq.dynamicTopic指定到这个topic上
 * 用于接收canal监听到的订单表日志变化
 * 目前逆向只监听了3张表：
 * after_sale_info、after_sale_item、after_sale_refund
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
public class AfterSaleCanalConsumer implements RocketMQListener<String> {
    public static final String UPDATE = "UPDATE";
    public static final String INSERT = "INSERT";
    public static final String DELETE = "DELETE";
    public static final String AFTER_SALE_ID = "after_sale_id";

    @Value("${canal.binlog.consumer.enable:false}")
    private Boolean enable;

    @Autowired
    private EsAfterSaleSyncHandler esAfterSaleSyncHandler;

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
        log.info("enable={}，canal aftersale forward 接收到消息 -> {}", enable, msg);
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
        List<String> afterSaleIds = parseAfterSaleIds(flatMessage.getData());
        if (afterSaleIds.isEmpty()) {
            return;
        }
        // 变更的表名
        String table = flatMessage.getTable();
        // 变更类型, INSERT或者UPDATE
        String type = flatMessage.getType();
        // 变更时间
        long timestamp = flatMessage.getTs();

        // 2. 按表和操作类型, 进行分发处理
        if (StringUtils.startsWith(table, AfterSaleInfoDO.TABLE_NAME)) {
            // 处理售后单after_sale_info表
            this.processAfterSaleInfoLog(type, afterSaleIds, timestamp);
        } else if (StringUtils.startsWith(table, AfterSaleItemDO.TABLE_NAME)) {
            // 处理售后单条目after_sale_item表
            this.processAfterSaleItemLog(type, afterSaleIds, timestamp);
        } else if (StringUtils.startsWith(table, AfterSaleRefundDO.TABLE_NAME)) {
            // 处理售后退款单after_sale_refund表
            this.processAfterSaleRefundLog(type, afterSaleIds, timestamp);
        }
    }

    /**
     * 处理售后单binlog日志
     */
    private void processAfterSaleInfoLog(String type, List<String> afterSaleIds, long timestamp) {
        if (StringUtils.equalsIgnoreCase(INSERT, type)) {
            // 处理订单新增binlog日志
            esAfterSaleSyncHandler.syncFullDataByAfterSaleIds(afterSaleIds);
        } else if (StringUtils.equalsIgnoreCase(UPDATE, type)) {
            // 处理订单更新binlog日志
            esAfterSaleSyncHandler.syncAfterSales(afterSaleIds, timestamp);
        }
    }


    /**
     * 处理售后条目binlog日志
     */
    private void processAfterSaleItemLog(String type, List<String> afterSaleIds, long timestamp) {
        if (StringUtils.equalsIgnoreCase(UPDATE, type)) {
            // 只需要处理售后单条目更新binlog日志
            esAfterSaleSyncHandler.syncAfterSaleItems(afterSaleIds, timestamp);
        }
    }


    /**
     * 处理售后退款单binlog日志
     */
    private void processAfterSaleRefundLog(String type, List<String> afterSaleIds, long timestamp) {
        if (StringUtils.equalsIgnoreCase(UPDATE, type)) {
            // 只需要处理售后退款单更新binlog日志
            esAfterSaleSyncHandler.syncAfterSaleRefund(afterSaleIds, timestamp);
        }
    }

    /**
     * 解析出afterSaleIds
     */
    private List<String> parseAfterSaleIds(List<Map<String, String>> rows) {
        return rows.stream()
            .map(row -> row.get(AFTER_SALE_ID))
            .collect(Collectors.toList());
    }
}
