package moe.ahao.commerce.order;

import com.alibaba.fastjson.JSONObject;
import moe.ahao.commerce.aftersale.api.command.LackCommand;
import moe.ahao.commerce.aftersale.api.dto.LackDTO;
import moe.ahao.commerce.aftersale.application.OrderLackAppService;
import moe.ahao.commerce.order.OrderApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = OrderApplication.class)
@ActiveProfiles("test")
public class OrderLackServiceTest {

    @Autowired
    private OrderLackAppService orderLackAppService;

    @Test
    public void lockItem() throws Exception {
        LackCommand lackRequest = new LackCommand();
        lackRequest.setOrderId("1011250000000010000");
        lackRequest.setUserId("user_id_001");

        Set<LackCommand.LackItem> itemRequests = new HashSet<>();
        itemRequests.add(new LackCommand.LackItem("10101010", new BigDecimal("1")));
        lackRequest.setLackItems(itemRequests);

        LackDTO dto = orderLackAppService.execute(lackRequest);
        System.out.println("dto=" + JSONObject.toJSONString(dto));
    }
}
