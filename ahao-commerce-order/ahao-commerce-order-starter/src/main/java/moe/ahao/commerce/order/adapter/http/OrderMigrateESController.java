package moe.ahao.commerce.order.adapter.http;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.adapter.http.migrate.AbstractMigrateToEsHandler;
import moe.ahao.commerce.order.adapter.http.migrate.AfterSaleMigrateToEsHandler;
import moe.ahao.commerce.order.adapter.http.migrate.OrderMigrateToEsHandler;
import moe.ahao.domain.entity.Result;
import moe.ahao.util.commons.io.JSONHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 将现有数据迁移至es
 */
@Slf4j
@RestController
@RequestMapping("/api/order/migrate")
public class OrderMigrateESController {
    @Autowired
    private OrderMigrateToEsHandler orderMigrateToEsHandler;
    @Autowired
    private AfterSaleMigrateToEsHandler afterSaleMigrateToEsHandler;

    /**
     * 迁移订单
     */
    @PostMapping("/order")
    public Result<Boolean> migrateOrder(@RequestBody AbstractMigrateToEsHandler.Content content) throws Exception {
        orderMigrateToEsHandler.execute(content);
        return Result.success(true);
    }

    /**
     * 迁移售后单
     */
    @PostMapping("/afterSale")
    public Result<Boolean> migrateAfterSale(@RequestBody AbstractMigrateToEsHandler.Content content) throws Exception {
        afterSaleMigrateToEsHandler.execute(content);
        return Result.success(true);
    }
}
