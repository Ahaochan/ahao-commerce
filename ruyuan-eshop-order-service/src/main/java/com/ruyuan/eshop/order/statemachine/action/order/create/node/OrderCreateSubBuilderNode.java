package com.ruyuan.eshop.order.statemachine.action.order.create.node;

import cn.hutool.json.JSONUtil;
import com.ruyuan.eshop.common.enums.AmountTypeEnum;
import com.ruyuan.eshop.common.enums.OrderStatusEnum;
import com.ruyuan.eshop.common.utils.JsonUtil;
import com.ruyuan.eshop.order.builder.FullOrderData;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.OrderSnapshotDAO;
import com.ruyuan.eshop.order.dao.OrderSnapshotDAOUtils;
import com.ruyuan.eshop.order.domain.entity.*;
import com.ruyuan.eshop.order.enums.OrderNoTypeEnum;
import com.ruyuan.eshop.order.enums.SnapshotTypeEnum;
import com.ruyuan.eshop.order.hbase.entity.OrderInfoExtJsonDTO;
import com.ruyuan.eshop.order.manager.OrderNoManager;
import com.ruyuan.eshop.order.service.impl.NewOrderDataHolder;
import com.ruyuan.eshop.order.utils.OrderTypeUtils;
import com.ruyuan.process.engine.process.ProcessContext;
import com.ruyuan.process.engine.process.StandardProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 创建订单子订单构建节点
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
public class OrderCreateSubBuilderNode extends StandardProcessor {

    @Autowired
    private OrderNoManager orderNoManager;

    @Autowired
    private OrderConverter orderConverter;

    @Override
    protected void processInternal(ProcessContext processContext) {
        FullOrderData fullMasterOrderData = processContext.get("fullMasterOrderData");
        Integer productType = processContext.get("productType");

        // 1、生成子订单
        FullOrderData fullSubOrderData = addNewSubOrder(fullMasterOrderData, productType);


        // 2、封装子订单数据到NewOrderData对象中
        NewOrderDataHolder newOrderDataHolder = new NewOrderDataHolder();
        newOrderDataHolder.appendOrderData(fullSubOrderData);

        processContext.set("fullMasterOrderData", fullMasterOrderData);
        processContext.set("newOrderDataHolder", newOrderDataHolder);
        processContext.set("fullSubOrderData", fullSubOrderData);
    }

