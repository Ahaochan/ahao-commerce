package moe.ahao.commerce.order;

import moe.ahao.commerce.order.OrderApplication;
import moe.ahao.commerce.order.application.OrderFulFillService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = OrderApplication.class)
@ActiveProfiles("test")
public class OrderFulFillServiceTest {

    @Autowired
    private OrderFulFillService orderFulFillService;

    @Test
    public void triggerOrderFulFill() throws Exception {
        String orderId = "1011250000000010000";
        orderFulFillService.triggerOrderFulFill(orderId);
    }
}
