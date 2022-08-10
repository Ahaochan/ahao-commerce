package moe.ahao.commerce.order.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.RefundOrderCallbackCommand;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.AfterSaleStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RefundCallbackAppService {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    public void refundCallback(RefundOrderCallbackCommand command) {
        String orderId = command.getOrderId();
        log.info("接收到支付退款回调, orderId:{}", orderId);

        //  售后状态机 操作 支付回调退款成功 更新售后信息 RefundPayCallbackAction
        AfterSaleStateMachine afterSaleStateMachine = stateMachineFactory.getAfterSaleStateMachine(AfterSaleStatusEnum.REFUNDING);
        afterSaleStateMachine.fire(AfterSaleStatusChangeEnum.AFTER_SALE_REFUNDED, command);
    }
}
