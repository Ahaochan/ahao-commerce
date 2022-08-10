package moe.ahao.commerce.order.infrastructure.gateway;

import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.feign.PayFeignClient;
import moe.ahao.commerce.pay.api.command.PayOrderCommand;
import moe.ahao.commerce.pay.api.command.RefundOrderCommand;
import moe.ahao.commerce.pay.api.dto.PayOrderDTO;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 支付服务远程接口
 */
@Component
public class PayGateway {
    /**
     * 支付服务
     */
    @Autowired
    private PayFeignClient payFeignClient;

    /**
     * 调用支付系统进行预支付下单
     */
    public PayOrderDTO payOrder(PayOrderCommand command) {
        Result<PayOrderDTO> result = payFeignClient.payOrder(command);
        if (result.getCode() != Result.SUCCESS) {
            throw OrderExceptionEnum.ORDER_PRE_PAY_ERROR.msg();
        }
        return result.getObj();
    }

    /**
     * 调用支付系统执行退款
     */
    public void executeRefund(RefundOrderCommand command) {
        Result<Boolean> result = payFeignClient.refundOrder(command);
        if (result.getCode() != Result.SUCCESS) {
            throw OrderExceptionEnum.ORDER_REFUND_AMOUNT_FAILED.msg();
        }
    }
}
