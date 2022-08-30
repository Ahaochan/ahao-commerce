package com.ruyuan.eshop.order.utils.mock;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.ruyuan.eshop.address.domain.dto.AddressDTO;
import com.ruyuan.eshop.address.domain.query.AddressQuery;
import com.ruyuan.eshop.common.enums.*;
import com.ruyuan.eshop.common.utils.JsonUtil;
import com.ruyuan.eshop.common.utils.RandomUtil;
import com.ruyuan.eshop.market.domain.dto.CalculateOrderAmountDTO;
import com.ruyuan.eshop.market.domain.request.CalculateOrderAmountRequest;
import com.ruyuan.eshop.order.builder.FullOrderData;
import com.ruyuan.eshop.order.builder.NewOrderBuilder;
import com.ruyuan.eshop.order.config.OrderProperties;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.*;
import com.ruyuan.eshop.order.domain.dto.GenOrderIdDTO;
import com.ruyuan.eshop.order.domain.entity.*;
import com.ruyuan.eshop.order.domain.request.CreateOrderRequest;
import com.ruyuan.eshop.order.domain.request.GenOrderIdRequest;
import com.ruyuan.eshop.order.enums.AccountTypeEnum;
import com.ruyuan.eshop.order.enums.SnapshotTypeEnum;
import com.ruyuan.eshop.order.service.OrderService;
import com.ruyuan.eshop.order.service.impl.NewOrderDataHolder;
import com.ruyuan.eshop.order.service.impl.OrderOperateLogFactory;
import com.ruyuan.eshop.order.statemachine.action.order.create.node.CreateOrderMasterBuilderNode;
import com.ruyuan.eshop.product.domain.dto.ProductSkuDTO;
import com.ruyuan.eshop.product.enums.ProductTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 准备order全量信息准备utils
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
public class PrepareOrderFullDataUtils {

    @Autowired
    private OrderConverter orderConverter;

    @Autowired
    private OrderProperties orderProperties;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CreateOrderMasterBuilderNode createOrderMasterBuilderNode;

    @Autowired
    private OrderOperateLogFactory orderOperateLogFactory;

    private static int partitionSize = 100;

    //订单状态
    private static Integer[] orderStatus = {10, 20, 30, 40, 50, 60, 70};

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private OrderAmountDAO orderAmountDAO;

    @Autowired
    private OrderAmountDetailDAO orderAmountDetailDAO;

    @Autowired
    private OrderOperateLogDAO orderOperateLogDAO;

    @Autowired
    private OrderSnapshotDAO orderSnapshotDAO;


    /**
     * 创建订单请求模板
     */
    private static String CREATE_ORDER_PARAM_TEMPLATE = "{\n" +
            "    \"orderId\":1022011295805961100,\n" +
            "    \"businessIdentifier\":1,\n" +
            "    \"openid\":null,\n" +
            "    \"userId\":100,\n" +
            "    \"orderType\":1,\n" +
            "    \"sellerId\":101,\n" +
            "    \"userRemark\":\"test reamark\",\n" +
            "    \"deliveryType\":1,\n" +
            "    \"province\":\"110000\",\n" +
            "    \"city\":\"110100\",\n" +
            "    \"area\":\"110105\",\n" +
            "    \"street\":\"110101007\",\n" +
            "    \"detailAddress\":\"北京路10号\",\n" +
            "    \"lon\":100.10000,\n" +
            "    \"lat\":1010.201010,\n" +
            "    \"receiverName\":\"张三\",\n" +
            "    \"receiverPhone\":\"13434545545\",\n" +
            "    \"userAddressId\":\"1010\",\n" +
            "    \"addressCode\":\"1010100\",\n" +
            "    \"regionId\":\"10002020\",\n" +
            "    \"shippingAreaId\":\"101010212\",\n" +
            "    \"clientIp\":\"34.53.12.34\",\n" +
            "    \"deviceId\":\"45sf2354adfw245\",\n" +
            "    \"orderItemRequestList\":[\n" +
            "        {\n" +
            "            \"productType\":1,\n" +
            "            \"saleQuantity\":10,\n" +
            "            \"skuCode\":\"skuCode001\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"orderAmountRequestList\":[\n" +
            "        {\n" +
            "            \"amountType\":10,\n" +
            "            \"amount\":10000\n" +
            "        },\n" +
            "        {\n" +
            "            \"amountType\":30,\n" +
            "            \"amount\":0\n" +
            "        },\n" +
            "        {\n" +
            "            \"amountType\":50,\n" +
            "            \"amount\":10000\n" +
            "        }\n" +
            "    ],\n" +
            "    \"paymentRequestList\":[\n" +
            "        {\n" +
            "            \"payType\":10,\n" +
            "            \"accountType\":1,\n" +
            "            \"payAmount\":10000\n" +
            "        }\n" +
            "    ]\n" +
            "}";


