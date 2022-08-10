package moe.ahao.commerce.aftersale.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.AfterSaleAuditCommand;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.common.enums.CustomerAuditResult;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.AfterSaleStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AfterSaleAuditAppService {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    public void audit(AfterSaleAuditCommand command) {
        // 1. 客服审核拒绝
        Integer auditResult = command.getAuditResult();
        if (CustomerAuditResult.REJECT.getCode().equals(auditResult)) {
            // 1.1. 更新 审核拒绝 售后信息
            AfterSaleStateMachine afterSaleStateMachine = stateMachineFactory.getAfterSaleStateMachine(AfterSaleStatusEnum.COMMITTED);
            afterSaleStateMachine.fire(AfterSaleStatusChangeEnum.AFTER_SALE_REVIEWED_REJECTION, command);
        }
        // 2. 客服审核通过
        else if (CustomerAuditResult.ACCEPT.getCode().equals(auditResult)) {
            // 2.1. 更新 审核接受 售后信息, 发送释放权益资产事务MQ
            AfterSaleStateMachine afterSaleStateMachine = stateMachineFactory.getAfterSaleStateMachine(AfterSaleStatusEnum.COMMITTED);
            afterSaleStateMachine.fire(AfterSaleStatusChangeEnum.AFTER_SALE_REVIEWED_PASS, command);
        }
    }
}
