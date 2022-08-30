package com.ruyuan.eshop.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ruyuan.consistency.annotation.ConsistencyTask;
import com.ruyuan.eshop.common.constants.RocketDelayedLevel;
import com.ruyuan.eshop.common.constants.RocketMqConstant;
import com.ruyuan.eshop.common.core.DateProvider;
import com.ruyuan.eshop.common.enums.*;
import com.ruyuan.eshop.common.message.OrderStdChangeEvent;
import com.ruyuan.eshop.common.message.RefundMessage;
import com.ruyuan.eshop.common.utils.LoggerFormat;
import com.ruyuan.eshop.customer.domain.request.CustomerReceiveAfterSaleRequest;
import com.ruyuan.eshop.market.domain.request.ReleaseUserCouponRequest;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.OrderDeliveryDetailDAO;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.dto.SendLackItemRefundEventDTO;
import com.ruyuan.eshop.order.domain.dto.SendOrderStdEventDTO;
import com.ruyuan.eshop.order.domain.entity.OrderDeliveryDetailDO;
import com.ruyuan.eshop.order.domain.request.*;
import com.ruyuan.eshop.order.mq.producer.DefaultProducer;
import com.ruyuan.eshop.order.service.RocketMqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.ruyuan.eshop.common.constants.RocketMqConstant.ACTUAL_REFUND_TOPIC;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Service
public class RocketMqServiceImpl implements RocketMqService {

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderConverter orderConverter;

    @Autowired
    private DefaultProducer defaultProducer;

    @Autowired
    private DateProvider dateProvider;

    @Autowired
    private OrderDeliveryDetailDAO deliveryDetailDAO;

    @Override
    @ConsistencyTask(id = "sendOrderPayTimeoutDelayMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendOrderPayTimeoutDelayMessage(OrderPaymentDelayRequest orderPaymentDelayRequest) {
        String orderId = orderPaymentDelayRequest.getOrderId();
        Map<String, String> map = new HashMap<>(16);
        map.put("orderId", orderId);
        defaultProducer.sendMessage(
                RocketMqConstant.PAY_ORDER_TIMEOUT_DELAY_TOPIC,
                JSONObject.toJSONString(map),
                RocketDelayedLevel.DELAYED_30m,
                "支付订单超时延迟消息",
                AfterSaleStateMachineChangeEnum.CANCEL_ORDER.getTags(),
                orderId);
    }

    /**
     * 注意，一致性框架代理的方法不要传入大对象（比如OrderInfoDTO）,不然会导致插入一致性框架ruyuan_tend_consistency_task表的task_parameter巨长无比
     * 进而导致插入失败
     */
    @Override
    @ConsistencyTask(id = "sendStandardOrderStatusChangeMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendStandardOrderStatusChangeMessage(SendOrderStdEventDTO sendOrderStdEventDTO) {
        String orderId = sendOrderStdEventDTO.getOrderId();
        OrderStatusChangeEnum orderStatusChangeEnum = sendOrderStdEventDTO.getOrderStatusChangeEnum();

        // 1、从数据库查询订单
        OrderInfoDTO orderInfoDTO = orderConverter.orderInfoDO2DTO(orderInfoDAO.getByOrderId(orderId));

        // 2、构造标准订单变更消息
        OrderStdChangeEvent stdChangeEvent = buildStdEvent(orderStatusChangeEnum, orderInfoDTO);

        // 3、发送标准变更消息
        sendMessage(JSONObject.toJSONString(stdChangeEvent), stdChangeEvent.getOrderId(), orderStatusChangeEnum.getTags());
    }

    @Override
    @ConsistencyTask(id = "sendLackItemRefundMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendLackItemRefundMessage(SendLackItemRefundEventDTO sendLackItemRefundEventDTO) {
        RefundMessage refundMessage = new RefundMessage();
        refundMessage.setOrderId(sendLackItemRefundEventDTO.getOrderId());
        refundMessage.setAfterSaleId(sendLackItemRefundEventDTO.getAfterSaleId());
        defaultProducer.sendMessage(ACTUAL_REFUND_TOPIC, JSON.toJSONString(refundMessage), "缺品退款消息",
                null, null);
    }

    public OrderStdChangeEvent buildStdEvent(OrderStatusChangeEnum event, OrderInfoDTO order) {
        // 构造正向订单标准变更消息
        OrderStdChangeEvent stdChangeEvent = OrderStdChangeEvent.builder()
                .orderId(order.getOrderId())
                .businessIdentifier(BusinessIdentifierEnum.getByCode(order.getBusinessIdentifier()))
                .orderType(OrderTypeEnum.getByCode(order.getOrderType()))
                .businessOrderId(order.getBusinessOrderId())
                .parentOrderId(order.getParentOrderId())
                .payAmount(order.getPayAmount())
                .statusChange(event)
                .payTime(dateProvider.formatDatetime(order.getPayTime()))
                .payType(order.getPayType())
                .sellerId(order.getSellerId())
                .totalAmount(order.getTotalAmount())
                .userId(order.getUserId())
                .build();

        // 如果是出库后的状态
        if (OrderStatusEnum.afterOutStockStatus().contains(order.getOrderStatus())) {
            OrderDeliveryDetailDO deliveryDetail = deliveryDetailDAO.getByOrderId(order.getOrderId());
            stdChangeEvent.setOutStockTime(dateProvider.formatDatetime(deliveryDetail.getOutStockTime()));
            stdChangeEvent.setSignedTime(dateProvider.formatDatetime(deliveryDetail.getSignedTime()));
        }
        return stdChangeEvent;
    }

