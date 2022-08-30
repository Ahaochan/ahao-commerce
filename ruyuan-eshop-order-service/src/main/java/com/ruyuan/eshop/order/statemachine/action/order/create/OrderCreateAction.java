package com.ruyuan.eshop.order.statemachine.action.order.create;

import com.ruyuan.consistency.annotation.ConsistencyTask;
import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.common.enums.OrderStatusEnum;
import com.ruyuan.eshop.order.builder.FullOrderData;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.dto.SplitOrderDTO;
import com.ruyuan.eshop.order.domain.request.CreateOrderRequest;
import com.ruyuan.eshop.order.domain.request.SubOrderCreateRequest;
import com.ruyuan.eshop.order.statemachine.StateMachineFactory;
import com.ruyuan.eshop.order.statemachine.action.OrderStateAction;
import com.ruyuan.process.engine.model.ProcessContextFactory;
import com.ruyuan.process.engine.process.ProcessContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 创建订单Action
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
public class OrderCreateAction extends OrderStateAction<CreateOrderRequest> {

    @Autowired
    private ProcessContextFactory processContextFactory;

    @Autowired
    private StateMachineFactory stateMachineFactory;

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_CREATED;
    }

    @Override
    protected OrderInfoDTO onStateChangeInternal(OrderStatusChangeEnum event, CreateOrderRequest context) {
        // 获取流程引擎并执行
        ProcessContext masterOrderCreateProcess = processContextFactory.getContext("masterOrderCreateProcess");
        masterOrderCreateProcess.set("createOrderRequest", context);
        masterOrderCreateProcess.start();

        // 流程执行完之后，获取返回参数
        Set<Integer> productTypeSet = masterOrderCreateProcess.get("productTypeSet");
        FullOrderData fullOrderData = masterOrderCreateProcess.get("fullMasterOrderData");
        OrderInfoDTO orderInfoDTO = masterOrderCreateProcess.get("orderInfoDTO");
        orderInfoDTO.setProductTypeSet(productTypeSet);
        orderInfoDTO.setFullOrderData(fullOrderData);
        log.info("OrderCreated->orderId={}",orderInfoDTO.getOrderId());
        return orderInfoDTO;
    }

    @Override
    protected void postStateChange(OrderStatusChangeEnum event, OrderInfoDTO context) {
        // 先发送主单的状态变更消息
        super.postStateChange(event, context);

        Set<Integer> productTypeSet = context.getProductTypeSet();
        FullOrderData fullOrderData = context.getFullOrderData();
        if (productTypeSet.size() <= 1) {
            return;
        }
        // 存在多种商品类型，需要按商品类型进行拆单
        for (Integer productType : productTypeSet) {
            doSplitOrderAction(new SplitOrderDTO(productType, fullOrderData));
        }
    }

    /**
     * 触发拆单状态机
     */
    @ConsistencyTask(id = "doSplitOrder", alertActionBeanName = "tendConsistencyAlerter")
    public void doSplitOrderAction(SplitOrderDTO splitOrderDTO) {
        // 通过状态机来生成子订单
        StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.NULL);
        orderStateMachine.fire(OrderStatusChangeEnum.SUB_ORDER_CREATED,
                new SubOrderCreateRequest(splitOrderDTO.getFullOrderData(), splitOrderDTO.getProductType()));
    }

}
