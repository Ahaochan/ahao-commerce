package moe.ahao.commerce.aftersale.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.RevokeAfterSaleCommand;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.AfterSaleStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RevokeAfterSaleAppService {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    public void revoke(RevokeAfterSaleCommand command) {
        //  售后状态机 操作 售后撤销 AfterSaleRevokeAction
        AfterSaleStateMachine afterSaleStateMachine = stateMachineFactory.getAfterSaleStateMachine(AfterSaleStatusEnum.COMMITTED);
        afterSaleStateMachine.fire(AfterSaleStatusChangeEnum.AFTER_SALE_REVOKED, command);
    }
}
