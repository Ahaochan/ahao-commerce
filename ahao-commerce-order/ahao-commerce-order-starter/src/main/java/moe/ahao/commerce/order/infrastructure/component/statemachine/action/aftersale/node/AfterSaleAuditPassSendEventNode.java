package moe.ahao.commerce.order.infrastructure.component.statemachine.action.aftersale.node;

import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.common.enums.AfterSaleTypeEnum;
import moe.ahao.commerce.order.api.command.AfterSaleAuditPassReleaseAssetsEvent;
import moe.ahao.commerce.order.infrastructure.publisher.OrderEventPublisher;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.StandardProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 售后审核 客服审核通过后释放权益资产消息(释放库存和实际退款) 节点
 */
@Component
public class AfterSaleAuditPassSendEventNode extends StandardProcessor {
    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Override
    protected void processInternal(ProcessContext processContext) {
        AfterSaleInfoDO afterSaleInfo = processContext.get("afterSaleInfo");
        AfterSaleAuditPassReleaseAssetsEvent event = new AfterSaleAuditPassReleaseAssetsEvent();
        event.setOrderId(afterSaleInfo.getOrderId());
        event.setAfterSaleId(afterSaleInfo.getAfterSaleId());
        orderEventPublisher.sendAuditPassMessage(event);
    }
}
