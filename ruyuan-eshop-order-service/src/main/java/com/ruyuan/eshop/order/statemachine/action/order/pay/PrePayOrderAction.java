package com.ruyuan.eshop.order.statemachine.action.order.pay;

import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.dao.OrderPaymentDetailDAO;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.entity.OrderPaymentDetailDO;
import com.ruyuan.eshop.order.statemachine.action.OrderStateAction;
import com.ruyuan.eshop.pay.domain.dto.PayOrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 订单预支付Action
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
public class PrePayOrderAction extends OrderStateAction<PayOrderDTO> {

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Override
    protected OrderInfoDTO onStateChangeInternal(OrderStatusChangeEnum event, PayOrderDTO context) {
        // 更新订单表与支付信息表
        updateOrderPaymentInfo(context);

        // 返回null，不会发送标准订单变更消息
        return null;
    }

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_PREPAY;
    }

    /**
     * 预支付更新订单支付信息
     */
    private void updateOrderPaymentInfo(PayOrderDTO payOrderDTO) {
        // 提取业务参数
        String orderId = payOrderDTO.getOrderId();
        Integer payType = payOrderDTO.getPayType();
        String outTradeNo = payOrderDTO.getOutTradeNo();
        Date payTime = new Date();

        // 更新主订单支付信息
        updateMasterOrderPaymentInfo(orderId, payType, payTime, outTradeNo);

        // 更新子订单支付信息
        updateSubOrderPaymentInfo(orderId, payType, payTime, outTradeNo);
    }

    /**
     * 更新主订单支付信息
     */
    private void updateMasterOrderPaymentInfo(String orderId, Integer payType, Date payTime, String outTradeNo) {
        List<String> orderIds = Collections.singletonList(orderId);
        // 更新订单表支付信息
        updateOrderInfo(orderIds, payType, payTime);
        // 更新支付明细信息
        updateOrderPaymentDetail(orderIds, payType, payTime, outTradeNo);
    }

    /**
     * 更新订单信息表
     */
    private void updateOrderInfo(List<String> orderIds, Integer payType, Date payTime) {
        if (orderIds == null) {
            return;
        }
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        orderInfoDO.setPayType(payType);
        orderInfoDO.setPayTime(payTime);
        if (orderIds.size() == 1) {
            orderInfoDAO.updateByOrderId(orderInfoDO, orderIds.get(0));
        } else {
            orderInfoDAO.updateBatchByOrderIds(orderInfoDO, orderIds);
        }

    }

    /**
     * 更新订单支付明细表
     */
    private void updateOrderPaymentDetail(List<String> orderIds, Integer payType, Date payTime, String outTradeNo) {
        if (orderIds == null) {
            return;
        }
        OrderPaymentDetailDO orderPaymentDetailDO = new OrderPaymentDetailDO();
        orderPaymentDetailDO.setPayTime(payTime);
        orderPaymentDetailDO.setPayType(payType);
        orderPaymentDetailDO.setOutTradeNo(outTradeNo);
        if (orderIds.size() == 1) {
            orderPaymentDetailDAO.updateByOrderId(orderPaymentDetailDO, orderIds.get(0));
        } else {
            orderPaymentDetailDAO.updateBatchByOrderIds(orderPaymentDetailDO, orderIds);
        }
    }

    /**
     * 更新子订单支付信息
     */
    private void updateSubOrderPaymentInfo(String orderId, Integer payType, Date payTime, String outTradeNo) {
        // 判断是否存在子订单，不存在则不处理
        List<String> subOrderIds = orderInfoDAO.listSubOrderIds(orderId);
        if (subOrderIds == null || subOrderIds.isEmpty()) {
            return;
        }

        // 更新子订单支付信息
        updateOrderInfo(subOrderIds, payType, payTime);

        // 更新子订单支付明细信息
        updateOrderPaymentDetail(subOrderIds, payType, payTime, outTradeNo);
    }

}
