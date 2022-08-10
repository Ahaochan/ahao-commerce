package moe.ahao.commerce.pay.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.RefundOrderCallbackCommand;
import moe.ahao.commerce.pay.api.command.RefundCallbackCommand;
import moe.ahao.commerce.pay.infrastructure.gateway.AfterSaleGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RefundOrderCallbackAppService {
    @Autowired
    private AfterSaleGateway afterSaleGateway;

    public Boolean callback(RefundCallbackCommand command) {
        RefundOrderCallbackCommand refundOrderCallbackCommand = new RefundOrderCallbackCommand();
        refundOrderCallbackCommand.setOrderId(command.getOrderId());
        refundOrderCallbackCommand.setBatchNo(command.getOrderId());
        refundOrderCallbackCommand.setRefundStatus(command.getRefundStatus());
        refundOrderCallbackCommand.setRefundFee(command.getRefundFee());
        refundOrderCallbackCommand.setTotalFee(command.getTotalFee());
        refundOrderCallbackCommand.setSign(command.getSign());
        refundOrderCallbackCommand.setTradeNo(command.getTradeNo());
        refundOrderCallbackCommand.setRefundTime(command.getRefundTime());

        Boolean success = afterSaleGateway.refundOrderCallback(refundOrderCallbackCommand);
        return success;
    }
}