    /**
     * mock 普通订单全量信息
     *
     * @param
     * @return
     */
    public FullOrderData genFullMasterOrderData() {
        PrepareOrderFullDataUtils.MockUserInfo mockUserInfo = genMockUser();
        return mockFullMasterOrderData(mockUserInfo);
    }
    /**
     * mock 订单全量信息
     *
     * @param mockUserInfo
     * @return
     */
    public FullOrderData mockFullMasterOrderData(MockUserInfo mockUserInfo) {

        // 1、mock创建订单请求入参
        CreateOrderRequest mockCreateOrderRequest = mockCreateOrderRequest(mockUserInfo);

        // 2、mock ProductSkuDTO
        List<ProductSkuDTO> productSkus = mockProductSkuList(mockCreateOrderRequest.getOrderItemRequestList());
        // 对sku按照商品类型进行分组
        Map<Integer, List<ProductSkuDTO>> productTypeMap = productSkus.stream()
                .collect(Collectors.groupingBy(ProductSkuDTO::getProductType));

        // 3、mock CalculateOrderAmountDTO
        CalculateOrderAmountDTO calculateOrderAmountDTO = mockCalculateOrderAmountDTO(mockCreateOrderRequest, productSkus);

        // 4、生成主订单
        FullOrderData fullMasterOrderData = addNewMasterOrder(mockCreateOrderRequest, productSkus, productTypeMap,
                calculateOrderAmountDTO);

        // 5、随机化订单状态
        randomOrderStatus(fullMasterOrderData);

        return fullMasterOrderData;
    }

    /**
     * 批量插入数据库
     *
     * @param fullOrderDataList
     */
    public void batchInsertOrderFullData(List<FullOrderData> fullOrderDataList) {
        List<List<FullOrderData>> lists = Lists.partition(fullOrderDataList, partitionSize);
        NewOrderDataHolder newOrderDataHolder = new NewOrderDataHolder();

        for (List<FullOrderData> list : lists) {

            for (FullOrderData fullOrderData : list) {
                newOrderDataHolder.appendOrderData(fullOrderData);
            }

            // 订单信息
            List<OrderInfoDO> orderInfoDOList = newOrderDataHolder.getOrderInfoDOList();
            if (!orderInfoDOList.isEmpty()) {
                orderInfoDAO.saveBatch(orderInfoDOList);
            }
            Map<String,OrderInfoDO> orderInfoMap = orderInfoDOList.stream().collect(Collectors.toMap(OrderInfoDO::getOrderId,o->o));

            // 订单条目
            List<OrderItemDO> orderItemDOList = newOrderDataHolder.getOrderItemDOList();
            if (!orderItemDOList.isEmpty()) {
                orderItemDAO.saveBatch(orderItemDOList);
            }

            // 订单配送信息
            List<OrderDeliveryDetailDO> orderDeliveryDetailDOList = newOrderDataHolder.getOrderDeliveryDetailDOList();
            if (!orderDeliveryDetailDOList.isEmpty()) {
                orderDeliveryDetailDAO.saveBatch(orderDeliveryDetailDOList);
            }

            // 订单支付信息
            List<OrderPaymentDetailDO> orderPaymentDetailDOList = newOrderDataHolder.getOrderPaymentDetailDOList();
            if (!orderPaymentDetailDOList.isEmpty()) {
                orderPaymentDetailDAO.saveBatch(orderPaymentDetailDOList);
            }

            // 订单费用信息
            List<OrderAmountDO> orderAmountDOList = newOrderDataHolder.getOrderAmountDOList();
            if (!orderAmountDOList.isEmpty()) {
                orderAmountDAO.saveBatch(orderAmountDOList);
            }

            // 订单费用明细
            List<OrderAmountDetailDO> orderAmountDetailDOList = newOrderDataHolder.getOrderAmountDetailDOList();
            if (!orderAmountDetailDOList.isEmpty()) {
                orderAmountDetailDAO.saveBatch(orderAmountDetailDOList);
            }

            // 订单状态变更日志信息
            List<OrderOperateLogDO> orderOperateLogDOList = newOrderDataHolder.getOrderOperateLogDOList();
            if (!orderOperateLogDOList.isEmpty()) {
                orderOperateLogDAO.batchSave(orderOperateLogDOList);
            }
            // 订单快照数据
            List<OrderSnapshotDO> orderSnapshotDOList = newOrderDataHolder.getOrderSnapshotDOList();
            if (!orderSnapshotDOList.isEmpty()) {
                Map<String,List<OrderSnapshotDO>> orderSnapshotMapByOrderId = orderSnapshotDOList.stream().collect(Collectors.groupingBy(OrderSnapshotDO::getOrderId));
                for(Map.Entry<String,List<OrderSnapshotDO>> entry : orderSnapshotMapByOrderId.entrySet()) {
                    OrderInfoDO orderInfoDO = orderInfoMap.get(entry.getKey());
                    List<OrderSnapshotDO> orderSnapshots = entry.getValue();
                    orderSnapshotDAO.batchSave(orderSnapshots,OrderSnapshotDAOUtils.getRowKeyPrefixList(orderInfoDO));
                }
            }
        }
    }


