package moe.ahao.commerce.inventory;

import moe.ahao.commerce.inventory.application.SyncStockToCacheProcessor;
import moe.ahao.commerce.inventory.infrastructure.cache.RedisCacheSupport;
import moe.ahao.embedded.RedisExtension;
import moe.ahao.util.spring.redis.RedisHelper;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = InventoryApplication.class)
@ActiveProfiles("test")

// @EnableAutoConfiguration(exclude = {SeataAutoConfiguration.class, RocketMQAutoConfiguration.class})
@EnableAutoConfiguration(exclude = {RocketMQAutoConfiguration.class})
class InventorySyncCacheTest {
    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();

    @Autowired
    private SyncStockToCacheProcessor syncStockToCacheProcessor;

    @Test
    void syncStockToCache() throws Exception {
        String skuCode1 = "10101010";
        String productStockKey1 = RedisCacheSupport.buildProductStockKey(skuCode1);
        String skuCode2 = "10101011";
        String productStockKey2 = RedisCacheSupport.buildProductStockKey(skuCode2);

        Assertions.assertEquals(0, RedisHelper.hmget(productStockKey1).size());
        Assertions.assertEquals(0, RedisHelper.hmget(productStockKey2).size());

        syncStockToCacheProcessor.syncStock(skuCode1);
        syncStockToCacheProcessor.syncStock(skuCode2);

        Assertions.assertTrue(RedisHelper.hmget(productStockKey1).size() > 0);
        Assertions.assertTrue(RedisHelper.hmget(productStockKey2).size() > 0);
    }
}
