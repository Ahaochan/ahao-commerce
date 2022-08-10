package moe.ahao.commerce.order.infrastructure.gateway;

import moe.ahao.commerce.fulfill.api.command.CancelFulfillCommand;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.feign.FulfillFeignClient;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 履约服务远程接口
 */
@Component
public class FulfillGateway {
    @Autowired
    private FulfillFeignClient fulfillFeignClient;

    /**
     * 取消订单履约
     */
    public void cancelFulfill(CancelFulfillCommand command) {
        Result<Boolean> result = fulfillFeignClient.cancelFulfill(command);
        if (result.getCode() != Result.SUCCESS) {
            throw OrderExceptionEnum.CANCEL_ORDER_FULFILL_ERROR.msg();
        }
    }

    /**
     * 接收订单履约
     */
    public void receiveOrderFulFill(ReceiveFulfillCommand command) {
        Result<Boolean> result = fulfillFeignClient.receiveOrderFulFill(command);
        if (result.getCode() != Result.SUCCESS) {
            throw OrderExceptionEnum.ORDER_FULFILL_ERROR.msg();
        }
    }
}
