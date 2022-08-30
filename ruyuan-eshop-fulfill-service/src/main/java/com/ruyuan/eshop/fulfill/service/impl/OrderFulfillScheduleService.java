package com.ruyuan.eshop.fulfill.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruyuan.eshop.common.bean.SpringApplicationContext;
import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.fulfill.converter.FulFillConverter;
import com.ruyuan.eshop.fulfill.dao.OrderFulfillDAO;
import com.ruyuan.eshop.fulfill.domain.entity.OrderFulfillDO;
import com.ruyuan.eshop.fulfill.domain.entity.OrderFulfillItemDO;
import com.ruyuan.eshop.fulfill.domain.event.OrderDeliveredEvent;
import com.ruyuan.eshop.fulfill.domain.event.OrderOutStockEvent;
import com.ruyuan.eshop.fulfill.domain.request.TriggerOrderAfterFulfillEventRequest;
import com.ruyuan.eshop.fulfill.remote.TmsRemote;
import com.ruyuan.eshop.fulfill.remote.WmsRemote;
import com.ruyuan.eshop.fulfill.service.OrderAfterFulfillEventProcessor;
import com.ruyuan.eshop.tms.domain.SendOutDTO;
import com.ruyuan.eshop.tms.domain.SendOutRequest;
import com.ruyuan.eshop.wms.domain.PickDTO;
import com.ruyuan.eshop.wms.domain.PickGoodsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 订单履约调度service
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class OrderFulfillScheduleService {

    @Autowired
    private OrderFulfillDAO orderFulfillDAO;

    @Autowired
    private FulFillConverter fulFillConverter;

    @Autowired
    private SpringApplicationContext springApplicationContext;

    @Autowired
    private TmsRemote tmsRemote;

    @Autowired
    private WmsRemote wmsRemote;

    @Autowired
    private WmsShipEventProcessorFactory wmsShipEventProcessorFactory;

    /**
     * 执行履约单调度
     */
    public void doSchedule(OrderFulfillDO orderFulfill, List<OrderFulfillItemDO> orderFulfillItems) {

        // 1、调用wms的接口进行捡货出库
        PickDTO pickResult = wmsRemote.pickGoods(buildPickGoodsRequest(orderFulfill, orderFulfillItems));
        log.info("捡货结果，result={}", JSONObject.toJSONString(pickResult));

        // 2、处理发送订单已出库消息
        processSendOrderOutStockEvent(orderFulfill);

        // 3、调用tms的接口进行发货
        SendOutDTO sendOutResult = tmsRemote.sendOut(buildSendOutRequest(orderFulfill, orderFulfillItems));
        log.info("发货结果，result={}", JSONObject.toJSONString(sendOutResult));

        // 4、存储物流单号
        String logisticsCode = sendOutResult.getLogisticsCode();
        orderFulfillDAO.saveLogisticsCode(orderFulfill.getFulfillId(), logisticsCode);

        // 5、处理发送订单已配送消息
        processSendOrderDeliveredEvent(orderFulfill, sendOutResult);
    }

    /**
     * 处理发送订单已出库消息
     */
    private void processSendOrderOutStockEvent(OrderFulfillDO orderFulfill) {
        OrderStatusChangeEnum statusChange = OrderStatusChangeEnum.ORDER_OUT_STOCKED;
        OrderAfterFulfillEventProcessor wmsShipEventProcessor = wmsShipEventProcessorFactory.getWmsShipEventProcessor(statusChange);
        TriggerOrderAfterFulfillEventRequest triggerEventRequest = new TriggerOrderAfterFulfillEventRequest();
        triggerEventRequest.setOrderId(orderFulfill.getOrderId());
        triggerEventRequest.setOrderStatusChange(statusChange);
        triggerEventRequest.setFulfillId(orderFulfill.getFulfillId());

        // 设置出库事件
        OrderOutStockEvent orderOutStockEvent = new OrderOutStockEvent();
        orderOutStockEvent.setOutStockTime(new Date());
        triggerEventRequest.setAfterFulfillEvent(orderOutStockEvent);
        wmsShipEventProcessor.execute(triggerEventRequest, orderFulfill);
    }

    /**
     * 处理发送订单已出库消息
     */
    private void processSendOrderDeliveredEvent(OrderFulfillDO orderFulfill, SendOutDTO sendOutDTO) {
        OrderStatusChangeEnum statusChange = OrderStatusChangeEnum.ORDER_DELIVERED;
        OrderAfterFulfillEventProcessor wmsShipEventProcessor = wmsShipEventProcessorFactory.getWmsShipEventProcessor(statusChange);
        TriggerOrderAfterFulfillEventRequest triggerEventRequest = new TriggerOrderAfterFulfillEventRequest();
        triggerEventRequest.setOrderId(orderFulfill.getOrderId());
        triggerEventRequest.setOrderStatusChange(statusChange);
        triggerEventRequest.setFulfillId(orderFulfill.getFulfillId());

        // 设置配送员信息
        OrderDeliveredEvent orderDeliveredEvent = new OrderDeliveredEvent();
        orderDeliveredEvent.setDelivererNo(sendOutDTO.getDelivererNo());
        orderDeliveredEvent.setDelivererName(sendOutDTO.getDelivererName());
        orderDeliveredEvent.setDelivererPhone(sendOutDTO.getDelivererPhone());
        triggerEventRequest.setAfterFulfillEvent(orderDeliveredEvent);

        wmsShipEventProcessor.execute(triggerEventRequest, orderFulfill);
    }

    /**
     * 构造捡货请求
     */
    private PickGoodsRequest buildPickGoodsRequest(OrderFulfillDO orderFulfill, List<OrderFulfillItemDO> orderFulfillItems) {
        PickGoodsRequest request = fulFillConverter.convertPickGoodsRequest(orderFulfill);
        List<PickGoodsRequest.OrderItemRequest> itemRequests = fulFillConverter.convertPickOrderItemRequests(orderFulfillItems);
        request.setOrderItems(itemRequests);
        return request;
    }

    /**
     * 构造发货请求
     */
    private SendOutRequest buildSendOutRequest(OrderFulfillDO orderFulfill, List<OrderFulfillItemDO> orderFulfillItems) {
        SendOutRequest request = fulFillConverter.convertSendOutRequest(orderFulfill);
        List<SendOutRequest.OrderItemRequest> itemRequests = fulFillConverter.convertSendOutOrderItemRequests(orderFulfillItems);
        request.setOrderItems(itemRequests);
        return request;
    }

}
