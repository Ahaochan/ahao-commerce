package moe.ahao.commerce.order;

import moe.ahao.commerce.order.OrderApplication;
import moe.ahao.commerce.order.api.dto.OrderDetailDTO;
import moe.ahao.commerce.order.api.dto.OrderListDTO;
import moe.ahao.commerce.order.api.query.OrderPageQuery;
import moe.ahao.commerce.order.application.OrderQueryService;
import moe.ahao.domain.entity.PagingInfo;
import moe.ahao.util.commons.io.JSONHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = OrderApplication.class)
@ActiveProfiles("test")
public class OrderQueryServiceTest {
    @Autowired
    private OrderQueryService orderQueryService;

    @Test
    public void listByOrderIdV1() {
        OrderPageQuery query = new OrderPageQuery();
        Set<String> orderIds = new HashSet<>();
        orderIds.add("1011250000000010000");
        orderIds.add("1011260000000020000");
        query.setOrderIds(orderIds);
        PagingInfo<OrderListDTO> result = orderQueryService.queryV1(query);

        System.out.println(JSONHelper.toString(result));
        System.out.println(result.getList().size());
    }

    @Test
    public void listV1() {
        OrderPageQuery query = new OrderPageQuery();

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

        query.setQueryStartCreatedTime(new Date());
        query.setQueryEndCreatedTime(new Date());

        query.setQueryStartPayTime(new Date());
        query.setQueryEndPayTime(new Date());

        query.setQueryStartPayAmount(new BigDecimal("1"));
        query.setQueryEndPayAmount(new BigDecimal("1"));

        query.setPageNo(2);
        query.setPageSize(100);

        PagingInfo<OrderListDTO> result = orderQueryService.queryV1(query);

        System.out.println(JSONHelper.toString(result));
    }

    @Test
    public void orderDetail() throws Exception {
        String orderId = "1011250000000010000";
        OrderDetailDTO orderDetailDTO = orderQueryService.orderDetail(orderId);
        System.out.println(JSONHelper.toString(orderDetailDTO));
    }

    @Test
    public void listV2() throws Exception {
        OrderPageQuery query = new OrderPageQuery();
        query.setBusinessIdentifier(1);

        PagingInfo<OrderDetailDTO> result = orderQueryService.queryV2(query);

        System.out.println(JSONHelper.toString(result));
    }
}