    /**
     * 随机化订单状态
     *
     * @param fullOrderData
     */
    private void randomOrderStatus(FullOrderData fullOrderData) {
        OrderInfoDO order = fullOrderData.getOrderInfoDO();
        Integer status = CommonUtils.getStatus(Arrays.asList(orderStatus));
        fullOrderData.getOrderInfoDO().setOrderStatus(status);
        List<OrderOperateLogDO> orderOperateLogs = new ArrayList<>();
        // 添加订单创建的操作日志
        orderOperateLogs.add(orderConverter.copyOrderOperationLogDO(fullOrderData.getOrderOperateLogDO()));

        OrderStatusEnum orderStatus = OrderStatusEnum.getByCode(status);
        if (OrderStatusEnum.CREATED.equals(orderStatus)) {
            // do nothing
        } else if (OrderStatusEnum.PAID.equals(orderStatus)) {
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_PAID));
        } else if (OrderStatusEnum.FULFILL.equals(orderStatus)) {
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_PAID));
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_FULFILLED));
            setOrderPaid(fullOrderData);
        } else if (OrderStatusEnum.OUT_STOCK.equals(orderStatus)) {
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_PAID));
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_FULFILLED));
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_OUT_STOCKED));
            setOrderPaid(fullOrderData);
        } else if (OrderStatusEnum.DELIVERY.equals(orderStatus)) {
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_PAID));
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_FULFILLED));
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_OUT_STOCKED));
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_DELIVERED));
            setOrderPaid(fullOrderData);
        } else if (OrderStatusEnum.SIGNED.equals(orderStatus)) {
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_PAID));
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_FULFILLED));
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_OUT_STOCKED));
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_DELIVERED));
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_SIGNED));
            setOrderPaid(fullOrderData);
        } else if (OrderStatusEnum.CANCELLED.equals(orderStatus)) {
            orderOperateLogs.add(orderOperateLogFactory.get(order, OrderStatusChangeEnum.ORDER_UN_PAID_MANUAL_CANCELLED));
            fullOrderData.getOrderInfoDO().setCancelTime(new Date());
            fullOrderData.getOrderInfoDO().setCancelType("1");
        }

        fullOrderData.setTestOrderOperateLogDOs(orderOperateLogs);
    }

    /**
     * 设置订单已支付
     *
     * @param fullOrderData
     */
    private void setOrderPaid(FullOrderData fullOrderData) {
        fullOrderData.getOrderInfoDO().setPayTime(new Date());
        fullOrderData.getOrderPaymentDetailDOList().forEach(
                payment -> {
                    payment.setPayTime(new Date());
                    payment.setOutTradeNo(RandomUtil.genRandomNumber(10));
                }
        );
    }

    /**
     * mock CalculateOrderAmountDTO
     *
     * @param mockCreateOrderRequest
     * @param productSkus
     * @return
     */
    private CalculateOrderAmountDTO mockCalculateOrderAmountDTO(CreateOrderRequest mockCreateOrderRequest, List<ProductSkuDTO> productSkus) {

        CalculateOrderAmountRequest calculateOrderPriceRequest = orderConverter.convertToCalculateOrderAmountRequest(mockCreateOrderRequest);

        // 订单条目补充商品信息
        Map<String, ProductSkuDTO> productSkuDTOMap = productSkus.stream().collect(Collectors.toMap(ProductSkuDTO::getSkuCode, Function.identity()));
        calculateOrderPriceRequest.getOrderItemRequestList().forEach(item -> {
            String skuCode = item.getSkuCode();
            ProductSkuDTO productSkuDTO = productSkuDTOMap.get(skuCode);
            item.setProductId(productSkuDTO.getProductId());
            item.setSalePrice(productSkuDTO.getSalePrice());
        });

        // 计算订单金额
        CalculateOrderAmountDTO calculateOrderAmountDTO = calculateOrderAmount(calculateOrderPriceRequest);
        return calculateOrderAmountDTO;
    }

    /**
     * 计算订单金额
     *
     * @param calculateOrderAmountRequest
     * @return
     */
    private CalculateOrderAmountDTO calculateOrderAmount(CalculateOrderAmountRequest calculateOrderAmountRequest) {

        String orderId = calculateOrderAmountRequest.getOrderId();

        // 原订单费用信息
        List<CalculateOrderAmountDTO.OrderAmountDTO> orderAmountList = orderConverter.convertOrderAmountRequest(calculateOrderAmountRequest.getOrderAmountRequestList());
        for (CalculateOrderAmountDTO.OrderAmountDTO orderAmountDTO : orderAmountList) {
            orderAmountDTO.setOrderId(orderId);
        }

        // 订单条目费用信息
        List<CalculateOrderAmountDTO.OrderAmountDetailDTO> orderAmountDetailDTOList = new ArrayList<>();
        List<CalculateOrderAmountRequest.OrderItemRequest> orderItemRequestList =
                calculateOrderAmountRequest.getOrderItemRequestList();

        // 先统计全部商品费用
        int totalProductAmount = 0;
        for (CalculateOrderAmountRequest.OrderItemRequest orderItemRequest : orderItemRequestList) {
            totalProductAmount += orderItemRequest.getSalePrice() * orderItemRequest.getSaleQuantity();
        }

        int index = 0;
        int totalNum = orderItemRequestList.size();
        Integer notLastItemTotalDiscountAmount = 0;
        for (CalculateOrderAmountRequest.OrderItemRequest orderItemRequest : orderItemRequestList) {
            // 订单条目支付原价
            CalculateOrderAmountDTO.OrderAmountDetailDTO originPayAmountDetail =
                    createOrderAmountDetailDTO(orderId,
                            AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode(),
                            null,
                            null,
                            orderItemRequest);
            orderAmountDetailDTOList.add(originPayAmountDetail);

            // 优惠券抵扣金额
            Integer discountAmount = 0;

            // 优惠券抵扣金额
            CalculateOrderAmountDTO.OrderAmountDetailDTO couponDiscountAmountDetail = null;
            if (discountAmount > 0) {
                if (++index < totalNum) {
                    // 订单条目分摊的优惠金额
                    double partDiscountAmount = Integer.valueOf(discountAmount
                            * orderItemRequest.getSalePrice() * orderItemRequest.getSaleQuantity()).doubleValue()
                            / Integer.valueOf(totalProductAmount).doubleValue();

                    // 遇到小数则向上取整
                    double curDiscountAmount = Math.ceil(partDiscountAmount);
                    couponDiscountAmountDetail =
                            createOrderAmountDetailDTO(orderId,
                                    AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode(),
                                    Double.valueOf(curDiscountAmount).intValue(),
                                    null,
                                    orderItemRequest);

                    notLastItemTotalDiscountAmount += couponDiscountAmountDetail.getAmount();
                } else {
                    // 最后一条item的优惠金额等于总优惠金额-前面所有item分摊的优惠总额
                    couponDiscountAmountDetail =
                            createOrderAmountDetailDTO(orderId,
                                    AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode(),
                                    discountAmount - notLastItemTotalDiscountAmount,
                                    null,
                                    orderItemRequest);
                }
                orderAmountDetailDTOList.add(couponDiscountAmountDetail);
            }

            // 实付金额
            Integer realPayAmount = originPayAmountDetail.getAmount();
            if (couponDiscountAmountDetail != null) {
                realPayAmount = realPayAmount - couponDiscountAmountDetail.getAmount();
            }
            CalculateOrderAmountDTO.OrderAmountDetailDTO realPayAmountDetail =
                    createOrderAmountDetailDTO(orderId,
                            AmountTypeEnum.REAL_PAY_AMOUNT.getCode(),
                            null,
                            realPayAmount,
                            orderItemRequest);
            orderAmountDetailDTOList.add(realPayAmountDetail);
        }

        // 重新计算订单支付原价、优惠券抵扣金额、实付金额
        Integer totalOriginPayAmount = 0;
        Integer totalDiscountAmount = 0;
        Integer totalRealPayAmount = 0;
        for (CalculateOrderAmountDTO.OrderAmountDetailDTO orderAmountDetailDTO : orderAmountDetailDTOList) {
            Integer amountType = orderAmountDetailDTO.getAmountType();
            Integer amount = orderAmountDetailDTO.getAmount();
            if (AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode().equals(amountType)) {
                totalOriginPayAmount += amount;
            } else if (AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode().equals(amountType)) {
                totalDiscountAmount += amount;
            } else if (AmountTypeEnum.REAL_PAY_AMOUNT.getCode().equals(amountType)) {
                totalRealPayAmount += amount;
            }
        }

        // 总的实付金额还要加上运费
        Map<Integer, CalculateOrderAmountDTO.OrderAmountDTO> orderAmountMap =
                orderAmountList.stream().collect(Collectors.toMap(
                        CalculateOrderAmountDTO.OrderAmountDTO::getAmountType, Function.identity()));

        for (CalculateOrderAmountDTO.OrderAmountDTO orderAmountDTO : orderAmountList) {
            Integer amountType = orderAmountDTO.getAmountType();
            if (AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode().equals(amountType)) {
                orderAmountDTO.setAmount(totalOriginPayAmount);
            } else if (AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode().equals(amountType)) {
                orderAmountDTO.setAmount(totalDiscountAmount);
            } else if (AmountTypeEnum.REAL_PAY_AMOUNT.getCode().equals(amountType)) {
                orderAmountDTO.setAmount(totalRealPayAmount);
            }
        }

        CalculateOrderAmountDTO calculateOrderAmountDTO = new CalculateOrderAmountDTO();
        calculateOrderAmountDTO.setOrderAmountList(orderAmountList);
        calculateOrderAmountDTO.setOrderAmountDetail(orderAmountDetailDTOList);

        return calculateOrderAmountDTO;
    }

    private CalculateOrderAmountDTO.OrderAmountDetailDTO createOrderAmountDetailDTO(
            String orderId,
            Integer amountType,
            Integer discountAmount,
            Integer realPayAmount,
            CalculateOrderAmountRequest.OrderItemRequest orderItemRequest) {
        CalculateOrderAmountDTO.OrderAmountDetailDTO orderAmountDetailDTO =
                new CalculateOrderAmountDTO.OrderAmountDetailDTO();
        orderAmountDetailDTO.setOrderId(orderId);
        orderAmountDetailDTO.setProductType(orderItemRequest.getProductType());
        orderAmountDetailDTO.setSkuCode(orderItemRequest.getSkuCode());
        orderAmountDetailDTO.setSaleQuantity(orderItemRequest.getSaleQuantity());
        orderAmountDetailDTO.setSalePrice(orderItemRequest.getSalePrice());
        orderAmountDetailDTO.setAmountType(amountType);
        if (AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode().equals(amountType)) {
            orderAmountDetailDTO.setAmount(orderAmountDetailDTO.getSaleQuantity()
                    * orderAmountDetailDTO.getSalePrice());
        } else if (AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode().equals(amountType)) {
            orderAmountDetailDTO.setAmount(discountAmount);
        } else if (AmountTypeEnum.REAL_PAY_AMOUNT.getCode().equals(amountType)) {
            orderAmountDetailDTO.setAmount(realPayAmount);
        }
        return orderAmountDetailDTO;
    }

    /**
     * mock ProductSkuDTO
     *
     * @param orderItemRequests
     * @return
     */
    private List<ProductSkuDTO> mockProductSkuList(List<CreateOrderRequest.OrderItemRequest> orderItemRequests) {
        List<ProductSkuDTO> productSkus = new ArrayList<>(orderItemRequests.size());

        for (CreateOrderRequest.OrderItemRequest item : orderItemRequests) {
            String no = cutOutSkuCodeNo(item.getSkuCode());
            ProductSkuDTO productSku = new ProductSkuDTO();
            productSku.setProductId(item.getSkuCode());
            productSku.setProductImg("img_" + no);
            productSku.setSkuCode(item.getSkuCode());
            productSku.setProductName("普通商品_" + no);
            productSku.setProductType(item.getProductType());
            productSku.setProductUnit("个");
            productSku.setPurchasePrice(1000);
            productSku.setSalePrice(1000);

            productSkus.add(productSku);
        }

        return productSkus;
    }


    /**
     * mock 创建订单请求
     */
    private CreateOrderRequest mockCreateOrderRequest(MockUserInfo mockUserInfo) {
        CreateOrderRequest createOrderRequest = JSONObject.parseObject(CREATE_ORDER_PARAM_TEMPLATE, CreateOrderRequest.class);
        createOrderRequest.setOrderId(genOrderId(mockUserInfo.userId));
        createOrderRequest.setUserId(mockUserInfo.userId);
        createOrderRequest.setBusinessIdentifier(BusinessIdentifierEnum.PRESSURE.getCode());
        createOrderRequest.setOrderType(OrderTypeEnum.NORMAL.getCode());
        createOrderRequest.setReceiverName(mockUserInfo.receiverName);

        List<CreateOrderRequest.OrderItemRequest> orderItemRequests = mockOrderItemRequestList();
        createOrderRequest.setOrderItemRequestList(orderItemRequests);

        List<CreateOrderRequest.OrderAmountRequest> orderAmountRequests = mockOrderAmountRequestList(orderItemRequests);
        createOrderRequest.setOrderAmountRequestList(orderAmountRequests);
        createOrderRequest.setPaymentRequestList(mockOrderPaymentRequestList(orderAmountRequests));

        return createOrderRequest;
    }

    /**
     * mock 订单条目信息
     */
    private List<CreateOrderRequest.OrderItemRequest> mockOrderItemRequestList() {
        Integer orderItemNum = genOrderItemNum();

        List<CreateOrderRequest.OrderItemRequest> orderItemRequests = new ArrayList<>();
        Set<String> skuCodeSet = new HashSet<>();
        for (int i = 0; i < orderItemNum; i++) {
            skuCodeSet.add(genSkuCode());
        }

        for (String skuCode : skuCodeSet) {
            CreateOrderRequest.OrderItemRequest orderItemRequest = new CreateOrderRequest.OrderItemRequest();
            orderItemRequest.setProductType(ProductTypeEnum.NORMAL.getCode());
            orderItemRequest.setSaleQuantity(genSaleQuantity());
            orderItemRequest.setSkuCode(skuCode);
            orderItemRequests.add(orderItemRequest);
        }

        return orderItemRequests;
    }

    /**
     * mock 订单费用信息
     */
    private List<CreateOrderRequest.OrderAmountRequest> mockOrderAmountRequestList(List<CreateOrderRequest.OrderItemRequest> orderItemRequests) {

        List<CreateOrderRequest.OrderAmountRequest> orderAmountRequests = new ArrayList<>();

        int totalAmount = 0;

        for (CreateOrderRequest.OrderItemRequest item : orderItemRequests) {
            totalAmount += item.getSaleQuantity() * 1000;
        }

        CreateOrderRequest.OrderAmountRequest orderAmountRequest1 = new CreateOrderRequest.OrderAmountRequest();
        orderAmountRequest1.setAmount(totalAmount);
        orderAmountRequest1.setAmountType(AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode());
        orderAmountRequests.add(orderAmountRequest1);

        CreateOrderRequest.OrderAmountRequest orderAmountRequest2 = new CreateOrderRequest.OrderAmountRequest();
        orderAmountRequest2.setAmount(0);
        orderAmountRequest2.setAmountType(AmountTypeEnum.SHIPPING_AMOUNT.getCode());
        orderAmountRequests.add(orderAmountRequest2);


        CreateOrderRequest.OrderAmountRequest orderAmountRequest3 = new CreateOrderRequest.OrderAmountRequest();
        orderAmountRequest3.setAmount(totalAmount);
        orderAmountRequest3.setAmountType(AmountTypeEnum.REAL_PAY_AMOUNT.getCode());
        orderAmountRequests.add(orderAmountRequest3);

        return orderAmountRequests;
    }

    /**
     * 设置订单支付信息
     *
     * @return
     */
    private List<CreateOrderRequest.PaymentRequest> mockOrderPaymentRequestList(List<CreateOrderRequest.OrderAmountRequest> orderAmountRequests) {

        // 实付金额
        CreateOrderRequest.OrderAmountRequest orderAmount = orderAmountRequests.stream()
                .filter(amount -> AmountTypeEnum.REAL_PAY_AMOUNT.getCode().equals(amount.getAmountType()))
                .findFirst().get();

        CreateOrderRequest.PaymentRequest paymentRequest = new CreateOrderRequest.PaymentRequest();
        paymentRequest.setPayAmount(orderAmount.getAmount());
        paymentRequest.setPayType(10);
        paymentRequest.setAccountType(AccountTypeEnum.THIRD.getCode());

        return Lists.newArrayList(paymentRequest);
    }


    private String genOrderId(String userId) {
        GenOrderIdRequest genOrderIdRequest = new GenOrderIdRequest();
        genOrderIdRequest.setUserId(userId);
        genOrderIdRequest.setBusinessIdentifier(2);
        GenOrderIdDTO genOrderIdDTO = orderService.genOrderId(genOrderIdRequest);
        return genOrderIdDTO.getOrderId();
    }

    public MockUserInfo genMockUser() {
        String userId = "";
        String receiverName = "";
        int random = new Random().nextInt(10000000) + 1;
        userId = "normalUser_";
        if (random < 10) {
            userId += "00";
        } else if (random < 100) {
            userId += "0";
        }
        userId += random;
        receiverName = "普通用户_" + random;

        return new MockUserInfo(userId, receiverName);
    }

    private static String genSkuCode() {
        int random = new Random().nextInt(100000) + 1;
        return "productSkuCode_" + random;
    }

    private static String cutOutSkuCodeNo(String skuCode) {
        return skuCode.substring("productSkuCode_".length());
    }


    /**
     * 新增主订单信息订单
     */
    private FullOrderData addNewMasterOrder(CreateOrderRequest createOrderRequest, List<ProductSkuDTO> productSkuList,
                                            Map<Integer, List<ProductSkuDTO>> productTypeMap,
                                            CalculateOrderAmountDTO calculateOrderAmountDTO) {
        NewOrderBuilder newOrderBuilder = new NewOrderBuilder(createOrderRequest, productSkuList, productTypeMap,
                calculateOrderAmountDTO, orderProperties, orderConverter);
        FullOrderData fullOrderData = newOrderBuilder.buildOrder()
                .setOrderType()
                .buildOrderItems()
                .addPreSaleInfoToOrderItems()
                .buildOrderDeliveryDetail()
                .buildOrderPaymentDetail()
                .buildOrderAmount()
                .buildOrderAmountDetail()
                .buildOperateLog()
                .buildOrderSnapshot()
                .build();

        // 订单条目信息
        List<OrderItemDO> orderItemDOList = fullOrderData.getOrderItemDOList();

        // 订单费用信息
        List<OrderAmountDO> orderAmountDOList = fullOrderData.getOrderAmountDOList();

        // 补全地址信息
        OrderDeliveryDetailDO orderDeliveryDetailDO = fullOrderData.getOrderDeliveryDetailDO();
        String detailAddress = getDetailAddress(orderDeliveryDetailDO);
        orderDeliveryDetailDO.setDetailAddress(detailAddress);

        // 补全订单状态变更日志
        OrderOperateLogDO orderOperateLogDO = fullOrderData.getOrderOperateLogDO();
        String remark = "创建订单操作0-10";
        orderOperateLogDO.setRemark(remark);

        // 补全订单商品快照信息
        List<OrderSnapshotDO> orderSnapshotDOList = fullOrderData.getOrderSnapshotDOList();
        for (OrderSnapshotDO orderSnapshotDO : orderSnapshotDOList) {
            // 订单费用信息
            if (orderSnapshotDO.getSnapshotType().equals(SnapshotTypeEnum.ORDER_AMOUNT.getCode())) {
                orderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(orderAmountDOList));
            }
            // 订单条目信息
            else if (orderSnapshotDO.getSnapshotType().equals(SnapshotTypeEnum.ORDER_ITEM.getCode())) {
                orderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(orderItemDOList));
            }
        }

        return fullOrderData;
    }

    /**
     * 获取用户收货详细地址
     */
    private String getDetailAddress(OrderDeliveryDetailDO orderDeliveryDetailDO) {
        String provinceCode = orderDeliveryDetailDO.getProvince();
        String cityCode = orderDeliveryDetailDO.getCity();
        String areaCode = orderDeliveryDetailDO.getArea();
        String streetCode = orderDeliveryDetailDO.getStreet();
        AddressQuery query = new AddressQuery();
        query.setProvinceCode(provinceCode);
        query.setCityCode(cityCode);
        query.setAreaCode(areaCode);
        query.setStreetCode(streetCode);
        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setProvinceCode(provinceCode);
        addressDTO.setProvince("北京");
        addressDTO.setCityCode(cityCode);
        addressDTO.setCity("北京");
        addressDTO.setArea(areaCode);
        addressDTO.setArea("东城区");
        addressDTO.setStreetCode(streetCode);
        addressDTO.setStreet("东华门街道");

        StringBuilder detailAddress = new StringBuilder();
        if (StringUtils.isNotEmpty(addressDTO.getProvince())) {
            detailAddress.append(addressDTO.getProvince());
        }
        if (StringUtils.isNotEmpty(addressDTO.getCity())) {
            detailAddress.append(addressDTO.getCity());
        }
        if (StringUtils.isNotEmpty(addressDTO.getArea())) {
            detailAddress.append(addressDTO.getArea());
        }
        if (StringUtils.isNotEmpty(addressDTO.getStreet())) {
            detailAddress.append(addressDTO.getStreet());
        }
        if (StringUtils.isNotEmpty(orderDeliveryDetailDO.getDetailAddress())) {
            detailAddress.append(orderDeliveryDetailDO.getDetailAddress());
        }
        return detailAddress.toString();
    }

    /**
     * mock 订单条目数
     *
     * @return
     */
    private static Integer genOrderItemNum() {
        return new Random().nextInt(10) + 1;
    }

    /**
     * mock sku下单数量
     *
     * @return
     */
    private static Integer genSaleQuantity() {
        return new Random().nextInt(100) + 1;
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class MockUserInfo {
        private String userId;
        private String receiverName;
    }

    public static void main(String[] args) {
        SpelExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression("#root.orderId");
        OrderInfoDO order = new OrderInfoDO();
        order.setOrderId("111111");
        System.out.println(expression.getValue(order));
    }

}
