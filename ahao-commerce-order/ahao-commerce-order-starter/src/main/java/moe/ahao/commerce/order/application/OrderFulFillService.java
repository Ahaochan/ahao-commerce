package moe.ahao.commerce.order.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.AmountTypeEnum;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.OrderStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import moe.ahao.commerce.order.infrastructure.domain.dto.AfterFulfillDTO;
import moe.ahao.commerce.order.infrastructure.exception.OrderException;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderAmountDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderDeliveryDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderItemDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderAmountMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderDeliveryDetailMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单履约相关service
 */
@Slf4j
@Service
public class OrderFulFillService {
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderAmountMapper orderAmountMapper;
    @Autowired
    private OrderDeliveryDetailMapper orderDeliveryDetailMapper;

    @Autowired
    private StateMachineFactory stateMachineFactory;

    /**
     * 触发订单进行履约流程
     */
    public void triggerOrderFulFill(String orderId) {
        OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.PAID);
        orderStateMachine.fire(OrderStatusChangeEnum.ORDER_FULFILLED, orderId);
    }

    /**
     * 通知订单物流配送结果接口
     */
    public void informOrderWmsShipResult(AfterFulfillDTO afterFulfillDTO) throws OrderException {
        // 状态机流转
        // OrderStatusChangeEnum.ORDER_OUT_STOCKED;
        // OrderStatusChangeEnum.ORDER_DELIVERED;
        // OrderStatusChangeEnum.ORDER_SIGNED;
        OrderStatusChangeEnum event = afterFulfillDTO.getStatusChange();
        OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(event.getFromStatus());
        orderStateMachine.fire(event, afterFulfillDTO);
    }

    /**
     * 构建接受订单履约请求
     */
    public ReceiveFulfillCommand buildReceiveFulFillRequest(OrderInfoDO orderInfo) {
        OrderDeliveryDetailDO orderDeliveryDetail = orderDeliveryDetailMapper.selectOneByOrderId(orderInfo.getOrderId());
        List<OrderItemDO> orderItems = orderItemMapper.selectListByOrderId(orderInfo.getOrderId());
        OrderAmountDO deliveryAmount = orderAmountMapper.selectOneByOrderIdAndAmountType(orderInfo.getOrderId(), AmountTypeEnum.SHIPPING_AMOUNT.getCode());

        // 构造请求
        ReceiveFulfillCommand request = new ReceiveFulfillCommand();
        request.setBusinessIdentifier(orderInfo.getBusinessIdentifier());
        request.setOrderId(orderInfo.getOrderId());
        request.setSellerId(orderInfo.getSellerId());
        request.setUserId(orderInfo.getUserId());
        request.setDeliveryType(orderDeliveryDetail.getDeliveryType());
        request.setReceiverName(orderDeliveryDetail.getReceiverName());
        request.setReceiverPhone(orderDeliveryDetail.getReceiverPhone());
        request.setReceiverProvince(orderDeliveryDetail.getProvince());
        request.setReceiverCity(orderDeliveryDetail.getCity());
        request.setReceiverArea(orderDeliveryDetail.getArea());
        request.setReceiverStreet(orderDeliveryDetail.getStreet());
        request.setReceiverDetailAddress(orderDeliveryDetail.getDetailAddress());
        request.setReceiverLat(orderDeliveryDetail.getLat());
        request.setReceiverLon(orderDeliveryDetail.getLon());
        request.setPayType(orderInfo.getPayType());
        request.setPayAmount(orderInfo.getPayAmount());
        request.setTotalAmount(orderInfo.getTotalAmount());
        request.setReceiveOrderItems(this.buildReceiveOrderItemRequest(orderInfo, orderItems));

        // 运费
        if (deliveryAmount != null) {
            request.setDeliveryAmount(deliveryAmount.getAmount());
        }
        return request;
    }


    private List<ReceiveFulfillCommand.ReceiveOrderItem> buildReceiveOrderItemRequest(OrderInfoDO orderInfo, List<OrderItemDO> items) {
        List<ReceiveFulfillCommand.ReceiveOrderItem> itemRequests = new ArrayList<>();
        for (OrderItemDO item : items) {
            ReceiveFulfillCommand.ReceiveOrderItem request = new ReceiveFulfillCommand.ReceiveOrderItem();
            request.setSkuCode(item.getSkuCode());
            request.setProductName(item.getProductName());
            request.setSalePrice(item.getSalePrice());
            request.setSaleQuantity(item.getSaleQuantity());
            request.setProductUnit(item.getProductUnit());
            request.setPayAmount(item.getPayAmount());
            request.setOriginAmount(item.getOriginAmount());

            itemRequests.add(request);
        }
        return itemRequests;
    }
}
