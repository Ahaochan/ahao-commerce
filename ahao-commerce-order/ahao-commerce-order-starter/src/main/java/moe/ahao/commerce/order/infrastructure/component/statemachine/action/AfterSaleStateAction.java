package moe.ahao.commerce.order.infrastructure.component.statemachine.action;

import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.process.engine.wrapper.model.ProcessContextFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 售后状态action
 */
public abstract class AfterSaleStateAction<T> extends AbstractStateAction<T, String, AfterSaleStatusChangeEnum> {
    @Autowired
    protected ProcessContextFactory processContextFactory;
}
