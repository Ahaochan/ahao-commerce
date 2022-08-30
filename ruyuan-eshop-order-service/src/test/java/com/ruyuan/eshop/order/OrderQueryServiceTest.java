package com.ruyuan.eshop.order;

import com.alibaba.fastjson.JSONObject;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.common.tuple.Pair;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.domain.dto.CreateOrderDTO;
import com.ruyuan.eshop.order.domain.dto.GenOrderIdDTO;
import com.ruyuan.eshop.order.domain.dto.OrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.OrderListDTO;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import com.ruyuan.eshop.order.domain.request.CreateOrderRequest;
import com.ruyuan.eshop.order.domain.request.GenOrderIdRequest;
import com.ruyuan.eshop.order.service.OrderQueryService;
import com.ruyuan.eshop.order.service.OrderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

@SpringBootTest(classes = OrderApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class OrderQueryServiceTest {

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderService orderService;

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

    @Test
    public void test() {
        OrderQuery query = new OrderQuery();
        Set<String> orderIds = new HashSet<>();
        orderIds.add("1011250000000010000");
        orderIds.add("1011260000000020000");
        query.setOrderIds(orderIds);
        PagingInfo<OrderListDTO> result = orderQueryService.executeListQueryV1(query);

        System.out.println(JSONObject.toJSONString(result));
        System.out.println(result.getList().size());
    }

    @Test
    public void test1() {

        OrderQuery query = new OrderQuery();

        query.setBusinessIdentifier(1);
        Set<String> orderIds = new HashSet<>();
        orderIds.add("11");
        query.setOrderIds(orderIds);

        Set<Integer> orderTypes = new HashSet<>();
        orderTypes.add(1);
        query.setOrderTypes(orderTypes);

        Set<String> sellerId = new HashSet<>();
        sellerId.add("1");
        query.setSellerIds(sellerId);

        Set<String> parentOrderIds = new HashSet<>();
        parentOrderIds.add("1");
        query.setParentOrderIds(parentOrderIds);

        Set<String> userIds = new HashSet<>();
        userIds.add("1");
        query.setUserIds(userIds);

        Set<Integer> orderStatus = new HashSet<>();
        orderStatus.add(1);
        query.setOrderStatus(orderStatus);

        Set<String> receiverPhones = new HashSet<>();
        receiverPhones.add("1");
        query.setReceiverPhones(receiverPhones);

        Set<String> receiverNames = new HashSet<>();
        receiverNames.add("1");
        query.setReceiverNames(receiverNames);

        Set<String> tradeNos = new HashSet<>();
        tradeNos.add("1");
        query.setTradeNos(tradeNos);

        Set<String> skuCodes = new HashSet<>();
        skuCodes.add("1");
        query.setSkuCodes(skuCodes);

        Set<String> productNames = new HashSet<>();
        productNames.add("1");
        query.setProductNames(productNames);

        Pair<Date, Date> createdTimeInterval = Pair.of(new Date(), new Date());
        query.setCreatedTimeInterval(createdTimeInterval);
        Pair<Date, Date> payTimeInterval = Pair.of(new Date(), new Date());
        query.setPayTimeInterval(payTimeInterval);

        Pair<Integer, Integer> payAmountInterval = Pair.of(1, 1);
        query.setPayAmountInterval(payAmountInterval);

        query.setPageNo(2);
        query.setPageSize(100);

        PagingInfo<OrderListDTO> result = orderQueryService.executeListQueryV1(query);

        System.out.println(JSONObject.toJSONString(result));
    }

    @Test
    public void orderDetail() throws Exception {
        String orderId = "1011250000000010000";
        OrderDetailDTO orderDetailDTO = orderQueryService.orderDetailV1(orderId);
        System.out.println(JSONObject.toJSONString(orderDetailDTO));
    }


    @Test
    public void test_v2() throws Exception {
        OrderQuery query = new OrderQuery();
        query.setBusinessIdentifier(1);

        PagingInfo<OrderDetailDTO> result = orderQueryService.executeListQueryV2(query);

        System.out.println(JSONObject.toJSONString(result));
    }

    @Test
    public void prepareOrderQueryListData() throws Exception {

        for (int i = 0; i < 30; i++) {
            CreateOrderRequest createOrderRequest = JSONObject.parseObject(CREATE_ORDER_PARAM_TEMPLATE, CreateOrderRequest.class);
            String userId = genUserId();
            String skuCode = genSkuCode();
            Integer saleQuantity = genSaleQuantity();
            createOrderRequest.setBusinessIdentifier(2);
            createOrderRequest.setOrderId(genOrderId(userId));
            createOrderRequest.getOrderItemRequestList().get(0).setSkuCode(skuCode);
            createOrderRequest.getOrderItemRequestList().get(0).setSaleQuantity(saleQuantity);
            modifyOrderAmountList(createOrderRequest.getOrderAmountRequestList(), saleQuantity);
            modifyPaymentList(createOrderRequest.getPaymentRequestList(), saleQuantity);
            createOrderRequest.setReceiverName(genName(createOrderRequest.getReceiverName()));
            createOrderRequest.setReceiverPhone("13334541555");
            CreateOrderDTO createOrderDTO = orderService.createOrder(createOrderRequest);
            System.out.println(JSONObject.toJSONString(createOrderDTO));
        }

        Thread.sleep(30000);
    }

    private void modifyPaymentList(List<CreateOrderRequest.PaymentRequest> paymentRequestList, int saleQuantity) {
        int amount = 1000 * saleQuantity;
        for (CreateOrderRequest.PaymentRequest request : paymentRequestList) {
            request.setPayAmount(amount);
        }
    }

    private void modifyOrderAmountList(List<CreateOrderRequest.OrderAmountRequest> orderAmountRequestList, int saleQuantity) {
        int amount = 1000 * saleQuantity;
        for (CreateOrderRequest.OrderAmountRequest request : orderAmountRequestList) {
            if (request.getAmountType().equals(10) || request.getAmountType().equals(50)) {
                request.setAmount(amount);
            }
        }
    }

    private String genName(String name) {
        return name + "_00" + new Random().nextInt(4);
    }

    private String genOrderId(String userId) {
        GenOrderIdRequest genOrderIdRequest = new GenOrderIdRequest();
        genOrderIdRequest.setUserId(userId);
        genOrderIdRequest.setBusinessIdentifier(2);
        GenOrderIdDTO genOrderIdDTO = orderService.genOrderId(genOrderIdRequest);
        return genOrderIdDTO.getOrderId();
    }

    private static String genUserId() {
        return "userId_00" + new Random().nextInt(4);
    }

    private static String genSkuCode() {

        List<String> skuCodes = new ArrayList<>();
        skuCodes.add("skuCode001");
        skuCodes.add("skuCode003");
        skuCodes.add("skuCode004");
        skuCodes.add("skuCode005");

        return skuCodes.get(new Random().nextInt(4));
    }

    private static Integer genSaleQuantity() {
        return (new Random().nextInt(4) + 1) * 10;
    }


    public static void main(String[] args) {
        for (int i = 0; i < 20; i++) {
            System.out.println(genSaleQuantity());
        }
    }
}
