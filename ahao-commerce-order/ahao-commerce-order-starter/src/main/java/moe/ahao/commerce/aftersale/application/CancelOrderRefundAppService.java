package moe.ahao.commerce.aftersale.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.AfterSaleStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CancelOrderRefundAppService {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    /**
     * 取消订单/超时未支付取消 执行 退款前计算金额、记录售后信息等准备工作
     */
    public boolean handler(String orderId) {
        // 售后状态机 操作 取消订单时记录售后信息 CancelOrderCreatedInfoAction
        AfterSaleStateMachine afterSaleStateMachine = stateMachineFactory.getAfterSaleStateMachine(AfterSaleStatusEnum.UN_CREATED);
        afterSaleStateMachine.fire(AfterSaleStatusChangeEnum.CANCEL_AFTER_SALE_CREATED, orderId);
        return true;
    }
}
