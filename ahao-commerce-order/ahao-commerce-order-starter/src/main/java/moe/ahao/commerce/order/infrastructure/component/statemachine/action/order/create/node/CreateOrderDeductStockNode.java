package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.create.node;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.inventory.api.command.DeductProductStockCommand;
import moe.ahao.commerce.inventory.api.command.ReleaseProductStockCommand;
import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.infrastructure.gateway.InventoryGateway;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.RollbackProcessor;
import moe.ahao.tend.consistency.core.annotation.ConsistencyTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CreateOrderDeductStockNode extends RollbackProcessor {

    @Autowired
    private InventoryGateway inventoryGateway;

    @Override
    protected void processInternal(ProcessContext processContext) {
        // 扣减库存
        CreateOrderCommand command = processContext.get("createOrderCommand");
        this.deductProductStock(command);
    }

    private void deductProductStock(CreateOrderCommand createOrderCommand) {
        DeductProductStockCommand command = new DeductProductStockCommand();
        command.setBusinessIdentifier(createOrderCommand.getBusinessIdentifier());
        command.setOrderId(createOrderCommand.getOrderId());
        command.setUserId(createOrderCommand.getUserId());
        command.setSellerId(createOrderCommand.getSellerId());

        List<DeductProductStockCommand.OrderItem> orderItemCommandList = new ArrayList<>();
        for (CreateOrderCommand.OrderItem orderItem : createOrderCommand.getOrderItems()) {
            DeductProductStockCommand.OrderItem orderItemCommand = new DeductProductStockCommand.OrderItem();
            orderItemCommand.setSkuCode(orderItem.getSkuCode());
            orderItemCommand.setSaleQuantity(orderItem.getSaleQuantity());

            orderItemCommandList.add(orderItemCommand);
        }
        command.setOrderItems(orderItemCommandList);

        inventoryGateway.deductProductStock(command);
    }

    @Override
    protected void rollback(ProcessContext processContext) {
        CreateOrderCommand createOrderCommand = processContext.get("createOrderCommand");

        ReleaseProductStockCommand command = new ReleaseProductStockCommand();
        command.setOrderId(createOrderCommand.getOrderId());

        List<ReleaseProductStockCommand.OrderItem> orderItemCommandList = new ArrayList<>();
        for (CreateOrderCommand.OrderItem orderItem : createOrderCommand.getOrderItems()) {
            ReleaseProductStockCommand.OrderItem orderItemCommand = new ReleaseProductStockCommand.OrderItem();
            orderItemCommand.setSkuCode(orderItem.getSkuCode());
            orderItemCommand.setSaleQuantity(orderItem.getSaleQuantity());

            orderItemCommandList.add(orderItemCommand);
        }
        command.setOrderItems(orderItemCommandList);

        this.doRollback(command);
    }

    /**
     * 一致性框架只能拦截public方法
     */
    @ConsistencyTask(id = "rollbackDeductStock", alertActionBeanName = "tendConsistencyAlerter")
    public void doRollback(ReleaseProductStockCommand command) {
        inventoryGateway.releaseProductStock(command);
    }
}
