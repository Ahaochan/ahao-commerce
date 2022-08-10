package moe.ahao.commerce.order;

import com.alibaba.fastjson.JSONObject;
import moe.ahao.commerce.aftersale.api.dto.AfterSaleOrderDetailDTO;
import moe.ahao.commerce.aftersale.api.dto.AfterSaleOrderListDTO;
import moe.ahao.commerce.aftersale.api.query.AfterSalePageQuery;
import moe.ahao.commerce.aftersale.application.AfterSaleQueryService;
import moe.ahao.commerce.order.OrderApplication;
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
public class AfterSaleQueryServiceTest {
    @Autowired
    private AfterSaleQueryService afterSaleQueryService;

    @Test
    public void test() {
        AfterSalePageQuery query = new AfterSalePageQuery();
        Set<String> afterSaleIds = new HashSet<>();
        afterSaleIds.add("2021112837104128002");
        afterSaleIds.add("2021112837103888002");
        query.setAfterSaleIds(afterSaleIds);
        PagingInfo<AfterSaleOrderListDTO> result = afterSaleQueryService.queryV1(query);

        System.out.println(JSONObject.toJSONString(result));
        System.out.println(result.getList().size());
    }

    @Test
    public void test1() {

        AfterSalePageQuery query = new AfterSalePageQuery();

        Set<String> orderIds = new HashSet<>();
        orderIds.add("11");
        query.setOrderIds(orderIds);

        Set<Integer> orderTypes = new HashSet<>();
        orderTypes.add(1);
        query.setOrderTypes(orderTypes);

        Set<Integer> afterSaleStatus = new HashSet<>();
        afterSaleStatus.add(1);
        query.setAfterSaleStatus(afterSaleStatus);

        Set<Integer> applySources = new HashSet<>();
        applySources.add(1);
        query.setApplySources(applySources);

        Set<Integer> afterSaleTypes = new HashSet<>();
        afterSaleTypes.add(1);
        query.setAfterSaleTypes(afterSaleTypes);


        Set<String> afterSaleIds = new HashSet<>();
        afterSaleIds.add("1");
        query.setAfterSaleIds(afterSaleIds);

        Set<String> userIds = new HashSet<>();
        userIds.add("1");
        query.setUserIds(userIds);

        Set<String> skuCodes = new HashSet<>();
        skuCodes.add("1");
        query.setSkuCodes(skuCodes);

        query.setQueryStartCreatedTime(new Date());
        query.setQueryEndCreatedTime(new Date());
        query.setQueryStartApplyTime(new Date());
        query.setQueryEndApplyTime(new Date());
        query.setQueryStartReviewTime(new Date());
        query.setQueryEndReviewTime(new Date());
        query.setQueryStartRefundPayTime(new Date());
        query.setQueryEndRefundPayTime(new Date());
        query.setQueryStartRefundAmount(new BigDecimal("1"));
        query.setQueryEndRefundAmount(new BigDecimal("1"));

        PagingInfo<AfterSaleOrderListDTO> result = afterSaleQueryService.queryV1(query);

        System.out.println(JSONObject.toJSONString(result));
        System.out.println(result.getList().size());
    }

    @Test
    public void afterSaleDetail() throws Exception {
        String afterSaleId = "2021112837103888002";
        AfterSaleOrderDetailDTO afterSaleOrderDetailDTO = afterSaleQueryService.afterSaleDetail(afterSaleId);

        System.out.println(JSONHelper.toString(afterSaleOrderDetailDTO));
    }
}
