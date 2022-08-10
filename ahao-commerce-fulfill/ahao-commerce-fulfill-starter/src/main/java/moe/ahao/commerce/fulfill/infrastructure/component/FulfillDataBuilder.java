package moe.ahao.commerce.fulfill.infrastructure.component;

import lombok.Data;
import moe.ahao.commerce.common.enums.OrderTypeEnum;
import moe.ahao.commerce.common.enums.ProductTypeEnum;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillOperateTypeEnum;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillStatusEnum;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillTypeEnum;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillItemDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillLogDO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class FulfillDataBuilder {
    private final String fulfillId;
    private final ReceiveFulfillCommand command;
    private final FulfillData fulfillData;

    public FulfillDataBuilder(String fulfillId, ReceiveFulfillCommand command) {
        this.fulfillId = fulfillId;
        this.command = command;
        this.fulfillData = new FulfillData();
    }

    /**
     * 构建OrderFulfill对象
     */
    private OrderFulfillDO buildOrderFulfill() {
        OrderFulfillDO data = new OrderFulfillDO();
        data.setBusinessIdentifier(command.getBusinessIdentifier());
        data.setFulfillId(fulfillId);
        data.setOrderId(command.getOrderId());
        data.setSellerId(command.getSellerId());
        data.setUserId(command.getUserId());
        // 设置履约单状态为"已创建"
        data.setStatus(OrderFulfillStatusEnum.FULFILL.getCode());
        data.setOrderFulfillType(OrderFulfillTypeEnum.getByOrderType(command.getOrderType()).getCode());
        data.setDeliveryType(command.getDeliveryType());
        data.setReceiverName(command.getReceiverName());
        data.setReceiverPhone(command.getReceiverPhone());
        data.setReceiverProvince(command.getReceiverProvince());
        data.setReceiverCity(command.getReceiverCity());
        data.setReceiverArea(command.getReceiverArea());
        data.setReceiverStreet(command.getReceiverStreet());
        data.setReceiverDetailAddress(command.getReceiverDetailAddress());
        data.setReceiverLon(command.getReceiverLon());
        data.setReceiverLat(command.getReceiverLat());
        // 配送信息等已配送再填写
        // data.setDelivererNo();
        // data.setDelivererName();
        // data.setDelivererPhone();
        // data.setLogisticsCode();
        data.setUserRemark(command.getUserRemark());
        data.setPayType(command.getPayType());
        data.setPayAmount(command.getPayAmount());
        data.setTotalAmount(command.getTotalAmount());
        data.setDeliveryAmount(command.getDeliveryAmount());
        // 设置预售履约单预售商品信息
        command.getReceiveOrderItems().stream()
            .filter(item -> ProductTypeEnum.PRE_SALE.getCode().equals(item.getProductType()))
            .findFirst()
            .ifPresent(preSaleOrderItem -> data.setExtJson(preSaleOrderItem.getExtJson()));

        this.fulfillData.setOrderFulfill(data);
        return data;
    }

    /**
     * 构建OrderFulfillItemDO对象
     */
    private List<OrderFulfillItemDO> buildOrderFulfillItems() {
        List<ReceiveFulfillCommand.ReceiveOrderItem> receiveOrderItems = command.getReceiveOrderItems();

        List<OrderFulfillItemDO> list = new ArrayList<>();
        for (ReceiveFulfillCommand.ReceiveOrderItem receiveOrderItem : receiveOrderItems) {
            OrderFulfillItemDO data = new OrderFulfillItemDO();
            data.setFulfillId(fulfillId);
            data.setSkuCode(receiveOrderItem.getSkuCode());
            data.setProductType(receiveOrderItem.getProductType());
            data.setProductName(receiveOrderItem.getProductName());
            data.setSalePrice(receiveOrderItem.getSalePrice());
            data.setSaleQuantity(receiveOrderItem.getSaleQuantity());
            data.setProductUnit(receiveOrderItem.getProductUnit());
            data.setPayAmount(receiveOrderItem.getPayAmount());
            data.setOriginAmount(receiveOrderItem.getOriginAmount());

            list.add(data);
        }

        this.fulfillData.setOrderFulfillItems(list);
        return list;
    }

    /**
     * 构建OrderFulfillLogDO对象
     */
    private OrderFulfillLogDO buildOrderFulfillLog() {
        OrderFulfillLogDO data = new OrderFulfillLogDO();
        data.setOrderId(command.getOrderId());
        data.setFulfillId(fulfillId);
        data.setOperateType(OrderFulfillOperateTypeEnum.NEW_ORDER.getCode());
        data.setPreStatus(OrderFulfillOperateTypeEnum.NEW_ORDER.getFromStatus().getCode());
        data.setCurrentStatus(OrderFulfillOperateTypeEnum.NEW_ORDER.getToStatus().getCode());
        data.setRemark(OrderFulfillOperateTypeEnum.NEW_ORDER.getMsg());

        this.fulfillData.setOrderFulfillLog(data);
        return data;
    }

    public FulfillData build() {
        Optional.ofNullable(fulfillData.getOrderFulfill()).orElseGet(this::buildOrderFulfill);
        Optional.ofNullable(fulfillData.getOrderFulfillItems()).orElseGet(this::buildOrderFulfillItems);
        Optional.ofNullable(fulfillData.getOrderFulfillLog()).orElseGet(this::buildOrderFulfillLog);
        return fulfillData;
    }

    @Data
    public static class FulfillData {
        // 履约单信息
        private OrderFulfillDO orderFulfill;
        // 履约单条目
        private List<OrderFulfillItemDO> orderFulfillItems;
        // 履约单状态变更日志信息
        private OrderFulfillLogDO orderFulfillLog;

        public List<FulfillData> split() {
            boolean isPreSale = Objects.equals(orderFulfill.getOrderFulfillType(), OrderFulfillTypeEnum.PRE_SALE.getCode());
            boolean moreThanOne = orderFulfillItems.size() > 1;
            boolean shouldSplit = isPreSale && moreThanOne; // 预售单才拆分
            if (!shouldSplit) {
                return Collections.singletonList(this);
            }

            // 对拆分后的子订单条目, 生成相关的子订单数据
            String orderId = orderFulfill.getOrderId();
            List<FulfillData> subOrderDataList = new ArrayList<>();
            BigDecimal totalDeliveryAmount = BigDecimal.ZERO;
            for (int i = 0, len = orderFulfillItems.size(); i < len; i++) {
                OrderFulfillItemDO orderFulfillItem = orderFulfillItems.get(i);
                FulfillData subFulfillData = new FulfillData();
                subOrderDataList.add(subFulfillData);

                // 生成新的子订单的履约单号
                String subFulfillId = orderFulfill.getFulfillId() + String.format("%03d", i);

                // 拆分后的履约单的条目
                OrderFulfillItemDO orderFulfillItemDO = new OrderFulfillItemDO(orderFulfillItem);
                orderFulfillItemDO.setFulfillId(subFulfillId);
                orderFulfillItemDO.setSkuCode(orderFulfillItem.getSkuCode());
                orderFulfillItemDO.setProductType(orderFulfillItem.getProductType());
                orderFulfillItemDO.setProductName(orderFulfillItem.getProductName());
                orderFulfillItemDO.setSalePrice(orderFulfillItem.getSalePrice());
                orderFulfillItemDO.setSaleQuantity(orderFulfillItem.getSaleQuantity());
                orderFulfillItemDO.setProductUnit(orderFulfillItem.getProductUnit());
                orderFulfillItemDO.setPayAmount(orderFulfillItem.getPayAmount());
                orderFulfillItemDO.setOriginAmount(orderFulfillItem.getOriginAmount());
                subFulfillData.setOrderFulfillItems(Collections.singletonList(orderFulfillItemDO));

                // 子履约单的详细信息
                OrderFulfillDO orderFulfillDO = new OrderFulfillDO(orderFulfill);
                orderFulfillDO.setFulfillId(subFulfillId);
                // 计算拆分履约单后的运费
                if (i == len - 1) {
                    // 最后一个履约单的运费 = 总运费 - 前面所有履约单的运费之和
                    BigDecimal deliveryAmount = orderFulfillDO.getDeliveryAmount().subtract(totalDeliveryAmount);
                    orderFulfillDO.setDeliveryAmount(deliveryAmount);
                } else {
                    // 拆分后的运费 = 实付金额/总实付金额 * 运费
                    BigDecimal rate = orderFulfillItemDO.getPayAmount().divide(orderFulfillDO.getPayAmount(), 2, RoundingMode.DOWN);
                    BigDecimal deliveryAmount = orderFulfillDO.getDeliveryAmount().multiply(rate);
                    totalDeliveryAmount = totalDeliveryAmount.add(deliveryAmount);
                    orderFulfillDO.setDeliveryAmount(deliveryAmount);
                }
                subFulfillData.setOrderFulfill(orderFulfillDO);

                // 订单状态变更日志信息
                OrderFulfillLogDO subOrderFulfillLog = new OrderFulfillLogDO();
                subOrderFulfillLog.setOrderId(orderId);
                subOrderFulfillLog.setFulfillId(subFulfillId);
                subOrderFulfillLog.setOperateType(OrderFulfillOperateTypeEnum.NEW_ORDER.getCode());
                subOrderFulfillLog.setPreStatus(OrderFulfillOperateTypeEnum.NEW_ORDER.getFromStatus().getCode());
                subOrderFulfillLog.setCurrentStatus(OrderFulfillOperateTypeEnum.NEW_ORDER.getToStatus().getCode());
                subOrderFulfillLog.setRemark(OrderFulfillOperateTypeEnum.NEW_ORDER.getMsg());
                subFulfillData.setOrderFulfillLog(subOrderFulfillLog);
            }
            return subOrderDataList;
        }
    }
}
