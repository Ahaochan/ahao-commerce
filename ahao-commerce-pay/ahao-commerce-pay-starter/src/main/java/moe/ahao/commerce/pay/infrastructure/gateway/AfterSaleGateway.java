package moe.ahao.commerce.pay.infrastructure.gateway;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import moe.ahao.commerce.aftersale.api.command.RefundOrderCallbackCommand;
import moe.ahao.commerce.pay.infrastructure.exception.PayException;
import moe.ahao.commerce.pay.infrastructure.gateway.fallback.AfterSaleGatewayFallback;
import moe.ahao.commerce.pay.infrastructure.gateway.feign.AfterSaleFeignClient;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 订单售后远程接口
 */
@Component
public class AfterSaleGateway {

    @Autowired
    private AfterSaleFeignClient afterSaleFeignClient;

    /**
     * 取消订单支付退款回调
     */
    @SentinelResource(value = "AfterSaleGateway:refundOrderCallback", fallbackClass = AfterSaleGatewayFallback.class, fallback = "refundCallbackFallback")
    public Boolean refundOrderCallback(RefundOrderCallbackCommand command) {
        Result<Boolean> result = afterSaleFeignClient.refundCallback(command);
        if (result.getCode() != Result.SUCCESS) {
            throw new PayException(result.getCode(), result.getMsg());
        }
        return result.getObj();
    }
}
