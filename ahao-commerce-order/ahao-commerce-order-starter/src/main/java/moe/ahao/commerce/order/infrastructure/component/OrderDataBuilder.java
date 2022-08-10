package moe.ahao.commerce.order.infrastructure.component;

import lombok.Data;
import moe.ahao.commerce.common.enums.*;
import moe.ahao.commerce.market.api.dto.CalculateOrderAmountDTO;
import moe.ahao.commerce.market.api.dto.UserCouponDTO;
import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.api.command.GenOrderIdCommand;
import moe.ahao.commerce.order.application.GenOrderIdAppService;
import moe.ahao.commerce.order.infrastructure.config.OrderProperties;
import moe.ahao.commerce.order.infrastructure.enums.CommentStatusEnum;
import moe.ahao.commerce.order.infrastructure.enums.PayStatusEnum;
import moe.ahao.commerce.order.infrastructure.enums.SnapshotTypeEnum;
import moe.ahao.commerce.order.infrastructure.repository.impl.hbase.data.OrderSnapshotDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.data.OrderOperateLogDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.*;
import moe.ahao.commerce.product.api.dto.ProductSkuDTO;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrderDataBuilder {
    private final CreateOrderCommand createOrderCommand;
    private final List<ProductSkuDTO> productSkuList;
    private final CalculateOrderAmountDTO calculateOrderAmountDTO;
    private final OrderProperties orderProperties;

    private final OrderData orderData;

    public OrderDataBuilder(CreateOrderCommand createOrderCommand,
                            List<ProductSkuDTO> productSkuList,
                            CalculateOrderAmountDTO calculateOrderAmountDTO,
                            OrderProperties orderProperties) {
        this.createOrderCommand = createOrderCommand;
        this.productSkuList = productSkuList;
        this.calculateOrderAmountDTO = calculateOrderAmountDTO;

        this.orderProperties = orderProperties;
        this.orderData = new OrderData();
    }

    /**
     * 构建OrderInfoDO对象
     */
    private OrderInfoDO buildOrderInfo() {
        long currentTimeMillis = System.currentTimeMillis();
        Map<Integer, List<ProductSkuDTO>> productTypeMap = productSkuList.stream()
            .collect(Collectors.groupingBy(ProductSkuDTO::getProductType));

        OrderInfoDO data = new OrderInfoDO();
        // data.setId(0L);
        data.setOrderId(createOrderCommand.getOrderId());
        data.setParentOrderId(null);
        data.setBusinessIdentifier(createOrderCommand.getBusinessIdentifier());
        if(productTypeMap.keySet().size() <= 1) {
            data.setOrderType(getOrderType(productSkuList.get(0).getProductType()));
        } else {
            data.setOrderType(getOrderType(OrderTypeEnum.NORMAL.getCode()));
        }
        data.setUserRemark(createOrderCommand.getUserRemark());
        data.setOrderStatus(OrderStatusEnum.CREATED.getCode());
        data.setDeleteStatus(DeleteStatusEnum.NO.getCode());
        data.setCommentStatus(CommentStatusEnum.NO.getCode());

        data.setSellerId(createOrderCommand.getSellerId());

        data.setUserId(createOrderCommand.getUserId());

        Map<Integer, BigDecimal> orderAmountMap = createOrderCommand.getOrderAmounts().stream()
            .collect(Collectors.toMap(
                CreateOrderCommand.OrderAmount::getAmountType,
                CreateOrderCommand.OrderAmount::getAmount));
        data.setTotalAmount(orderAmountMap.get(AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode()));
        data.setPayAmount(orderAmountMap.get(AmountTypeEnum.REAL_PAY_AMOUNT.getCode()));
        List<CreateOrderCommand.OrderPayment> paymentRequestList = createOrderCommand.getOrderPayments();
        if (paymentRequestList != null && !paymentRequestList.isEmpty()) {
            data.setPayType(paymentRequestList.get(0).getPayType());
        }
        data.setCouponId(createOrderCommand.getCouponId());
        data.setPayTime(null);
        data.setExpireTime(new Date(currentTimeMillis + orderProperties.getExpireTime()));

        data.setCancelType(null);
        data.setCancelTime(null);

        data.setExtJson(null);

        data.setCreateBy(null);
        data.setUpdateBy(null);
        data.setCreateTime(null);
        data.setUpdateTime(null);

        this.orderData.setOrderInfo(data);
        return data;
    }

    private static int getOrderType(Integer productType) {
        ProductTypeEnum productTypeEnum = ProductTypeEnum.getByCode(productType);
        if (ProductTypeEnum.NORMAL.equals(productTypeEnum)) {
            return OrderTypeEnum.NORMAL.getCode();
        } else if (ProductTypeEnum.VIRTUAL.equals(productTypeEnum)) {
            return OrderTypeEnum.VIRTUAL.getCode();
        } else if (ProductTypeEnum.PRE_SALE.equals(productTypeEnum)) {
            return OrderTypeEnum.PRE_SALE.getCode();
        }
        return OrderTypeEnum.NORMAL.getCode();
    }

    /**
     * 构建OrderItemDO对象
     */
    private List<OrderItemDO> buildOrderItems() {
        String orderId = createOrderCommand.getOrderId();
        String sellerId = createOrderCommand.getSellerId();
        List<CreateOrderCommand.OrderItem> orderItems = createOrderCommand.getOrderItems();
        Map<String, Map<Integer, CalculateOrderAmountDTO.OrderItemAmountDTO>> orderItemAmountMap = calculateOrderAmountDTO.getOrderItemAmountList().stream()
            .collect(Collectors.groupingBy(CalculateOrderAmountDTO.OrderItemAmountDTO::getSkuCode,
                Collectors.toMap(CalculateOrderAmountDTO.OrderItemAmountDTO::getAmountType, Function.identity())));
        Map<String, ProductSkuDTO> productSkuMap = productSkuList.stream().collect(Collectors.toMap(ProductSkuDTO::getSkuCode, Function.identity()));

        List<OrderItemDO> list = new ArrayList<>();
        for (int i = 0; i < orderItems.size(); i++) {
            CreateOrderCommand.OrderItem orderItem = orderItems.get(i);
            String skuCode = orderItem.getSkuCode();
            ProductSkuDTO productSkuDTO = productSkuMap.get(skuCode);

            OrderItemDO data = new OrderItemDO();
            data.setOrderId(orderId);
            String orderItemId = orderId + "_" + String.format("%03d", i);
            data.setOrderItemId(orderItemId);
            data.setSellerId(sellerId);

            data.setProductType(productSkuDTO.getProductType());
            data.setProductId(productSkuDTO.getProductId());
            data.setProductImg(productSkuDTO.getProductImg());
            data.setProductName(productSkuDTO.getProductName());
            data.setProductUnit(productSkuDTO.getProductUnit());
            data.setSkuCode(productSkuDTO.getSkuCode());

            data.setSaleQuantity(orderItem.getSaleQuantity());
            data.setSalePrice(productSkuDTO.getSalePrice());
            data.setPurchasePrice(productSkuDTO.getPurchasePrice());
            BigDecimal originAmount = data.getSaleQuantity().multiply(data.getSalePrice());
            data.setOriginAmount(originAmount);
            // 商品项目实际支付金额，默认是originAmount，但是有优惠抵扣的时候需要分摊
            BigDecimal payAmount = Optional.of(orderItemAmountMap.get(skuCode))     // 获取当前sku的金额, 以AmountTypeEnum为key, 金额为value
                .map(m -> m.get(AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode()))   // 获取当前sku的优惠抵扣金额DTO
                .map(CalculateOrderAmountDTO.OrderItemAmountDTO::getAmount)         // 获取当前sku的优惠抵扣金额BigDecimal
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)  // 如果有优惠抵扣金额
                .map(a -> data.getOriginAmount().subtract(a))   // 就用原始金额减去优惠抵扣金额, 等于实付金额
                .orElse(data.getOriginAmount());                // 如果没有优惠抵扣金额, 就拿原始金额作为实付金额
            data.setPayAmount(payAmount);
            if (ProductTypeEnum.PRE_SALE.getCode().equals(data.getProductType())) {
                ProductSkuDTO productSku = productSkuMap.get(data.getSkuCode());
                // 将预售商品信息放入orderItem的extJson里面
                String mergeJSON = JSONHelper.mergeJSON(data.getExtJson(), JSONHelper.toString(productSku.getPreSaleInfo()));
                data.setExtJson(mergeJSON);
            }
            list.add(data);

        }

        this.orderData.setOrderItemList(list);
        return list;
    }

    /**
     * 构建OrderDeliveryDetailDO对象
     */
    private OrderDeliveryDetailDO buildOrderDeliveryDetail() {
        String province = createOrderCommand.getProvince();
        String city = createOrderCommand.getCity();
        String area = createOrderCommand.getArea();
        String street = createOrderCommand.getStreet();
        String detailAddress = createOrderCommand.getDetailAddress();
        // AddressGateway.queryAddress(
        String fullDetailAddress = Stream.of(province, city, area, street, detailAddress).filter(Objects::nonNull).collect(Collectors.joining(""));

        OrderDeliveryDetailDO data = new OrderDeliveryDetailDO();
        data.setOrderId(createOrderCommand.getOrderId());
        data.setDeliveryType(createOrderCommand.getDeliveryType());
        data.setProvince(createOrderCommand.getProvince());
        data.setCity(createOrderCommand.getCity());
        data.setArea(createOrderCommand.getArea());
        data.setStreet(createOrderCommand.getStreet());
        data.setDetailAddress(detailAddress);
        // data.setDetailAddress(fullDetailAddress);
        data.setLon(createOrderCommand.getLon());
        data.setLat(createOrderCommand.getLat());
        data.setReceiverName(createOrderCommand.getReceiverName());
        data.setReceiverPhone(createOrderCommand.getReceiverPhone());
        data.setModifyAddressCount(0);

        this.orderData.setOrderDeliveryDetail(data);
        return data;
    }

    /**
     * 构建OrderPaymentDetailDO对象
     */
    private List<OrderPaymentDetailDO> buildOrderPaymentDetail() {
        List<CreateOrderCommand.OrderPayment> orderPayments = createOrderCommand.getOrderPayments();
        List<OrderPaymentDetailDO> list = new ArrayList<>();
        for (CreateOrderCommand.OrderPayment orderPayment : orderPayments) {
            OrderPaymentDetailDO data = new OrderPaymentDetailDO();
            data.setOrderId(createOrderCommand.getOrderId());
            data.setAccountType(orderPayment.getAccountType());
            data.setPayType(orderPayment.getPayType());
            data.setPayStatus(PayStatusEnum.UNPAID.getCode());
            data.setPayAmount(orderPayment.getPayAmount());
            data.setPayTime(null);
            data.setOutTradeNo(null);
            data.setPayRemark(null);
            list.add(data);
        }

        this.orderData.setOrderPaymentDetailList(list);
        return list;
    }

    /**
     * 构建OrderAmountDO对象
     */
    private List<OrderAmountDO> buildOrderAmount() {
        List<CalculateOrderAmountDTO.OrderAmountDTO> orderAmountList = calculateOrderAmountDTO.getOrderAmountList();
        List<OrderAmountDO> list = new ArrayList<>();
        for (CalculateOrderAmountDTO.OrderAmountDTO orderAmountDTO : orderAmountList) {
            OrderAmountDO data = new OrderAmountDO();
            data.setOrderId(createOrderCommand.getOrderId());
            data.setAmountType(orderAmountDTO.getAmountType());
            data.setAmount(orderAmountDTO.getAmount());
            list.add(data);
        }

        this.orderData.setOrderAmountList(list);
        return list;
    }

    /**
     * 构建OrderAmountDetailDO对象
     */
    private List<OrderAmountDetailDO> buildOrderAmountDetail() {
        Map<String, OrderItemDO> orderItemMap = this.orderData.getOrderItemList()
            .stream().collect(Collectors.toMap(OrderItemDO::getSkuCode, Function.identity()));

        List<CalculateOrderAmountDTO.OrderItemAmountDTO> orderItemAmountList = calculateOrderAmountDTO.getOrderItemAmountList();
        List<OrderAmountDetailDO> list = new ArrayList<>();
        for (CalculateOrderAmountDTO.OrderItemAmountDTO orderAmountDetailDTO : orderItemAmountList) {
            String skuCode = orderAmountDetailDTO.getSkuCode();
            OrderItemDO orderItem = orderItemMap.get(skuCode);

            OrderAmountDetailDO data = new OrderAmountDetailDO();
            data.setOrderId(createOrderCommand.getOrderId());
            data.setProductType(orderItem.getProductType());
            data.setOrderItemId(orderItem.getOrderItemId());
            data.setProductId(orderItem.getProductId());
            data.setSkuCode(skuCode);

            data.setSaleQuantity(orderAmountDetailDTO.getSaleQuantity());
            data.setSalePrice(orderAmountDetailDTO.getSalePrice());
            data.setAmountType(orderAmountDetailDTO.getAmountType());
            data.setAmount(orderAmountDetailDTO.getAmount());
            list.add(data);
        }

        this.orderData.setOrderAmountDetailList(list);
        return list;
    }

    /**
     * 构建OrderOperateLogDO对象
     */
    private OrderOperateLogDO buildOperateLog() {
        OrderOperateLogDO data = new OrderOperateLogDO();
        data.setOrderId(createOrderCommand.getOrderId());
        data.setOperateType(OrderOperateTypeEnum.NEW_ORDER.getCode());
        data.setPreStatus(OrderStatusEnum.NULL.getCode());
        data.setCurrentStatus(OrderStatusEnum.CREATED.getCode());
        data.setRemark("创建订单操作0->10");

        this.orderData.setOrderOperateLog(data);
        return data;
    }

    /**
     * 构建OrderSnapshot对象
     */
    private List<OrderSnapshotDO> buildOrderSnapshot() {
        String orderId = createOrderCommand.getOrderId();
        String couponId = createOrderCommand.getCouponId();
        List<OrderAmountDO> orderAmountList = this.orderData.getOrderAmountList();
        List<OrderItemDO> orderItemList = this.orderData.getOrderItemList();
        UserCouponDTO userCouponDTO = calculateOrderAmountDTO.getUserCoupon();

        // 优惠券信息快照
        List<OrderSnapshotDO> list = new ArrayList<>();
        if (StringUtils.isNotEmpty(couponId)) {
            OrderSnapshotDO data1 = new OrderSnapshotDO();
            data1.setOrderId(orderId);
            data1.setSnapshotType(SnapshotTypeEnum.ORDER_COUPON.getCode());
            if (userCouponDTO != null) {
                data1.setSnapshotJson(JSONHelper.toString(userCouponDTO));
            } else {
                data1.setSnapshotJson("{\"couponId\": \"" + couponId + "\"}");
            }

            list.add(data1);
        }

        // 费用信息
        OrderSnapshotDO data2 = new OrderSnapshotDO();
        data2.setOrderId(orderId);
        data2.setSnapshotType(SnapshotTypeEnum.ORDER_AMOUNT.getCode());
        data2.setSnapshotJson(JSONHelper.toString(orderAmountList));
        list.add(data2);

        // 商品条目信息
        OrderSnapshotDO data3 = new OrderSnapshotDO();
        data3.setOrderId(orderId);
        data3.setSnapshotType(SnapshotTypeEnum.ORDER_ITEM.getCode());
        data3.setSnapshotJson(JSONHelper.toString(orderItemList));
        list.add(data3);

        this.orderData.setOrderSnapshotList(list);
        return list;
    }

    public OrderData build() {
        Optional.ofNullable(orderData.getOrderInfo()).orElseGet(this::buildOrderInfo);
        Optional.ofNullable(orderData.getOrderItemList()).orElseGet(this::buildOrderItems);
        Optional.ofNullable(orderData.getOrderDeliveryDetail()).orElseGet(this::buildOrderDeliveryDetail);
        Optional.ofNullable(orderData.getOrderPaymentDetailList()).orElseGet(this::buildOrderPaymentDetail);
        Optional.ofNullable(orderData.getOrderAmountList()).orElseGet(this::buildOrderAmount);
        Optional.ofNullable(orderData.getOrderAmountDetailList()).orElseGet(this::buildOrderAmountDetail);
        Optional.ofNullable(orderData.getOrderOperateLog()).orElseGet(this::buildOperateLog);
        Optional.ofNullable(orderData.getOrderSnapshotList()).orElseGet(this::buildOrderSnapshot);
        return orderData;
    }

    @Data
    public static class OrderData {
        // 订单信息
        private OrderInfoDO orderInfo;
        // 订单条目
        private List<OrderItemDO> orderItemList;
        // 订单配送信息
        private OrderDeliveryDetailDO orderDeliveryDetail;
        // 订单支付信息
        private List<OrderPaymentDetailDO> orderPaymentDetailList;
        // 订单费用信息
        private List<OrderAmountDO> orderAmountList;
        // 订单费用明细
        private List<OrderAmountDetailDO> orderAmountDetailList;
        // 订单状态变更日志信息
        private OrderOperateLogDO orderOperateLog;
        // 订单快照数据
        private List<OrderSnapshotDO> orderSnapshotList;

        public List<OrderData> split(Function<List<OrderItemDO>, List<List<OrderItemDO>>> splitter, GenOrderIdAppService genOrderIdAppService) {
            String orderId = orderInfo.getOrderId();
            String userId = orderInfo.getUserId();
            // 根据拆单规则, 拆分子订单条目
            List<List<OrderItemDO>> splitOrderItemList = splitter.apply(this.orderItemList);
            if (splitOrderItemList.size() <= 1) {
                // 无需拆单
                return Collections.emptyList();
            }
            Map<Integer, List<OrderItemDO>> orderItemMap = orderItemList.stream()
                .collect(Collectors.groupingBy(OrderItemDO::getProductType));

            // 对拆分后的子订单条目, 生成相关的子订单数据
            List<OrderData> subOrderDataList = new ArrayList<>();
            for (List<OrderItemDO> splitOrderItem : splitOrderItemList) {
                Map<String, OrderItemDO> subOrderItemMap = splitOrderItem.stream()
                    .collect(Collectors.toMap(OrderItemDO::getOrderItemId, Function.identity()));

                OrderData subOrderData = new OrderData();
                subOrderDataList.add(subOrderData);
                // 生成新的子订单的订单号
                String subOrderNo = this.getSubOrderId(genOrderIdAppService);

                // 统计子订单总金额
                BigDecimal subTotalAmount = BigDecimal.ZERO;
                BigDecimal subRealPayAmount = BigDecimal.ZERO;
                // 子订单的订单条目
                List<OrderItemDO> subOrderItemList = new ArrayList<>();
                for (OrderItemDO orderItem : splitOrderItem) {
                    String subOrderItemNo = this.getSubOrderItemId(orderItem.getOrderItemId(), subOrderNo);
                    OrderItemDO subOrderItem = new OrderItemDO(orderItem);
                    subOrderItem.setId(null);
                    subOrderItem.setOrderId(subOrderNo);
                    subOrderItem.setOrderItemId(subOrderItemNo);
                    // 子订单条目的金额不需要变动
                    subOrderItemList.add(subOrderItem);

                    // 对子订单的订单条目求和, 作为子订单的订单费用存储
                    subTotalAmount = subTotalAmount.add(orderItem.getOriginAmount());
                    subRealPayAmount = subRealPayAmount.add(orderItem.getPayAmount());
                }
                subOrderData.setOrderItemList(subOrderItemList);

                // 子订单的详细信息
                OrderInfoDO subOrderInfo = new OrderInfoDO(orderInfo);
                subOrderInfo.setId(null);
                subOrderInfo.setOrderId(subOrderNo);
                subOrderInfo.setParentOrderId(orderId);
                if(orderItemMap.keySet().size() <= 1) {
                    subOrderInfo.setOrderType(getOrderType(orderItemList.get(0).getProductType()));
                } else {
                    subOrderInfo.setOrderType(getOrderType(OrderTypeEnum.NORMAL.getCode()));
                }
                subOrderInfo.setOrderStatus(OrderStatusEnum.INVALID.getCode());
                subOrderInfo.setTotalAmount(subTotalAmount);
                subOrderInfo.setPayAmount(subRealPayAmount);
                subOrderData.setOrderInfo(subOrderInfo);

                // 子订单的配送地址信息
                OrderDeliveryDetailDO subOrderDeliveryDetail = new OrderDeliveryDetailDO(orderDeliveryDetail);
                subOrderDeliveryDetail.setId(null);
                subOrderDeliveryDetail.setOrderId(subOrderNo);
                subOrderData.setOrderDeliveryDetail(subOrderDeliveryDetail);

                // 统计子订单的费用信息
                BigDecimal subTotalOriginPayAmount = BigDecimal.ZERO;
                BigDecimal subTotalDiscountAmount = BigDecimal.ZERO;
                BigDecimal subTotalRealPayAmount = BigDecimal.ZERO;
                // 子订单的费用明细
                List<OrderAmountDetailDO> subOrderAmountDetailList = new ArrayList<>();
                for (OrderAmountDetailDO orderAmountDetail : orderAmountDetailList) {
                    String orderItemNo = orderAmountDetail.getOrderItemId();
                    if (!subOrderItemMap.containsKey(orderItemNo)) {
                        // 过滤主订单费用明细, 不在本次拆单范围内的就不处理
                        continue;
                    }
                    String subOrderItemNo = this.getSubOrderItemId(orderItemNo, subOrderNo);
                    OrderAmountDetailDO subOrderAmountDetail = new OrderAmountDetailDO(orderAmountDetail);
                    subOrderAmountDetail.setId(null);
                    subOrderAmountDetail.setOrderId(subOrderNo);
                    subOrderAmountDetail.setOrderItemId(subOrderItemNo);
                    // 子订单的费用明细的金额不需要变动
                    subOrderAmountDetailList.add(subOrderAmountDetail);

                    // 对过滤后的订单费用明细求和, 后面作为子订单费用存储
                    Integer amountType = orderAmountDetail.getAmountType();
                    BigDecimal amount = orderAmountDetail.getAmount();
                    if (AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode().equals(amountType)) {
                        subTotalOriginPayAmount = subTotalOriginPayAmount.add(amount);
                    } else if (AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode().equals(amountType)) {
                        subTotalDiscountAmount = subTotalDiscountAmount.add(amount);
                    } else if (AmountTypeEnum.REAL_PAY_AMOUNT.getCode().equals(amountType)) {
                        subTotalRealPayAmount = subTotalRealPayAmount.add(amount);
                    }
                }
                subOrderData.setOrderAmountDetailList(subOrderAmountDetailList);

                // 子订单的费用信息
                List<OrderAmountDO> subOrderAmountList = new ArrayList<>();
                for (OrderAmountDO orderAmountDO : orderAmountList) {
                    Integer amountType = orderAmountDO.getAmountType();
                    OrderAmountDO subOrderAmount = new OrderAmountDO(orderAmountDO);
                    subOrderAmount.setId(null);
                    subOrderAmount.setOrderId(subOrderNo);
                    if (AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode().equals(amountType)) {
                        subOrderAmount.setAmount(subTotalOriginPayAmount);
                        subOrderAmountList.add(subOrderAmount);
                    } else if (AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode().equals(amountType)) {
                        subOrderAmount.setAmount(subTotalDiscountAmount);
                        subOrderAmountList.add(subOrderAmount);
                    } else if (AmountTypeEnum.REAL_PAY_AMOUNT.getCode().equals(amountType)) {
                        subOrderAmount.setAmount(subTotalRealPayAmount);
                        subOrderAmountList.add(subOrderAmount);
                    }
                }
                subOrderData.setOrderAmountList(subOrderAmountList);

                // 订单支付信息
                List<OrderPaymentDetailDO> subOrderPaymentDetailList = new ArrayList<>();
                for (OrderPaymentDetailDO orderPaymentDetail : orderPaymentDetailList) {
                    OrderPaymentDetailDO subOrderPaymentDetail = new OrderPaymentDetailDO(orderPaymentDetail);
                    subOrderPaymentDetail.setId(null);
                    subOrderPaymentDetail.setOrderId(subOrderNo);
                    subOrderPaymentDetail.setPayAmount(subTotalRealPayAmount);
                    subOrderPaymentDetailList.add(subOrderPaymentDetail);
                }
                subOrderData.setOrderPaymentDetailList(subOrderPaymentDetailList);

                // 订单状态变更日志信息
                OrderOperateLogDO subOrderOperateLog = new OrderOperateLogDO(orderOperateLog);
                subOrderOperateLog.setId(null);
                subOrderOperateLog.setOrderId(subOrderNo);
                subOrderOperateLog.setCurrentStatus(OrderStatusEnum.INVALID.getCode());
                subOrderOperateLog.setRemark("创建订单操作0->127");
                subOrderData.setOrderOperateLog(subOrderOperateLog);

                // 订单商品快照信息
                List<OrderSnapshotDO> subOrderSnapshotList = new ArrayList<>();
                for (OrderSnapshotDO orderSnapshot : orderSnapshotList) {
                    OrderSnapshotDO subOrderSnapshot = new OrderSnapshotDO(orderSnapshot);
                    subOrderSnapshot.setId(null);
                    subOrderSnapshot.setOrderId(subOrderNo);
                    if (SnapshotTypeEnum.ORDER_AMOUNT.getCode().equals(orderSnapshot.getSnapshotType())) {
                        subOrderSnapshot.setSnapshotJson(JSONHelper.toString(subOrderAmountList));
                    } else if (SnapshotTypeEnum.ORDER_ITEM.getCode().equals(orderSnapshot.getSnapshotType())) {
                        subOrderSnapshot.setSnapshotJson(JSONHelper.toString(splitOrderItem));
                    }
                    subOrderSnapshotList.add(subOrderSnapshot);
                }
                subOrderData.setOrderSnapshotList(subOrderSnapshotList);
            }
            return subOrderDataList;
        }

        private String getSubOrderId(GenOrderIdAppService genOrderIdAppService) {
            GenOrderIdCommand command = new GenOrderIdCommand();
            command.setUserId(orderInfo.getUserId());
            command.setOrderIdType(orderInfo.getOrderType());
            String orderId = genOrderIdAppService.generate(command);
            return orderId;
        }

        private String getSubOrderItemId(String orderItemId, String subOrderId) {
            String suffix = orderItemId.substring(orderItemId.indexOf("_"));
            return subOrderId + suffix;
        }
    }
}