    /**
     * 生成子单
     *
     * @param fullOrderData 主单数据
     * @param productType   商品类型
     */
    private FullOrderData addNewSubOrder(FullOrderData fullOrderData, Integer productType) {

        // 主单信息
        OrderInfoDO orderInfoDO = fullOrderData.getOrderInfoDO();
        // 主订单条目
        List<OrderItemDO> orderItemDOList = fullOrderData.getOrderItemDOList();
        // 主订单配送信息
        OrderDeliveryDetailDO orderDeliveryDetailDO = fullOrderData.getOrderDeliveryDetailDO();
        // 主订单支付信息
        List<OrderPaymentDetailDO> orderPaymentDetailDOList = fullOrderData.getOrderPaymentDetailDOList();
        // 主订单费用信息
        List<OrderAmountDO> orderAmountDOList = fullOrderData.getOrderAmountDOList();
        // 主订单费用明细
        List<OrderAmountDetailDO> orderAmountDetailDOList = fullOrderData.getOrderAmountDetailDOList();
        // 主订单状态变更日志信息
        OrderOperateLogDO orderOperateLogDO = fullOrderData.getOrderOperateLogDO();
        // 主订单快照数据
        List<OrderSnapshotDO> orderSnapshotDOList = fullOrderData.getOrderSnapshotDOList();


        // 父订单号
        String orderId = orderInfoDO.getOrderId();
        // 用户ID
        String userId = orderInfoDO.getUserId();

        // 生成新的子订单的订单号
        String subOrderId = orderNoManager.genOrderId(OrderNoTypeEnum.SALE_ORDER.getCode(), userId);

        // 子订单全量的数据
        FullOrderData subFullOrderData = new FullOrderData();

        // 过滤出当前商品类型的订单条目信息
        List<OrderItemDO> subOrderItemDOList = orderItemDOList.stream()
                .filter(orderItemDO -> productType.equals(orderItemDO.getProductType()))
                .collect(Collectors.toList());

        // 统计子单总金额
        Integer subTotalAmount = 0;
        Integer subRealPayAmount = 0;
        for (OrderItemDO subOrderItemDO : subOrderItemDOList) {
            subTotalAmount += subOrderItemDO.getOriginAmount();
            subRealPayAmount += subOrderItemDO.getPayAmount();
        }

        // 订单主信息
        OrderInfoDO newSubOrderInfo = orderConverter.copyOrderInfoDTO(orderInfoDO);
        newSubOrderInfo.setId(null);
        newSubOrderInfo.setOrderId(subOrderId);
        newSubOrderInfo.setParentOrderId(orderId);
        newSubOrderInfo.setOrderStatus(OrderStatusEnum.INVALID.getCode());
        newSubOrderInfo.setTotalAmount(subTotalAmount);
        newSubOrderInfo.setPayAmount(subRealPayAmount);
        OrderTypeUtils.setOrderType(productType, newSubOrderInfo);
        subFullOrderData.setOrderInfoDO(newSubOrderInfo);


        // 订单条目
        List<OrderItemDO> newSubOrderItemList = new ArrayList<>();
        for (OrderItemDO orderItemDO : subOrderItemDOList) {
            OrderItemDO newSubOrderItem = orderConverter.copyOrderItemDO(orderItemDO);
            newSubOrderItem.setId(null);
            newSubOrderItem.setOrderId(subOrderId);
            String subOrderItemId = getSubOrderItemId(orderItemDO.getOrderItemId(), subOrderId);
            newSubOrderItem.setOrderItemId(subOrderItemId);
            newSubOrderItemList.add(newSubOrderItem);
        }
        subFullOrderData.setOrderItemDOList(newSubOrderItemList);

        // 订单配送地址信息
        OrderDeliveryDetailDO newSubOrderDeliveryDetail = orderConverter.copyOrderDeliverDetailDO(orderDeliveryDetailDO);
        newSubOrderDeliveryDetail.setId(null);
        newSubOrderDeliveryDetail.setOrderId(subOrderId);
        subFullOrderData.setOrderDeliveryDetailDO(newSubOrderDeliveryDetail);


        Map<String, OrderItemDO> subOrderItemMap = subOrderItemDOList.stream()
                .collect(Collectors.toMap(OrderItemDO::getOrderItemId, Function.identity()));

        // 统计子订单费用信息
        Integer subTotalOriginPayAmount = 0;
        Integer subTotalCouponDiscountAmount = 0;
        Integer subTotalRealPayAmount = 0;

        // 订单费用明细
        List<OrderAmountDetailDO> subOrderAmountDetailList = new ArrayList<>();
        for (OrderAmountDetailDO orderAmountDetailDO : orderAmountDetailDOList) {
            String orderItemId = orderAmountDetailDO.getOrderItemId();
            if (!subOrderItemMap.containsKey(orderItemId)) {
                continue;
            }
            OrderAmountDetailDO subOrderAmountDetail = orderConverter.copyOrderAmountDetail(orderAmountDetailDO);
            subOrderAmountDetail.setId(null);
            subOrderAmountDetail.setOrderId(subOrderId);
            String subOrderItemId = getSubOrderItemId(orderItemId, subOrderId);
            subOrderAmountDetail.setOrderItemId(subOrderItemId);
            subOrderAmountDetailList.add(subOrderAmountDetail);

            Integer amountType = orderAmountDetailDO.getAmountType();
            Integer amount = orderAmountDetailDO.getAmount();
            if (AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode().equals(amountType)) {
                subTotalOriginPayAmount += amount;
            }
            if (AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode().equals(amountType)) {
                subTotalCouponDiscountAmount += amount;
            }
            if (AmountTypeEnum.REAL_PAY_AMOUNT.getCode().equals(amountType)) {
                subTotalRealPayAmount += amount;
            }
        }
        subFullOrderData.setOrderAmountDetailDOList(subOrderAmountDetailList);

        // 订单费用信息
        List<OrderAmountDO> subOrderAmountList = new ArrayList<>();
        for (OrderAmountDO orderAmountDO : orderAmountDOList) {
            Integer amountType = orderAmountDO.getAmountType();
            OrderAmountDO subOrderAmount = orderConverter.copyOrderAmountDO(orderAmountDO);
            subOrderAmount.setId(null);
            subOrderAmount.setOrderId(subOrderId);
            if (AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode().equals(amountType)) {
                subOrderAmount.setAmount(subTotalOriginPayAmount);
                subOrderAmountList.add(subOrderAmount);
            }
            if (AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode().equals(amountType)) {
                subOrderAmount.setAmount(subTotalCouponDiscountAmount);
                subOrderAmountList.add(subOrderAmount);
            }
            if (AmountTypeEnum.REAL_PAY_AMOUNT.getCode().equals(amountType)) {
                subOrderAmount.setAmount(subTotalRealPayAmount);
                subOrderAmountList.add(subOrderAmount);
            }
        }
        subFullOrderData.setOrderAmountDOList(subOrderAmountList);

        // 订单支付信息
        List<OrderPaymentDetailDO> subOrderPaymentDetailDOList = new ArrayList<>();
        for (OrderPaymentDetailDO orderPaymentDetailDO : orderPaymentDetailDOList) {
            OrderPaymentDetailDO subOrderPaymentDetail = orderConverter.copyOrderPaymentDetailDO(orderPaymentDetailDO);
            subOrderPaymentDetail.setId(null);
            subOrderPaymentDetail.setOrderId(subOrderId);
            subOrderPaymentDetail.setPayAmount(subTotalRealPayAmount);
            subOrderPaymentDetailDOList.add(subOrderPaymentDetail);
        }
        subFullOrderData.setOrderPaymentDetailDOList(subOrderPaymentDetailDOList);

        // 订单状态变更日志信息
        OrderOperateLogDO subOrderOperateLogDO = orderConverter.copyOrderOperationLogDO(orderOperateLogDO);
        subOrderOperateLogDO.setId(null);
        subOrderOperateLogDO.setOrderId(subOrderId);
        subOrderOperateLogDO.setCurrentStatus(OrderStatusEnum.INVALID.getCode());
        String remark = "创建订单操作0-127";
        subOrderOperateLogDO.setRemark(remark);
        subFullOrderData.setOrderOperateLogDO(subOrderOperateLogDO);

        // 订单商品快照信息
        List<OrderSnapshotDO> subOrderSnapshotDOList = new ArrayList<>();
        for (OrderSnapshotDO orderSnapshotDO : orderSnapshotDOList) {
            OrderSnapshotDO subOrderSnapshotDO = orderConverter.copyOrderSnapshot(orderSnapshotDO);
            subOrderSnapshotDO.setId(null);
            subOrderSnapshotDO.setOrderId(subOrderId);
            if (SnapshotTypeEnum.ORDER_AMOUNT.getCode().equals(orderSnapshotDO.getSnapshotType())) {
                subOrderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(subOrderAmountList));
            } else if (SnapshotTypeEnum.ORDER_ITEM.getCode().equals(orderSnapshotDO.getSnapshotType())) {
                subOrderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(subOrderItemDOList));
            }
            Date currentDate = new Date();
            subOrderSnapshotDO.setGmtCreate(currentDate);
            subOrderSnapshotDO.setGmtModified(currentDate);
            subOrderSnapshotDOList.add(subOrderSnapshotDO);
        }

        // 预分配主单快照在HBASE中的rowKey前缀
        OrderInfoExtJsonDTO orderInfoExtJsonDTO = new OrderInfoExtJsonDTO();
        orderInfoExtJsonDTO.setOrderSnapshotRowKeyPrefixList(OrderSnapshotDAOUtils.batchGenerateRowKey(subOrderSnapshotDOList.size()));
        newSubOrderInfo.setExtJson(JSONUtil.toJsonStr(orderInfoExtJsonDTO));

        subFullOrderData.setOrderSnapshotDOList(subOrderSnapshotDOList);
        subFullOrderData.setOrderInfoDO(newSubOrderInfo);
        return subFullOrderData;
    }

    /**
     * 获取子订单的orderItemId值
     */
    private String getSubOrderItemId(String orderItemId, String subOrderId) {
        String postfix = orderItemId.substring(orderItemId.indexOf("_"));
        return subOrderId + postfix;
    }

}
