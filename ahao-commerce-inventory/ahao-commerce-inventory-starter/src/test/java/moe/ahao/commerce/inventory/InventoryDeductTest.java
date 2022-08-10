package moe.ahao.commerce.inventory;

import moe.ahao.commerce.inventory.api.command.DeductProductStockCommand;
import moe.ahao.commerce.inventory.api.command.ReleaseProductStockCommand;
import moe.ahao.commerce.inventory.api.dto.ProductStockDTO;
import moe.ahao.commerce.inventory.application.DeductProductStockAppService;
import moe.ahao.commerce.inventory.application.InventoryQueryService;
import moe.ahao.commerce.inventory.application.ReleaseProductStockAppService;
import moe.ahao.commerce.inventory.application.SyncStockToCacheProcessor;
import moe.ahao.embedded.RedisExtension;
import moe.ahao.util.commons.juc.ConcurrentTestUtils;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = InventoryApplication.class)
@ActiveProfiles("test")
// @Transactional // 这里不能加事务, MySQLTransactionRollbackException: Lock wait timeout exceeded; try restarting transaction
@EnableAutoConfiguration(exclude = RocketMQAutoConfiguration.class)
class InventoryDeductTest {
    private static final String skuCode1 = "10101010";
    private static final String skuCode2 = "10101011";
    private static final BigDecimal oldSaleStockQuantity1 = new BigDecimal(30);
    private static final BigDecimal oldSaledStockQuantity1 = new BigDecimal(70);
    private static final BigDecimal oldSaleStockQuantity2 = new BigDecimal(43);
    private static final BigDecimal oldSaledStockQuantity2 = new BigDecimal(7);
    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();

    @Autowired
    private InventoryQueryService inventoryQueryService;
    @Autowired
    private DeductProductStockAppService deductProductStockAppService;
    @Autowired
    private ReleaseProductStockAppService releaseProductStockAppService;
    @Autowired
    private SyncStockToCacheProcessor syncStockToCacheProcessor;

    @Test
    public void success() throws Exception {
        int threadCount = 20;
        BigDecimal saleIncremental1 = new BigDecimal(1);
        BigDecimal saleIncremental2 = new BigDecimal(2);

        // 1. 初始化数据到Redis
        this.init();

        // 2. 扣减库存
        this.deduct(threadCount, saleIncremental1, saleIncremental2);
        BigDecimal newSaleStockQuantity1 = oldSaleStockQuantity1.subtract(saleIncremental1.multiply(new BigDecimal(threadCount)));
        BigDecimal newSaledStockQuantity1 = oldSaledStockQuantity1.add(saleIncremental1.multiply(new BigDecimal(threadCount)));
        BigDecimal newSaleStockQuantity2 = oldSaleStockQuantity2.subtract(saleIncremental2.multiply(new BigDecimal(threadCount)));
        BigDecimal newSaledStockQuantity2 = oldSaledStockQuantity2.add(saleIncremental2.multiply(new BigDecimal(threadCount)));
        this.assertAmount(skuCode1, newSaleStockQuantity1, newSaledStockQuantity1);
        this.assertAmount(skuCode2, newSaleStockQuantity2, newSaledStockQuantity2);

        // 3. 回退库存
        this.release(threadCount, saleIncremental1, saleIncremental2);
        this.assertAmount(skuCode1, oldSaleStockQuantity1, oldSaledStockQuantity1);
        this.assertAmount(skuCode2, oldSaleStockQuantity2, oldSaledStockQuantity2);
    }

    @Test
    public void failure() throws Exception {
        int threadCount = 20;
        BigDecimal saleIncremental1 = new BigDecimal(1000);
        BigDecimal saleIncremental2 = new BigDecimal(2000);

        // 1. 初始化数据到Redis
        this.init();

        // 2. 扣减库存失败
        this.deduct(threadCount, saleIncremental1, saleIncremental2);
        this.assertAmount(skuCode1, oldSaleStockQuantity1, oldSaledStockQuantity1);
        this.assertAmount(skuCode2, oldSaleStockQuantity2, oldSaledStockQuantity2);
    }

    void init() throws Exception {
        syncStockToCacheProcessor.syncStock(skuCode1);
        syncStockToCacheProcessor.syncStock(skuCode2);
        this.assertAmount(skuCode1, oldSaleStockQuantity1, oldSaledStockQuantity1);
        this.assertAmount(skuCode2, oldSaleStockQuantity2, oldSaledStockQuantity2);
    }

    void deduct(int threadCount, BigDecimal saleIncremental1, BigDecimal saleIncremental2) throws Exception {
        List<Runnable> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            String orderId = "orderId-" + i;
            taskList.add(() -> {
                DeductProductStockCommand command = new DeductProductStockCommand();
                command.setBusinessIdentifier(1);
                command.setOrderId(orderId);
                command.setOrderItems(Arrays.asList(
                    new DeductProductStockCommand.OrderItem(skuCode1, saleIncremental1),
                    new DeductProductStockCommand.OrderItem(skuCode2, saleIncremental2)
                ));
                deductProductStockAppService.deduct(command);
            });
        }
        ConcurrentTestUtils.concurrentRunnable(threadCount, taskList);
    }

    void release(int threadCount, BigDecimal saleIncremental1, BigDecimal saleIncremental2) throws Exception {
        List<Runnable> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            String orderId = "orderId-" + i;
            taskList.add(() -> {
                ReleaseProductStockCommand command = new ReleaseProductStockCommand();
                command.setOrderId(orderId);
                command.setOrderItems(Arrays.asList(
                    new ReleaseProductStockCommand.OrderItem(skuCode1, saleIncremental1),
                    new ReleaseProductStockCommand.OrderItem(skuCode2, saleIncremental2)
                ));
                releaseProductStockAppService.releaseProductStock(command);
            });
        }
        ConcurrentTestUtils.concurrentRunnable(threadCount, taskList);
    }

    void assertAmount(String skuCode, BigDecimal expectSaleStockQuantity, BigDecimal expectSaledStockQuantity) {
        for (Map.Entry<String, ProductStockDTO> entry : inventoryQueryService.queryV1(skuCode).entrySet()) {
            Assertions.assertEquals(0, expectSaleStockQuantity.compareTo(entry.getValue().getSaleStockQuantity()));
            Assertions.assertEquals(0, expectSaledStockQuantity.compareTo(entry.getValue().getSaledStockQuantity()));
        }
        ProductStockDTO productStockDTO = inventoryQueryService.queryV2(skuCode);
        Assertions.assertEquals(0, expectSaleStockQuantity.compareTo(productStockDTO.getSaleStockQuantity()));
        Assertions.assertEquals(0, expectSaledStockQuantity.compareTo(productStockDTO.getSaledStockQuantity()));
    }
}
