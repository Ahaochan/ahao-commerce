package moe.ahao.commerce.order.infrastructure.component.statemachine.action.aftersale.node;

import moe.ahao.commerce.aftersale.api.command.AfterSaleAuditCommand;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleItemMapper;
import moe.ahao.commerce.common.enums.AfterSaleItemTypeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.StandardProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 售后审核 检查请求参数 节点
 */
@Component
public class AfterSaleAuditCheckNode extends StandardProcessor {
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;

    @Override
    protected void processInternal(ProcessContext processContext) {
        AfterSaleAuditCommand command = processContext.get("command");

        AfterSaleInfoDO afterSaleInfo = afterSaleInfoMapper.selectOneByAfterSaleId(command.getAfterSaleId());

        // 查询 售后订单条目
        if(afterSaleInfo == null) {
            throw OrderExceptionEnum.AFTER_SALE_FAILED.msg();
        }
        if(afterSaleInfo.getAfterSaleStatus() == null || afterSaleInfo.getAfterSaleStatus() > AfterSaleStatusEnum.COMMITTED.getCode()) {
            throw OrderExceptionEnum.AFTER_SALE_ORDER_STATUS_ERROR.msg();
        }
        processContext.set("afterSaleInfo", afterSaleInfo);
    }
}
