package com.ruyuan.eshop.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruyuan.eshop.common.constants.RocketMqConstant;
import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.common.message.PaidOrderSuccessMessage;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.fulfill.api.FulfillApi;
import com.ruyuan.eshop.fulfill.domain.event.OrderSignedEvent;
import com.ruyuan.eshop.fulfill.domain.request.ReceiveFulfillRequest;
import com.ruyuan.eshop.fulfill.domain.request.TriggerOrderAfterFulfillEventRequest;
import com.ruyuan.eshop.fulfill.dto.OrderFulfillDTO;
import com.ruyuan.eshop.order.api.OrderApi;
import com.ruyuan.eshop.order.api.OrderQueryApi;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.domain.dto.*;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import com.ruyuan.eshop.order.domain.request.*;
import com.ruyuan.eshop.order.mq.producer.DefaultProducer;
import com.ruyuan.eshop.order.service.impl.OrderFulFillServiceImpl;
import com.ruyuan.eshop.order.test.PrepareOrderFullDataUtilsTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 正向下单流程接口冒烟测试
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@RestController
@Slf4j
@RequestMapping("/order/test")
public class OrderTestController {

    /**
     * 订单服务
     */
    @DubboReference(version = "1.0.0", retries = 0)
    private OrderApi orderApi;

    @DubboReference(version = "1.0.0")
    private OrderQueryApi queryApi;

    @DubboReference(version = "1.0.0", retries = 0)
    private FulfillApi fulfillApi;

    @Autowired
    private DefaultProducer defaultProducer;

    @Autowired
    private OrderFulFillServiceImpl orderFulFillService;

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private PrepareOrderFullDataUtilsTest prepareOrderFullDataUtilsTest;

    @Value("${canal.binlog.consumer.enable}")
    private Boolean enable;

    /**
     *
     */
    @GetMapping("/canal/isEnable")
    public JsonResult<Boolean> isEnable() {
        return JsonResult.buildSuccess(enable);
    }

    /**
     * 测试生成新的订单号
     */
    @SentinelResource("OrderTestController:genOrderId")
    @PostMapping("/genOrderId")
    public JsonResult<GenOrderIdDTO> genOrderId(@RequestBody GenOrderIdRequest genOrderIdRequest) {
        return orderApi.genOrderId(genOrderIdRequest);
    }

