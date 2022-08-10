package moe.ahao.commerce.customer.infrastructure.gateway;

import moe.ahao.commerce.aftersale.api.command.AfterSaleAuditCommand;
import moe.ahao.commerce.customer.api.command.CustomerReviewReturnGoodsCommand;
import moe.ahao.commerce.customer.infrastructure.exception.CustomerExceptionEnum;
import moe.ahao.commerce.customer.infrastructure.gateway.feign.AfterSaleFeignClient;
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
     * 接收客服的审核结果
     */
    public Boolean receiveCustomerAuditResult(CustomerReviewReturnGoodsCommand command) {
        AfterSaleAuditCommand afterSaleAuditCommand = new AfterSaleAuditCommand();
        afterSaleAuditCommand.setAfterSaleId(command.getAfterSaleId());
        afterSaleAuditCommand.setCustomerId(command.getCustomerId());
        afterSaleAuditCommand.setAuditResult(command.getAuditResult());
        afterSaleAuditCommand.setAuditResultDesc(command.getAuditResultDesc());

        Result<Boolean> result = afterSaleFeignClient.receiveCustomerAuditResult(afterSaleAuditCommand);
        if (result.getCode() != Result.SUCCESS) {
            throw CustomerExceptionEnum.PROCESS_RECEIVE_AFTER_SALE.msg();
        }
        return result.getObj();
    }
}
