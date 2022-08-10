package moe.ahao.commerce.aftersale.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.common.event.ActualRefundEvent;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.AfterSaleStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AfterSaleActualRefundAppService {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    /**
     * 执行退款
     */
    public boolean refundMoney(ActualRefundEvent event) {
        // 售后状态机 操作 实际退款中通过更新售后信息 RefundingAction
        AfterSaleStateMachine afterSaleStateMachine = stateMachineFactory.getAfterSaleStateMachine(AfterSaleStatusEnum.REVIEW_PASS);
        afterSaleStateMachine.fire(AfterSaleStatusChangeEnum.AFTER_SALE_REFUNDING, event);
        return true;
    }
}
