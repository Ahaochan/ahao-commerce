package com.ruyuan.eshop.order.statemachine.action;

import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.dao.OrderOperateLogDAO;
import com.ruyuan.eshop.order.dao.OrderPaymentDetailDAO;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.dto.SendOrderStdEventDTO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.entity.OrderOperateLogDO;
import com.ruyuan.eshop.order.domain.entity.OrderPaymentDetailDO;
import com.ruyuan.eshop.order.service.RocketMqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
public abstract class OrderStateAction<T> extends AbstractStateAction<T, OrderInfoDTO, OrderStatusChangeEnum> {

    @Autowired
    protected OrderConverter orderConverter;

    @Autowired
    private RocketMqService rocketMqService;

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private OrderOperateLogDAO orderOperateLogDAO;

    @Override
    protected void postStateChange(OrderStatusChangeEnum event, OrderInfoDTO context) {
        if (context == null) {
            return;
        }
        if (event.isSendEvent()) {
            // 发送订单标准状态变更消息
            rocketMqService.sendStandardOrderStatusChangeMessage(new SendOrderStdEventDTO(event, context.getOrderId()));
        }
    }

    /**
     * 更新订单状态
     *
     * @param orderIdList 订单ID
     * @param orderStatus 订单状态
     */
    protected void updateOrderStatus(List<String> orderIdList, Integer orderStatus) {
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        orderInfoDO.setOrderStatus(orderStatus);
        if (orderIdList.size() == 1) {
            orderInfoDAO.updateByOrderId(orderInfoDO, orderIdList.get(0));
        } else {
            orderInfoDAO.updateBatchByOrderIds(orderInfoDO, orderIdList);
        }
    }

    /**
     * 保存订单操作日志
     *
     * @param orderId        订单ID
     * @param operateType    操作类型
     * @param preOrderStatus 前一个状态
     * @param currentStatus  后一个状态
     * @param remark         备注
     */
    protected void saveOrderOperateLog(String orderId, Integer operateType, Integer preOrderStatus, Integer currentStatus,
                                       String remark) {
        OrderOperateLogDO orderOperateLogDO = new OrderOperateLogDO();
        orderOperateLogDO.setOrderId(orderId);
        orderOperateLogDO.setOperateType(operateType);
        orderOperateLogDO.setPreStatus(preOrderStatus);
        orderOperateLogDO.setCurrentStatus(currentStatus);
        orderOperateLogDO.setRemark(remark);
        orderOperateLogDAO.save(orderOperateLogDO);
    }

    /**
     * 更新订单支付时间和状态
     */
    protected void updateOrderStatusAndPayTime(List<String> orderIdList, Integer orderStatus, Date payTime) {
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        orderInfoDO.setOrderStatus(orderStatus);
        orderInfoDO.setPayTime(payTime);
        if (orderIdList.size() == 1) {
            orderInfoDAO.updateByOrderId(orderInfoDO, orderIdList.get(0));
        } else {
            orderInfoDAO.updateBatchByOrderIds(orderInfoDO, orderIdList);
        }
    }

    /**
     * 更新订单支付状态和时间
     */
    protected void updatePaymentStatusAndPayTime(List<String> orderIdList, Integer payStatus, Date payTime) {
        OrderPaymentDetailDO orderPaymentDetailDO = new OrderPaymentDetailDO();
        orderPaymentDetailDO.setPayStatus(payStatus);
        orderPaymentDetailDO.setPayTime(payTime);
        if (orderIdList.size() == 1) {
            orderPaymentDetailDAO.updateByOrderId(orderPaymentDetailDO, orderIdList.get(0));
        } else {
            orderPaymentDetailDAO.updateBatchByOrderIds(orderPaymentDetailDO, orderIdList);
        }
    }

}
