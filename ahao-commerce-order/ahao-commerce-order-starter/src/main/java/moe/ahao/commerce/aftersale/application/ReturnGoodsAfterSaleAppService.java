package moe.ahao.commerce.aftersale.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.CreateReturnGoodsAfterSaleCommand;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.AfterSaleStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReturnGoodsAfterSaleAppService {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    public void create(CreateReturnGoodsAfterSaleCommand command) {
        //  售后状态机 操作 售后数据落库 AfterSaleCreatedInfoAction
        AfterSaleStateMachine afterSaleStateMachine = stateMachineFactory.getAfterSaleStateMachine(AfterSaleStatusEnum.UN_CREATED);
        afterSaleStateMachine.fire(AfterSaleStatusChangeEnum.AFTER_SALE_CREATED, command);
    }
}
