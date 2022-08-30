package com.ruyuan.eshop.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.*;
import com.ruyuan.eshop.order.domain.dto.OrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.dto.OrderLackItemDTO;
import com.ruyuan.eshop.order.domain.entity.*;
import com.ruyuan.eshop.order.enums.OrderQueryDataTypeEnums;
import com.ruyuan.eshop.order.service.AfterSaleQueryService;
import com.ruyuan.eshop.order.service.OrderLackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单详情builder
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
@Scope("prototype")
public class OrderDetailBuilder {

    private OrderDetailDTO.OrderDetailDTOBuilder builder = OrderDetailDTO.builder();

    private OrderInfoDO orderInfo;

    private boolean allNull = true;

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private OrderAmountDetailDAO orderAmountDetailDAO;

    @Autowired
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private OrderAmountDAO orderAmountDAO;

    @Autowired
    private AfterSaleQueryService afterSaleQueryService;

    @Autowired
    private OrderLackService orderLackService;

    @Autowired
    private OrderConverter orderConverter;
    /**
     * 订单快照数据存储的DAO组件
     */
    @Autowired
    private OrderSnapshotDAO orderSnapshotDAO;

    /**
     * 订单操作日志存储的DAO组件
     */
    @Autowired
    private OrderOperateLogDAO orderOperateLogDAO;

    public OrderDetailBuilder buildOrderInfo(OrderQueryDataTypeEnums dataType, String orderId) {
        if (OrderQueryDataTypeEnums.ORDER.equals(dataType)) {
            // 查询订单
            OrderInfoDO orderInfo = orderInfoDAO.getByOrderId(orderId);

            if (isNull(orderInfo)) {
                return this;
            }

            OrderInfoDTO orderInfoDTO = orderConverter.orderInfoDO2DTO(orderInfo);
            orderInfoDTO.setCreatedTime(orderInfo.getGmtCreate());
            builder.orderInfo(orderInfoDTO);
            this.orderInfo = orderInfo;
        }
        return this;
    }

    public OrderDetailBuilder buildOrderItems(OrderQueryDataTypeEnums dataType, String orderId) {
        if (OrderQueryDataTypeEnums.ORDER_ITEM.equals(dataType)) {
            // 查询订单条目
            List<OrderItemDO> orderItems = orderItemDAO.listByOrderId(orderId);
            if (isEmpty(orderItems)) {
                return this;
            }
            builder.orderItems(orderConverter.orderItemDO2DTO(orderItems));
        }
        return this;
    }

    public OrderDetailBuilder buildOrderAmountDetails(OrderQueryDataTypeEnums dataType, String orderId) {
        if (OrderQueryDataTypeEnums.ORDER_AMOUNT_DETAIL.equals(dataType)) {
            // 查询订单费用明细
            List<OrderAmountDetailDO> orderAmountDetails = orderAmountDetailDAO.listByOrderId(orderId);
            if (isEmpty(orderAmountDetails)) {
                return this;
            }

            builder.orderAmountDetails(orderConverter.orderAmountDetailDO2DTO(orderAmountDetails));
        }
        return this;
    }

    public OrderDetailBuilder buildOrderDeliveryDetail(OrderQueryDataTypeEnums dataType, String orderId) {
        if (OrderQueryDataTypeEnums.DELIVERY.equals(dataType)) {
            // 查询订单配送信息
            OrderDeliveryDetailDO orderDeliveryDetail = orderDeliveryDetailDAO.getByOrderId(orderId);

            if (isNull(orderDeliveryDetail)) {
                return this;
            }

            builder.orderDeliveryDetail(orderConverter.orderDeliveryDetailDO2DTO(orderDeliveryDetail));
        }
        return this;
    }

    public OrderDetailBuilder buildOrderPaymentDetails(OrderQueryDataTypeEnums dataType, String orderId) {
        if (OrderQueryDataTypeEnums.PAYMENT.equals(dataType)) {
            // 查询订单支付明细
            List<OrderPaymentDetailDO> orderPaymentDetails = orderPaymentDetailDAO.listByOrderId(orderId);

            if (isEmpty(orderPaymentDetails)) {
                return this;
            }

            builder.orderPaymentDetails(orderConverter.orderPaymentDetailDO2DTO(orderPaymentDetails));
        }
        return this;
    }

    public OrderDetailBuilder buildOrderAmounts(OrderQueryDataTypeEnums dataType, String orderId) {
        if (OrderQueryDataTypeEnums.AMOUNT.equals(dataType)) {
            // 查询订单费用类型
            List<OrderAmountDO> orderAmounts = orderAmountDAO.listByOrderId(orderId);
            if (isEmpty(orderAmounts)) {
                return this;
            }
            builder.orderAmounts(orderAmounts.stream().collect(
                    Collectors.toMap(OrderAmountDO::getAmountType, OrderAmountDO::getAmount, (v1, v2) -> v1)));
        }
        return this;
    }

    public OrderDetailBuilder buildOrderOperateLogs(OrderQueryDataTypeEnums dataType, String orderId) {
        if (OrderQueryDataTypeEnums.OPERATE_LOG.equals(dataType)) {
            // 查询订单操作日志
            List<OrderOperateLogDO> orderOperateLogs = orderOperateLogDAO.listByOrderId(orderId);
            if (isEmpty(orderOperateLogs)) {
                return this;
            }

            builder.orderOperateLogs(orderConverter.orderOperateLogsDO2DTO(orderOperateLogs));
        }
        return this;
    }

    public OrderDetailBuilder buildOrderSnapshots(OrderQueryDataTypeEnums dataType, String orderId) {
        if (OrderQueryDataTypeEnums.SNAPSHOT.equals(dataType)) {
            OrderInfoDO orderInfoDO = orderInfoDAO.getByOrderId(orderId);
            // 查询订单快照
            List<OrderSnapshotDO> orderSnapshots = orderSnapshotDAO.queryOrderSnapshotByOrderId(orderId, OrderSnapshotDAOUtils.getRowKeyPrefixList(orderInfoDO));
            if (isEmpty(orderSnapshots)) {
                return this;
            }
            builder.orderSnapshots(orderConverter.orderSnapshotsDO2DTO(orderSnapshots));
        }
        return this;
    }

    public OrderDetailBuilder buildOrderLackItems(OrderQueryDataTypeEnums dataType, String orderId) {
        if (OrderQueryDataTypeEnums.LACK_ITEM.equals(dataType)) {
            // 查询缺品退款信息
            if (Objects.isNull(orderInfo)) {
                orderInfo = orderInfoDAO.getByOrderId(orderId);
            }
            List<OrderLackItemDTO> lackItems = null;
            if (Objects.nonNull(orderInfo) && orderLackService.isOrderLacked(orderInfo)) {
                lackItems = afterSaleQueryService.getOrderLackItemInfo(orderId);
            }
            if (isEmpty(lackItems)) {
                return this;
            }
            builder.lackItems(lackItems);
        }
        return this;
    }

    public OrderDetailDTO build() {
        return builder.build();
    }

    public boolean allNull() {
        return allNull;
    }

    private boolean isNull(Object obj) {
        if (null != obj) {
            allNull = false;
            return false;
        }
        return true;
    }

    private boolean isEmpty(Collection<?> col) {
        if (CollectionUtils.isNotEmpty(col)) {
            allNull = false;
            return false;
        }
        return true;
    }
}
