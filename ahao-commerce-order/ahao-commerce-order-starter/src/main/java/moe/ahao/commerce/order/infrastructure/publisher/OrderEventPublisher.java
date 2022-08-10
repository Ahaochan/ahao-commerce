package moe.ahao.commerce.order.infrastructure.publisher;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.event.ActualRefundEvent;
import moe.ahao.commerce.common.event.OrderStdChangeEvent;
import moe.ahao.commerce.common.infrastructure.event.PayOrderTimeoutEvent;
import moe.ahao.commerce.common.infrastructure.event.ReleaseAssetsEvent;
import moe.ahao.commerce.common.infrastructure.rocketmq.RocketDelayedLevel;
import moe.ahao.commerce.customer.api.event.CustomerReceiveAfterSaleEvent;
import moe.ahao.commerce.market.api.command.ReleaseUserCouponCommand;
import moe.ahao.commerce.order.api.command.AfterSaleAuditPassReleaseAssetsEvent;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.tend.consistency.core.annotation.ConsistencyTask;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
public class OrderEventPublisher {
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private DefaultProducer defaultProducer;

    @ConsistencyTask(id = "sendOrderPayTimeoutDelayMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendPayOrderTimeoutEvent(PayOrderTimeoutEvent event) {
        String topic = RocketMqConstant.PAY_ORDER_TIMEOUT_DELAY_TOPIC;
        String msg = JSONHelper.toString(event);
        String tags = AfterSaleStatusChangeEnum.CANCEL_AFTER_SALE_CREATED.getTags();
        String keys = event.getOrderId();
        defaultProducer.sendMessage(topic, msg, RocketDelayedLevel.DELAYED_30m, tags, keys);
    }

    /**
     * 注意，一致性框架代理的方法不要传入大对象（比如OrderInfoDTO）,不然会导致插入一致性框架ruyuan_tend_consistency_task表的task_parameter巨长无比
     * 进而导致插入失败
     */
    @ConsistencyTask(id = "sendStandardOrderStatusChangeMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendStandardOrderStatusChangeMessage(String orderId, OrderStatusChangeEnum orderStatusChangeEnum) {
        // 1. 从数据库查询订单
        OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
        // 2. 构造标准订单变更消息
        OrderStdChangeEvent event = new OrderStdChangeEvent();
        event.setOrderId(orderId);
        event.setUserId(orderInfo.getUserId());
        event.setPayAmount(orderInfo.getPayAmount());
        event.setStatusChange(orderStatusChangeEnum);
        // 3. 发送标准变更消息
        String topic = RocketMqConstant.ORDER_STD_CHANGE_EVENT_TOPIC;
        String msg = JSONHelper.toString(event);
        String tags = orderStatusChangeEnum.getTags();
        String keys = orderId;
        if (StringUtils.isBlank(msg)) {
            return;
        }
        defaultProducer.sendMessage(topic, msg, -1, tags, keys, new MessageQueueSelector() {
            @Override
            public MessageQueue select(List<MessageQueue> mqs, Message message, Object arg) {
                // 根据订单id选择发送queue
                String orderId = (String) arg;
                // 解决取模可能为负数的情况
                long index = (orderId.hashCode() & Integer.MAX_VALUE) % mqs.size();
                return mqs.get((int) index);
            }
        }, orderId);
    }

    @ConsistencyTask(id = "sendLackItemRefundMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendLackItemRefundMessage(ActualRefundEvent event) {
        String topic = RocketMqConstant.ACTUAL_REFUND_TOPIC;
        String msg = JSONHelper.toString(event);
        String tags = null;
        String keys = null;
        defaultProducer.sendMessage(topic, msg, -1, tags, keys);
    }

    @ConsistencyTask(id = "sendReleaseAssetsMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendReleaseAssetsMessage(ReleaseAssetsEvent event) {
        String topic = RocketMqConstant.RELEASE_ASSETS_TOPIC;
        String msg = JSONHelper.toString(event);
        String tags = AfterSaleStatusChangeEnum.CANCEL_AFTER_SALE_CREATED.getTags();
        String keys = event.getOrderId();

        defaultProducer.sendMessage(topic, msg, -1, tags, keys);
    }

    @ConsistencyTask(id = "sendCancelOrderRefundMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendCancelOrderRefundMessage(ActualRefundEvent event) {
        String topic = RocketMqConstant.ACTUAL_REFUND_TOPIC;
        String msg = JSONHelper.toString(event);
        String tags = AfterSaleStatusChangeEnum.CANCEL_AFTER_SALE_CREATED.getTags();
        String keys = event.getOrderId();

        defaultProducer.sendMessage(topic, msg, -1, tags, keys);
    }

    @ConsistencyTask(id = "sendAfterSaleRefundMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendAfterSaleRefundMessage(CustomerReceiveAfterSaleEvent event) {
        String topic = RocketMqConstant.AFTER_SALE_CUSTOMER_AUDIT_TOPIC;
        String msg = JSONHelper.toString(event);
        String tags = AfterSaleStatusChangeEnum.AFTER_SALE_CREATED.getTags();
        String keys = event.getOrderId();

        defaultProducer.sendMessage(topic, msg, -1, tags, keys);
    }

    @ConsistencyTask(id = "sendAuditPassMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendAuditPassMessage(AfterSaleAuditPassReleaseAssetsEvent event) {
        String topic = RocketMqConstant.CUSTOMER_AUDIT_PASS_RELEASE_ASSETS_TOPIC;
        String msg = JSONHelper.toString(event);
        String tags = AfterSaleStatusChangeEnum.AFTER_SALE_REVIEWED_PASS.getTags();
        String keys = event.getOrderId();

        defaultProducer.sendMessage(topic, msg, -1, tags, keys);
    }

    @ConsistencyTask(id = "sendAfterSaleReleaseCouponMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendAfterSaleReleaseCouponMessage(ReleaseUserCouponCommand command) {
        String topic = RocketMqConstant.AFTER_SALE_RELEASE_PROPERTY_TOPIC;
        String msg = JSONHelper.toString(command);
        String tags = AfterSaleStatusChangeEnum.AFTER_SALE_REFUNDING.getTags();
        String keys = command.getOrderId();

        defaultProducer.sendMessage(topic, msg, -1, tags, keys);
    }
}
