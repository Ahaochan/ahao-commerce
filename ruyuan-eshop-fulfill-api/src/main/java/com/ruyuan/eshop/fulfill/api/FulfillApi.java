package com.ruyuan.eshop.fulfill.api;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.fulfill.domain.request.CancelFulfillRequest;
import com.ruyuan.eshop.fulfill.domain.request.ReceiveFulfillRequest;
import com.ruyuan.eshop.fulfill.domain.request.TriggerOrderAfterFulfillEventRequest;
import com.ruyuan.eshop.fulfill.dto.OrderFulfillDTO;

import java.util.List;


/**
 * 履约系统业务接口
 *
 * @author zhonghuashishan
 * @version 1.0
 */
public interface FulfillApi {

    /**
     * 接收订单履约
     *
     * @param request 请求
     * @return 相应
     */
    JsonResult<Boolean> receiveOrderFulFill(ReceiveFulfillRequest request);

    /**
     * 触发订单物流配送结果事件接口
     * 一个工具类接口，用于模拟触发"订单已出库事件"，"订单已配送事件"，"订单已签收事件"
     *
     * @return 相应
     */
    JsonResult<Boolean> triggerOrderWmsShipEvent(TriggerOrderAfterFulfillEventRequest request);

    /**
     * 履约通知停止配送
     */
    JsonResult<Boolean> cancelFulfill(CancelFulfillRequest cancelFulfillRequest);

    /**
     * 查询履约单
     */
    JsonResult<List<OrderFulfillDTO>> listOrderFulfills(String orderId);

}