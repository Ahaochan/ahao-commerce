package moe.ahao.commerce.order.migrate;

import moe.ahao.commerce.order.OrderApplication;
import moe.ahao.commerce.order.adapter.http.migrate.AbstractMigrateToEsHandler;
import moe.ahao.commerce.order.adapter.http.migrate.OrderMigrateToEsHandler;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = OrderApplication.class)
@ActiveProfiles("test")
public class MigrateToEsTest {
    @Autowired
    private OrderMigrateToEsHandler orderMigrateToEsHandler;

    @Test
    public void testOrder() throws Exception {
        AbstractMigrateToEsHandler.Content content = new AbstractMigrateToEsHandler.Content();
        content.setOffset(1);
        orderMigrateToEsHandler.execute(content);
    }
}
