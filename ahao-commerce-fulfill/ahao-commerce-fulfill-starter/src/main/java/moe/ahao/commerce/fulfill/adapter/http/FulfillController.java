package moe.ahao.commerce.fulfill.adapter.http;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.fulfill.api.FulfillFeignApi;
import moe.ahao.commerce.fulfill.api.command.CancelFulfillCommand;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.api.dto.OrderFulfillDTO;
import moe.ahao.commerce.fulfill.api.event.TriggerOrderAfterFulfillEvent;
import moe.ahao.commerce.fulfill.application.CancelFulfillAppService;
import moe.ahao.commerce.fulfill.application.ReceiveFulfillAppService;
import moe.ahao.commerce.fulfill.application.TriggerOrderWmsShipAppService;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(FulfillFeignApi.PATH)
public class FulfillController implements FulfillFeignApi {
    @Autowired
    private ReceiveFulfillAppService receiveFulfillAppService;
    @Autowired
    private CancelFulfillAppService cancelFulfillAppService;
    @Autowired
    private TriggerOrderWmsShipAppService triggerOrderWmsShipAppService;

    @Override
    public Result<Boolean> receiveOrderFulFill(@RequestBody ReceiveFulfillCommand command) {
        Boolean result = receiveFulfillAppService.fulfill(command);
        return Result.success(result);
    }

    @Override
    public Result<Boolean> cancelFulfill(@RequestBody CancelFulfillCommand command) {
        cancelFulfillAppService.cancelFulfillAndWmsAndTms(command);
        return Result.success(true);
    }


    @Override
    public Result<Boolean> triggerOrderWmsShipEvent(@RequestBody TriggerOrderAfterFulfillEvent event) {
        triggerOrderWmsShipAppService.trigger(event);
        return Result.success(true);
    }

    @Override
    public Result<List<OrderFulfillDTO>> listOrderFulfills(String orderId) {
        return null;
    }
}
