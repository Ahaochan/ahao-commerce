//package com.ruyuan.eshop.order.mq.consumer.listener;
//
//import com.alibaba.fastjson.JSON;
//import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
//import com.ruyuan.eshop.common.constants.RedisLockKeyConstants;
//import com.ruyuan.eshop.common.constants.RocketMqConstant;
//import com.ruyuan.eshop.common.enums.OrderTypeEnum;
//import com.ruyuan.eshop.common.exception.BaseBizException;
//import com.ruyuan.eshop.common.message.PaidOrderSuccessMessage;
//import com.ruyuan.eshop.common.redis.RedisLock;
//import com.ruyuan.eshop.common.utils.LoggerFormat;
//import com.ruyuan.eshop.fulfill.domain.request.ReceiveFulfillRequest;
//import com.ruyuan.eshop.order.dao.OrderInfoDAO;
//import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
//import com.ruyuan.eshop.order.exception.OrderBizException;
//import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
//import com.ruyuan.eshop.order.mq.consumer.AbstractRocketMqListener;
//import com.ruyuan.eshop.order.remote.FulfillRemote;
//import com.ruyuan.eshop.order.service.OrderFulFillService;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.spring.annotation.ConsumeMode;
//import org.apache.rocketmq.spring.annotation.MessageModel;
//import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Objects;
//
///**
// * 监听订单支付成功后的消息
// *
// * @author zhonghuashishan
// * @version 1.0
// */
//@Slf4j
//@Component
//@RocketMQMessageListener(
//        topic = RocketMqConstant.ORDER_STD_CHANGE_EVENT_TOPIC,
//        consumerGroup = RocketMqConstant.ORDER_STD_CHANGE_EVENT_CONSUMER_GROUP,
//        selectorExpression = "paid || sub_paid",
//        consumeMode = ConsumeMode.CONCURRENTLY,
//        messageModel = MessageModel.CLUSTERING,
//        consumeThreadMax = 1
//)
//public class PaidOrderSuccessListener extends AbstractRocketMqListener {
//
//    @Autowired
//    private OrderInfoDAO orderInfoDAO;
//
//    @Autowired
//    private OrderFulFillService orderFulFillService;
//
//    @Autowired
//    private RedisLock redisLock;
//
//    @Autowired
//    private FulfillRemote fulfillRemote;
//
//    @Override
//    public void onMessage(String message) {
//        PaidOrderSuccessMessage paidOrderSuccessMessage = JSON.parseObject(message, PaidOrderSuccessMessage.class);
//        String orderId = paidOrderSuccessMessage.getOrderId();
//        log.info("触发订单履约，orderId:{}", orderId);
//
//        OrderInfoDO order = orderInfoDAO.getByOrderId(orderId);
//        if (Objects.isNull(order)) {
//            throw new OrderBizException(OrderErrorCodeEnum.ORDER_INFO_IS_NULL);
//        }
//
//        // 判断是否可以触发履约: 没有子订单并且不是虚拟订单可以履约
//        List<OrderInfoDO> subOrders = orderInfoDAO.listByParentOrderId(orderId);
//        if (canTriggerFulfill(order, subOrders)) {
//            // 1、加分布式锁+里面的履约前置状态校验防止消息重复消费
//            String key = RedisLockKeyConstants.ORDER_FULFILL_KEY + orderId;
//            if (!redisLock.tryLock(key)) {
//                log.error("order has not acquired lock，cannot fulfill, orderId={}", orderId);
//                throw new BaseBizException(OrderErrorCodeEnum.ORDER_FULFILL_ERROR);
//            }
//            try {
//                log.info(LoggerFormat.build()
//                        .remark("PaidOrderSuccessListener->onMessage")
//                        .data("msg", "2、将订单已履约")
//                        .data("orderId", orderId)
//                        .finish());
//
//                // 2、将订单已履约
//                orderFulFillService.triggerOrderFulFill(orderId);
//
//                log.info(LoggerFormat.build()
//                        .remark("PaidOrderSuccessListener->onMessage")
//                        .data("msg", "3、将订单推送至履约")
//                        .data("orderId", orderId)
//                        .finish());
//
//                // 3、将订单推送至履约
//                ReceiveFulfillRequest request = orderFulFillService.buildReceiveFulFillRequest(order);
//                fulfillRemote.receiveOrderFulFill(request);
//
//            } finally {
//                redisLock.unlock(key);
//            }
//        }
//    }
//
//
//    /**
//     * 判断是否可以触发履约
//     */
//    private boolean canTriggerFulfill(OrderInfoDO order, List<OrderInfoDO> subOrders) {
//        return CollectionUtils.isEmpty(subOrders) && OrderTypeEnum.canFulfillTypes().contains(order.getOrderType());
//    }
//}