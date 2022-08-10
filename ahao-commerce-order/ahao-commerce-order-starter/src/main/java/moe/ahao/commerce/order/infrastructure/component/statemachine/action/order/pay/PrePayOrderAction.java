package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.pay;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.order.api.dto.PrePayOrderDTO;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.OrderStateAction;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderPaymentDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderPaymentDetailMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单预支付Action
 */
@Slf4j
@Component
public class PrePayOrderAction extends OrderStateAction<PrePayOrderDTO> {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderPaymentDetailMapper orderPaymentDetailMapper;

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_PREPAY;
    }

    @Override
    protected String onStateChangeInternal(OrderStatusChangeEnum event, PrePayOrderDTO context) {
        // 更新订单表与支付信息表
        // 当我们完成了预支付的操作之后，就是去更新订单和支付的数据表
        this.updateOrderPaymentInfo(context);
        // 返回null，不会发送标准订单变更消息
        return null;
    }

    /**
     * 预支付更新订单支付信息
     */
    private void updateOrderPaymentInfo(PrePayOrderDTO prePayOrderDTO) {
        String orderId = prePayOrderDTO.getOrderId();
        Integer payType = prePayOrderDTO.getPayType();
        String outTradeNo = prePayOrderDTO.getOutTradeNo();
        Date payTime = new Date();

        // 更新主订单支付信息
        this.updateMasterOrderPaymentInfo(orderId, payType, payTime, outTradeNo);

        // 更新子订单支付信息
        this.updateSubOrderPaymentInfo(orderId, payType, payTime, outTradeNo);
    }

    /**
     * 更新主订单支付信息
     */
    private void updateMasterOrderPaymentInfo(String orderId, Integer payType, Date payTime, String outTradeNo) {
        List<String> orderIds = Collections.singletonList(orderId);
        // 更新订单表支付信息
        this.updateOrderInfo(orderIds, payType, payTime);
        // 更新支付明细信息
        this.updateOrderPaymentDetail(orderIds, payType, payTime, outTradeNo);
    }

    /**
     * 更新子订单支付信息
     */
    private void updateSubOrderPaymentInfo(String orderId, Integer payType, Date payTime, String outTradeNo) {
        // 判断是否存在子订单，不存在则不处理
        List<OrderInfoDO> subOrderList = orderInfoMapper.selectListByParentOrderId(orderId);
        if (CollectionUtils.isEmpty(subOrderList)) {
            return;
        }
        List<String> subOrderIdList = subOrderList.stream().map(OrderInfoDO::getOrderId).collect(Collectors.toList());

        // 更新子订单支付信息
        this.updateOrderInfo(subOrderIdList, payType, payTime);

        // 更新子订单支付明细信息
        this.updateOrderPaymentDetail(subOrderIdList, payType, payTime, outTradeNo);
    }

    /**
     * 更新订单信息表
     */
    private void updateOrderInfo(List<String> orderIds, Integer payType, Date payTime) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return;
        }
        if (orderIds.size() == 1) {
            orderInfoMapper.updatePrePayInfoByOrderId(orderIds.get(0), payType, payTime);
        } else {
            orderInfoMapper.updatePrePayInfoByOrderIds(orderIds, payType, payTime);
        }
    }

    /**
     * 更新订单支付明细表
     */
    private void updateOrderPaymentDetail(List<String> orderIds, Integer payType, Date payTime, String outTradeNo) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return;
        }
        OrderPaymentDetailDO orderPaymentDetailDO = new OrderPaymentDetailDO();
        orderPaymentDetailDO.setPayTime(payTime);
        orderPaymentDetailDO.setPayType(payType);
        orderPaymentDetailDO.setOutTradeNo(outTradeNo);
        if (orderIds.size() == 1) {
            orderPaymentDetailMapper.updatePrePayInfoByOrderId(orderIds.get(0), payType, payTime, outTradeNo);
        } else {
            orderPaymentDetailMapper.updatePrePayInfoByOrderIds(orderIds, payType, payTime, outTradeNo);
        }
    }
}