    /**
     * hash
     *
     * @param orderId 订单id
     * @return hash后的值
     */
    private int hash(String orderId) {
        //解决取模可能为负数的情况
        return orderId.hashCode() & Integer.MAX_VALUE;
    }

    private void sendMessage(String body, String orderId, String tags) {
        log.info(LoggerFormat.build()
                .remark("OrderStateAction->sendMessage")
                .data("body", body)
                .data("orderId", orderId)
                .data("tags", tags)
                .finish());
        if (StringUtils.isBlank(body)) {
            return;
        }
        defaultProducer.sendMessage(RocketMqConstant.ORDER_STD_CHANGE_EVENT_TOPIC, body,
                -1, "订单变更标准消息",
                tags, orderId, (mqs, message, arg) -> {
                    //根据订单id选择发送queue
                    String orderId1 = (String) arg;
                    long index = hash(orderId1) % mqs.size();
                    return mqs.get((int) index);
                }, orderId);
    }

    @Override
    @ConsistencyTask(id = "sendReleaseAssetsMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendReleaseAssetsMessage(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        defaultProducer.sendMessage(
                RocketMqConstant.RELEASE_ASSETS_TOPIC,
                JSONObject.toJSONString(cancelOrderAssembleRequest),
                -1,
                "取消订单释放资产权益消息",
                AfterSaleStateMachineChangeEnum.CANCEL_ORDER.getTags(),
                cancelOrderAssembleRequest.getOrderId()
        );
    }

    @Override
    @ConsistencyTask(id = "sendCancelOrderRefundMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendCancelOrderRefundMessage(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        RefundMessage refundMessage = new RefundMessage();
        refundMessage.setOrderId(cancelOrderAssembleRequest.getOrderId());
        refundMessage.setAfterSaleId(cancelOrderAssembleRequest.getAfterSaleId());
        refundMessage.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());

        defaultProducer.sendMessage(
                RocketMqConstant.ACTUAL_REFUND_TOPIC,
                JSONObject.toJSONString(refundMessage),
                -1,
                "取消订单发送实际退款消息",
                AfterSaleStateMachineChangeEnum.CANCEL_ORDER.getTags(),
                cancelOrderAssembleRequest.getOrderId()
        );
    }

    @Override
    @ConsistencyTask(id = "sendAfterSaleRefundMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendAfterSaleRefundMessage(ManualAfterSaleDTO manualAfterSaleDTO) {
        // 组装发送消息数据
        CustomerReceiveAfterSaleRequest customerReceiveAfterSaleRequest
                = orderConverter.convertReturnGoodsAssembleRequest(manualAfterSaleDTO);
        defaultProducer.sendMessage(
                RocketMqConstant.AFTER_SALE_CUSTOMER_AUDIT_TOPIC,
                JSONObject.toJSONString(customerReceiveAfterSaleRequest),
                -1,
                "售后发送实际退款消息",
                AfterSaleStateMachineChangeEnum.INITIATE_AFTER_SALE.getTags(),
                manualAfterSaleDTO.getOrderId()
        );

    }

    @Override
    @ConsistencyTask(id = "sendAuditPassMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendAuditPassMessage(AuditPassReleaseAssetsRequest auditPassReleaseAssetsRequest) {
        defaultProducer.sendMessage(
                RocketMqConstant.CUSTOMER_AUDIT_PASS_RELEASE_ASSETS_TOPIC,
                JSONObject.toJSONString(auditPassReleaseAssetsRequest),
                -1,
                "客服审核释放权益资产",
                AfterSaleStateMachineChangeEnum.AUDIT_PASS.getTags(),
                auditPassReleaseAssetsRequest.getReleaseProductStockDTO().getOrderId()
        );
    }

    @Override
    @ConsistencyTask(id = "sendAfterSaleReleaseCouponMessage", alertActionBeanName = "tendConsistencyAlerter")
    public void sendAfterSaleReleaseCouponMessage(ReleaseUserCouponRequest releaseUserCouponRequest) {
        defaultProducer.sendMessage(
                RocketMqConstant.AFTER_SALE_RELEASE_PROPERTY_TOPIC,
                JSONObject.toJSONString(releaseUserCouponRequest),
                -1,
                "售后释放优惠券消息",
                AfterSaleStateMachineChangeEnum.REFUNDING.getTags(),
                releaseUserCouponRequest.getOrderId()
        );
    }
}
