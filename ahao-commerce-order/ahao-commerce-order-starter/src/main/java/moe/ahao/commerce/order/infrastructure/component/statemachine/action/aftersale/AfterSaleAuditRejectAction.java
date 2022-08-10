package moe.ahao.commerce.order.infrastructure.component.statemachine.action.aftersale;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.AfterSaleAuditCommand;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.AfterSaleStateAction;
import moe.ahao.process.engine.core.process.ProcessContext;
import org.springframework.stereotype.Component;

/**
 * 售后客服审核Action
 */
@Slf4j
@Component
public class AfterSaleAuditRejectAction extends AfterSaleStateAction<AfterSaleAuditCommand> {

    @Override
    public AfterSaleStatusChangeEnum event() {
        return AfterSaleStatusChangeEnum.AFTER_SALE_REVIEWED_REJECTION;
    }

    @Override
    protected String onStateChangeInternal(AfterSaleStatusChangeEnum event, AfterSaleAuditCommand command) {
        ProcessContext afterSaleProcess = processContextFactory.getContext("afterSaleRejectProcess");
        afterSaleProcess.set("command", command);
        afterSaleProcess.set("event", event);
        afterSaleProcess.start();
        return command.getAfterSaleId();
    }
}
