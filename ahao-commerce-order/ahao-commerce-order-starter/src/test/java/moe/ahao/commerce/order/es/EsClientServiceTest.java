package moe.ahao.commerce.order.es;

import com.alibaba.fastjson.JSONObject;
import moe.ahao.commerce.order.OrderApplication;
import moe.ahao.commerce.order.adapter.mq.handler.EsOrderSyncHandler;
import moe.ahao.commerce.order.adapter.schedule.CheckMysqlEsDataConsistencyTask;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.OrderListEsRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data.EsOrderListQueryDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.util.commons.io.JSONHelper;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = OrderApplication.class)
@ActiveProfiles("test")
public class EsClientServiceTest {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private EsOrderSyncHandler esOrderSyncHandler;
    @Autowired
    private OrderListEsRepository orderListEsRepository;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private CheckMysqlEsDataConsistencyTask checkMysqlEsDataConsistencyTask;

    @Test
    public void index() throws Exception {
        String orderId = "1022011279374601100";
        esOrderSyncHandler.syncOrderInfos(Collections.singletonList(orderId), -1);

        EsOrderListQueryDO index = orderListEsRepository.getOneByOrderId(orderId);
        System.out.println("result =" + JSONObject.toJSONString(index));
    }

    @Test
    public void testCheckOrderInfoDbAndEsDataConsistency() {
        List<OrderInfoDO> orderInfoList = new ArrayList<>();
        OrderInfoDO orderInfo = new OrderInfoDO();
        orderInfo.setOrderId("1022011133237417100");
        orderInfo.setOrderStatus(10);

        OrderInfoDO orderInfo2 = new OrderInfoDO();
        orderInfo2.setOrderId("1022011133180072100");
        orderInfo2.setOrderStatus(10);

        OrderInfoDO orderInfo3 = new OrderInfoDO();
        orderInfo3.setOrderId("1022011133180071100");
        orderInfo3.setOrderStatus(0);

        orderInfoList.add(orderInfo);
        orderInfoList.add(orderInfo2);
        orderInfoList.add(orderInfo3);

        List<OrderInfoDO> diffOrderInfoList = checkMysqlEsDataConsistencyTask.getDiffOrderInfoList(orderInfoList);
        System.out.println(JSONHelper.toString(diffOrderInfoList));
    }
}
