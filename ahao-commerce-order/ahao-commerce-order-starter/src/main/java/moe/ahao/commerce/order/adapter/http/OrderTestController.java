package moe.ahao.commerce.order.adapter.http;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RocketMqConstant;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.infrastructure.event.PaidOrderSuccessEvent;
import moe.ahao.commerce.fulfill.api.FulfillFeignApi;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.api.event.OrderDeliveredEvent;
import moe.ahao.commerce.fulfill.api.event.OrderOutStockEvent;
import moe.ahao.commerce.fulfill.api.event.OrderSignedEvent;
import moe.ahao.commerce.fulfill.api.event.TriggerOrderAfterFulfillEvent;
import moe.ahao.commerce.order.application.OrderFulFillService;
import moe.ahao.commerce.order.infrastructure.gateway.FulfillGateway;
import moe.ahao.commerce.order.infrastructure.publisher.DefaultProducer;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.domain.entity.Result;
import moe.ahao.util.commons.io.JSONHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 正向下单流程接口冒烟测试
 */
@Slf4j
@RestController
@RequestMapping("/api/order/test")
public class OrderTestController {
    @Autowired
    private FulfillFeignApi fulfillApi;
    @Autowired
    private DefaultProducer defaultProducer;
    @Autowired
    private OrderFulFillService orderFulFillService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private FulfillGateway fulfillGateway;

    /**
     * 支付回调时触发订单已支付事件
     */
    @GetMapping("/triggerPaidEvent")
    public Result<Boolean> triggerOrderPaidEvent(@RequestParam("orderId") String orderId) {
        PaidOrderSuccessEvent event = new PaidOrderSuccessEvent(orderId);
        String json = JSONHelper.toString(event);
        defaultProducer.sendMessage(RocketMqConstant.PAID_ORDER_SUCCESS_TOPIC, json, -1, null, orderId);
        return Result.success(true);
    }

    /**
     * 支付回调后触发接收订单履约
     */
    @GetMapping("/triggerReceiveOrderFulFill")
    public Result<Boolean> triggerReceiveOrderFulFill(@RequestParam("orderId") String orderId, @RequestParam("fulfillException") String fulfillException, @RequestParam("wmsException") String wmsException, @RequestParam("tmsException") String tmsException) {
        OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
        ReceiveFulfillCommand request = orderFulFillService.buildReceiveFulFillRequest(orderInfo);
        request.setFulfillException(fulfillException);
        request.setWmsException(wmsException);
        request.setTmsException(tmsException);
        fulfillGateway.receiveOrderFulFill(request);
        return Result.success(true);
    }


    /**
     * 履约时触发订单发货出库事件
     */
    @PostMapping("/triggerOutStockEvent")
    public Result<Boolean> triggerOrderOutStockWmsEvent(@RequestParam("orderId") String orderId, @RequestParam("fulfillId") String fulfillId, @RequestBody OrderOutStockEvent event) {
        TriggerOrderAfterFulfillEvent request = new TriggerOrderAfterFulfillEvent(orderId, fulfillId, OrderStatusChangeEnum.ORDER_OUT_STOCKED, event);
        Result<Boolean> result = fulfillApi.triggerOrderWmsShipEvent(request);
        return result;
    }

    /**
     * 履约配送时触发订单配送事件
     */
    @PostMapping("/triggerDeliveredWmsEvent")
    public Result<Boolean> triggerOrderDeliveredWmsEvent(@RequestParam("orderId") String orderId, @RequestParam("fulfillId") String fulfillId, @RequestBody OrderDeliveredEvent event) {
        TriggerOrderAfterFulfillEvent request = new TriggerOrderAfterFulfillEvent(orderId, fulfillId, OrderStatusChangeEnum.ORDER_DELIVERED, event);
        Result<Boolean> result = fulfillApi.triggerOrderWmsShipEvent(request);
        return result;
    }

    /**
     * 用户签收时触发订单签收事件
     */
    @PostMapping("/triggerSignedWmsEvent")
    public Result<Boolean> triggerOrderSignedWmsEvent(@RequestParam("orderId") String orderId, @RequestParam("fulfillId") String fulfillId, @RequestBody OrderSignedEvent event) {
        TriggerOrderAfterFulfillEvent request = new TriggerOrderAfterFulfillEvent(orderId, fulfillId, OrderStatusChangeEnum.ORDER_SIGNED, event);
        Result<Boolean> result = fulfillApi.triggerOrderWmsShipEvent(request);
        return result;
    }
}
