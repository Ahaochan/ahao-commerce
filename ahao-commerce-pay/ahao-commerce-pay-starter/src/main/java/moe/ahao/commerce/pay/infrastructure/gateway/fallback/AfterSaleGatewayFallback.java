package moe.ahao.commerce.pay.infrastructure.gateway.fallback;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.RefundOrderCallbackCommand;
import moe.ahao.commerce.pay.api.command.RefundCallbackCommand;
import moe.ahao.commerce.pay.api.dto.CheckCustomerReviewReturnGoodsRequestDTO;
import org.springframework.stereotype.Component;

/**
 * 订单售后远程服务降级处理组件
 */
@Slf4j
@Component
public class AfterSaleGatewayFallback {
    /**
     * 取消订单支付退款回调降级处理
     */
    public CheckCustomerReviewReturnGoodsRequestDTO refundCallbackFallback(RefundOrderCallbackCommand command, Throwable e) {
        log.error("取消订单支付退款回调触发降级了", e);
        CheckCustomerReviewReturnGoodsRequestDTO checkCustomerReviewReturnGoodsRequestDTO = new CheckCustomerReviewReturnGoodsRequestDTO();
        checkCustomerReviewReturnGoodsRequestDTO.setResult(false);
        return checkCustomerReviewReturnGoodsRequestDTO;
    }
}
