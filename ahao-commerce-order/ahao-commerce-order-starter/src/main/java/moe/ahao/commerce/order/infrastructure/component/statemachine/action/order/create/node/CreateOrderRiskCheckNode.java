package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.create.node;

import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.infrastructure.gateway.RiskGateway;
import moe.ahao.commerce.risk.api.command.CheckOrderRiskCommand;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.StandardProcessor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 创建订单风控检查
 */
@Component
public class CreateOrderRiskCheckNode extends StandardProcessor {

    @Resource
    private RiskGateway riskGateway;

    @Override
    protected void processInternal(ProcessContext processContext) {
        CreateOrderCommand createOrderCommand = processContext.get("createOrderCommand");

        // 调用风控服务进行风控检查
        CheckOrderRiskCommand command = new CheckOrderRiskCommand();
        command.setBusinessIdentifier(createOrderCommand.getBusinessIdentifier());
        command.setOrderId(createOrderCommand.getOrderId());
        command.setUserId(createOrderCommand.getUserId());
        command.setSellerId(createOrderCommand.getSellerId());
        command.setClientIp(createOrderCommand.getClientIp());
        command.setDeviceId(createOrderCommand.getDeviceId());

        riskGateway.checkOrderRisk(command);
    }
}