    /**
     * 测试提交订单
     */
    @SentinelResource("OrderTestController:createOrder")
    @PostMapping("/createOrder")
    public JsonResult<CreateOrderDTO> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        return orderApi.createOrder(createOrderRequest);
    }

    /**
     * 测试预支付订单
     */
    @SentinelResource("OrderTestController:prePayOrder")
    @PostMapping("/prePayOrder")
    public JsonResult<PrePayOrderDTO> prePayOrder(@RequestBody PrePayOrderRequest prePayOrderRequest) {
        return orderApi.prePayOrder(prePayOrderRequest);
    }

    /**
     * 测试支付回调
     */
    @SentinelResource("OrderTestController:payCallback")
    @PostMapping("/payCallback")
    public JsonResult<Boolean> payCallback(@RequestBody PayCallbackRequest payCallbackRequest) {
        return orderApi.payCallback(payCallbackRequest);
    }

    /**
     * 移除订单
     */
    @PostMapping("/removeOrders")
    public JsonResult<RemoveOrderDTO> removeOrders(@RequestBody RemoveOrderRequest request) {
        return orderApi.removeOrders(request);
    }

    /**
     * 调整订单配置地址
     */
    @PostMapping("/adjustDeliveryAddress")
    public JsonResult<AdjustDeliveryAddressDTO> adjustDeliveryAddress(@RequestBody AdjustDeliveryAddressRequest request) {
        return orderApi.adjustDeliveryAddress(request);
    }

    /**
     * 订单列表查询 v1
     */
    @PostMapping("/v1/listOrders")
    public JsonResult<PagingInfo<OrderListDTO>> listOrdersV1(@RequestBody OrderQuery query) {
        return queryApi.listOrdersV1(query);
    }

    /**
     * 订单列表查询 v2
     */
    @PostMapping("/v2/listOrders")
    public JsonResult<PagingInfo<OrderDetailDTO>> listOrdersV2(@RequestBody OrderQuery query) {
        return queryApi.listOrdersV2(query);
    }
    /**
     * 订单详情 v1
     */
    @GetMapping("/v1/orderDetail")
    public JsonResult<OrderDetailDTO> orderDetailV1(String orderId) {
        return queryApi.orderDetailV1(orderId);
    }

    /**
     * 订单详情 v2
     */
    @PostMapping("/v2/orderDetail")
    public JsonResult<OrderDetailDTO> orderDetailV2(@RequestBody OrderDetailRequest request) {
        return queryApi.orderDetailV2(request);
    }

    /**
     * 触发订单签收事件
     */
    @PostMapping("/triggerSignedWmsEvent")
    public JsonResult<Boolean> triggerOrderSignedWmsEvent(@RequestBody OrderSignedEvent event) {
        String orderId = event.getOrderId();
        String fulfillId = event.getFulfillId();
        log.info("orderId={},fulfillId={},event={}", orderId, fulfillId, JSONObject.toJSONString(event));

        TriggerOrderAfterFulfillEventRequest request = new TriggerOrderAfterFulfillEventRequest(orderId
                , fulfillId, OrderStatusChangeEnum.ORDER_SIGNED, event);

        return fulfillApi.triggerOrderWmsShipEvent(request);
    }


    /**
     * 触发订单已支付事件
     */
    @GetMapping("/triggerPaidEvent")
    public JsonResult<Boolean> triggerOrderPaidEvent(@RequestParam("orderId") String orderId) {
        log.info("orderId={}", orderId);
        PaidOrderSuccessMessage message = new PaidOrderSuccessMessage();
        message.setOrderId(orderId);
        String msgJson = JSON.toJSONString(message);
        defaultProducer.sendMessage(RocketMqConstant.PAID_ORDER_SUCCESS_TOPIC, msgJson, "订单已完成支付", null, orderId);
        return JsonResult.buildSuccess(true);
    }


    /**
     * 触发接收订单履约
     */
    @GetMapping("/triggerReceiveOrderFulFill")
    public JsonResult<Boolean> triggerReceiveOrderFulFill(@RequestParam("orderId") String orderId,
                                                          @RequestParam("fulfillException") String fulfillException,
                                                          @RequestParam("wmsException") String wmsException,
                                                          @RequestParam("tmsException") String tmsException) {
        log.info("orderId={}", orderId);

        OrderInfoDO order = orderInfoDAO.getByOrderId(orderId);
        ReceiveFulfillRequest request = orderFulFillService.buildReceiveFulFillRequest(order);
        request.setFulfillException(fulfillException);
        request.setWmsException(wmsException);
        request.setTmsException(tmsException);

        return fulfillApi.receiveOrderFulFill(request);
    }

    /**
     * 查询履约单
     */
    @GetMapping("/orderFulfill/list")
    public JsonResult<List<OrderFulfillDTO>> listOrderFulfills(@RequestParam("orderId") String orderId) {
        log.info("orderId={}", orderId);
        return  fulfillApi.listOrderFulfills(orderId);
    }

//    /**
//     * 批量生成订单数据
//     */
//    @GetMapping("/batchInsertOrder")
//    public JsonResult<Boolean> batchInsertOrder() {
//        prepareOrderFullDataUtilsTest.batchInsertOrder();
//        return JsonResult.buildSuccess(true);
//    }

    /**
     * 查询订单号,提供给压测订单详情使用
     */
    @GetMapping("/findOrderId")
    public JsonResult<String> findOrderId(@RequestParam("userId") String userId) {
        String orderId = queryApi.getOrderIdByUserId(userId);
        return JsonResult.buildSuccess(orderId);
    }
}