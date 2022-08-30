package com.ruyuan.eshop.order.statemachine.action.order.cancel;

import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.common.enums.OrderStatusEnum;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.dao.OrderOperateLogDAO;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.entity.OrderOperateLogDO;
import com.ruyuan.eshop.order.domain.request.CancelOrderAssembleRequest;
import com.ruyuan.eshop.order.statemachine.action.OrderStateAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 订单取消抽象Action
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
public abstract class AbstractOrderCancelAction extends OrderStateAction<CancelOrderAssembleRequest> {

    @Autowired
    private OrderConverter orderConverter;

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderOperateLogDAO orderOperateLogDAO;

    @Override
    protected OrderInfoDTO onStateChangeInternal(OrderStatusChangeEnum event, CancelOrderAssembleRequest context) {
        OrderInfoDO orderInfoDO = orderConverter.orderInfoDTO2DO(context.getOrderInfoDTO());
        List<OrderOperateLogDO> orderOperateLogDOList = new ArrayList<>();
        String orderId = orderInfoDO.getOrderId();

        //  1、查询全部订单
        List<OrderInfoDO> allOrderList = orderInfoDAO.getAllByOrderId(orderId);


        //  2、更新订单状态为已取消
        OrderInfoDO updateOrder = new OrderInfoDO();
        updateOrder.setCancelType(context.getCancelType().toString());
        updateOrder.setOrderStatus(OrderStatusEnum.CANCELLED.getCode());
        updateOrder.setCancelTime(new Date());
        orderInfoDAO.updateByOrderIdOrParentId(updateOrder, orderId);

        log.info("更新订单信息OrderInfo状态: orderId:{},status:{}", orderInfoDO.getOrderId(), orderInfoDO.getOrderStatus());

        //  3、封装订单操作日志数据
        for (OrderInfoDO orderInfo : allOrderList) {
            OrderOperateLogDO orderOperateLogDO = new OrderOperateLogDO();
            orderOperateLogDO.setOrderId(orderInfo.getOrderId());
            orderOperateLogDO.setPreStatus(getPreStatus());
            orderOperateLogDO.setCurrentStatus(OrderStatusEnum.CANCELLED.getCode());
            setOperateTypeAndRemark(orderOperateLogDO, context.getCancelType());
            orderOperateLogDOList.add(orderOperateLogDO);
        }

        //  4、新增订单操作操作日志表
        orderOperateLogDAO.batchSave(orderOperateLogDOList);
        log.info("新增订单操作日志OrderOperateLog状态,orderId:{}", orderInfoDO.getOrderId());

        //  5、返回标准订单信息
        return orderConverter.orderInfoDO2DTO(orderInfoDO);
    }


    /**
     * 设置操作类型和备注
     */
    protected abstract void setOperateTypeAndRemark(OrderOperateLogDO operateLogDO, Integer cancelType);

    private Integer getPreStatus() {
        return event().getFromStatus().getCode();
    }

}
