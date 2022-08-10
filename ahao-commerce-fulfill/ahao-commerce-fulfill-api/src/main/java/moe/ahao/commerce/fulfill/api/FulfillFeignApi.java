package moe.ahao.commerce.fulfill.api;

import moe.ahao.commerce.fulfill.api.command.CancelFulfillCommand;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.api.dto.OrderFulfillDTO;
import moe.ahao.commerce.fulfill.api.event.TriggerOrderAfterFulfillEvent;
import moe.ahao.domain.entity.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 履约系统业务接口
 */
public interface FulfillFeignApi {
    String PATH = "/api/fulfill/";

    /**
     * 接收订单履约
     */
    @PostMapping("/receiveOrderFulFill")
    Result<Boolean> receiveOrderFulFill(@RequestBody ReceiveFulfillCommand command);

    /**
     * 履约通知停止配送
     */
    @PostMapping("/cancelFulfill")
    Result<Boolean> cancelFulfill(@RequestBody CancelFulfillCommand command);

    /**
     * 触发订单物流配送结果事件接口
     * 一个工具类接口，用于模拟触发"订单已出库事件"，"订单已配送事件"，"订单已签收事件"
     */
    @PostMapping("/triggerOrderWmsShipEvent")
    Result<Boolean> triggerOrderWmsShipEvent(@RequestBody TriggerOrderAfterFulfillEvent event);

    /**
     * 查询履约单
     */
    @PostMapping("/listOrderFulfills")
    Result<List<OrderFulfillDTO>> listOrderFulfills(@RequestParam String orderId);
}
