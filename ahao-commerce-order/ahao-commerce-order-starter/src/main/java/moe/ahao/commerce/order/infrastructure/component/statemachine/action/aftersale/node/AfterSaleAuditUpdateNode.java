package moe.ahao.commerce.order.infrastructure.component.statemachine.action.aftersale.node;

import moe.ahao.commerce.aftersale.api.command.AfterSaleAuditCommand;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.AfterSaleLogDAO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.CustomerAuditResult;
import moe.ahao.commerce.common.enums.CustomerAuditSourceEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.StandardProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;

/**
 * 售后审核 更新售后信息节点 节点
 */
@Component
public class AfterSaleAuditUpdateNode extends StandardProcessor {
    @Autowired
    private AfterSaleLogDAO afterSaleLogDAO;
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    protected void processInternal(ProcessContext processContext) {
        // @Transactional无法生效，需要用编程式事务
        transactionTemplate.execute(transactionStatus -> {
            // 1. 处理审核结果
            AfterSaleAuditCommand command = processContext.get("command");
            Integer auditResult = command.getAuditResult();
            CustomerAuditResult customerAuditResult;
            AfterSaleStatusChangeEnum statusChangeEnum;
            if (CustomerAuditResult.REJECT.getCode().equals(auditResult)) {
                customerAuditResult = CustomerAuditResult.REJECT;
                statusChangeEnum = AfterSaleStatusChangeEnum.AFTER_SALE_REVIEWED_REJECTION;
            } else if (CustomerAuditResult.ACCEPT.getCode().equals(auditResult)) {
                customerAuditResult = CustomerAuditResult.ACCEPT;
                statusChangeEnum = AfterSaleStatusChangeEnum.AFTER_SALE_REVIEWED_PASS;
            } else {
                throw OrderExceptionEnum.CUSTOMER_AUDIT_RESULT_IS_NULL.msg();
            }

            // 2. 更新售后信息
            AfterSaleInfoDO afterSaleInfoDO = afterSaleInfoMapper.selectOneByAfterSaleId(command.getAfterSaleId());
            afterSaleInfoDO.setAfterSaleStatus(statusChangeEnum.getToStatus().getCode());
            afterSaleInfoDO.setReviewReason(customerAuditResult.getName());
            afterSaleInfoDO.setReviewReasonCode(customerAuditResult.getCode());
            afterSaleInfoDO.setReviewSource(CustomerAuditSourceEnum.SELF_MALL.getCode());
            afterSaleInfoDO.setReviewTime(new Date());
            afterSaleInfoMapper.updateReviewInfoByAfterSaleId(afterSaleInfoDO.getAfterSaleId(), afterSaleInfoDO.getAfterSaleStatus(), afterSaleInfoDO.getReviewReason(), afterSaleInfoDO.getReviewReasonCode(), afterSaleInfoDO.getReviewSource(), afterSaleInfoDO.getReviewTime());

            // 3. 记录售后日志
            afterSaleLogDAO.save(afterSaleInfoDO, statusChangeEnum);
            return true;
        });
    }
}
