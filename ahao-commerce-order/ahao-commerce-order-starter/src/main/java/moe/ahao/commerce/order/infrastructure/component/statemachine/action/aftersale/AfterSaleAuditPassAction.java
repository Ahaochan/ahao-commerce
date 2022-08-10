package moe.ahao.commerce.order.infrastructure.component.statemachine.action.aftersale;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.AfterSaleAuditCommand;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.AfterSaleStateAction;
import moe.ahao.process.engine.core.process.ProcessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 售后客服审核Action
 */
@Slf4j
@Component
public class AfterSaleAuditPassAction extends AfterSaleStateAction<AfterSaleAuditCommand> {
    @Override
    public AfterSaleStatusChangeEnum event() {
        return AfterSaleStatusChangeEnum.AFTER_SALE_REVIEWED_PASS;
    }

    @Override
    protected String onStateChangeInternal(AfterSaleStatusChangeEnum event, AfterSaleAuditCommand command) {
        // 客服审核通过
        ProcessContext afterSaleProcess = processContextFactory.getContext("afterSaleAuditProcess");
        afterSaleProcess.set("command", command);
        afterSaleProcess.set("event", event);
        afterSaleProcess.start();
        return command.getAfterSaleId();
    }
}
