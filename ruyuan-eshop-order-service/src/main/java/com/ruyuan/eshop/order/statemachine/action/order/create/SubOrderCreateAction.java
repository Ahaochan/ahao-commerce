package com.ruyuan.eshop.order.statemachine.action.order.create;

import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.order.builder.FullOrderData;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.request.SubOrderCreateRequest;
import com.ruyuan.eshop.order.statemachine.action.OrderStateAction;
import com.ruyuan.process.engine.model.ProcessContextFactory;
import com.ruyuan.process.engine.process.ProcessContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 创建子订单Action
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
public class SubOrderCreateAction extends OrderStateAction<SubOrderCreateRequest> {

    @Autowired
    private ProcessContextFactory processContextFactory;

    @Override
    protected OrderInfoDTO onStateChangeInternal(OrderStatusChangeEnum event, SubOrderCreateRequest context) {
        FullOrderData fullMasterOrderData = context.getFullMasterOrderData();
        Integer productType = context.getProductType();

        // 获取流程引擎并执行
        ProcessContext subOrderCreateProcess = processContextFactory.getContext("subOrderCreateProcess");
        subOrderCreateProcess.set("fullMasterOrderData", fullMasterOrderData);
        subOrderCreateProcess.set("productType", productType);
        subOrderCreateProcess.start();

        // 流程执行完之后，获取返回参数
        OrderInfoDTO orderInfoDTO = subOrderCreateProcess.get("orderInfoDTO");
        log.info("SubOrderCreated->subOrderId={},parentOrderId={}",orderInfoDTO.getOrderId(),orderInfoDTO.getParentOrderId());
        return orderInfoDTO;
    }

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.SUB_ORDER_CREATED;
    }
}
