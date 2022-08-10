package moe.ahao.commerce.inventory.adapter.http;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.inventory.api.InventoryFeignApi;
import moe.ahao.commerce.inventory.api.dto.ProductStockDTO;
import moe.ahao.commerce.inventory.application.InventoryQueryService;
import moe.ahao.commerce.inventory.application.SyncStockToCacheProcessor;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockDO;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockLogDO;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.mapper.ProductStockLogMapper;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.mapper.ProductStockMapper;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 正向下单流程接口冒烟测试
 */
@Slf4j
@RestController
@RequestMapping(InventoryFeignApi.PATH)
public class InventoryTestController {
    @Autowired
    private InventoryQueryService inventoryQueryService;
    @Autowired
    private SyncStockToCacheProcessor syncStockToCacheProcessor;
    @Autowired
    private ProductStockMapper productStockMapper;
    @Autowired
    private ProductStockLogMapper productStockLogMapper;

    /**
     * 查询商品库存
     */
    @GetMapping("/getStockInfo")
    public Result<ProductStockDTO> getStockInfo(@RequestParam String skuCode) {
        ProductStockDTO result = inventoryQueryService.queryV2(skuCode);
        return Result.success(result);
    }

    /**
     * 同步商品sku库存数据到缓存
     */
    @GetMapping("/syncStockToCache")
    public Result<Boolean> syncStockToCache(@RequestParam String skuCode) {
        syncStockToCacheProcessor.syncStock(skuCode);
        return Result.success(true);
    }

    /**
     * 初始化压测数据
     */
    @PostMapping("/initMeasureData")
    public Result<Boolean> initMeasureData(@RequestBody List<String> skuCodes) {
        if (CollectionUtils.isNotEmpty(skuCodes)) {
            // 初始化压测库存数据数据
            ProductStockDO update = new ProductStockDO();
            update.setSaleStockQuantity(new BigDecimal("1000000000"));
            update.setSaledStockQuantity(BigDecimal.ZERO);
            int affectRows = productStockMapper.update(update, new LambdaUpdateWrapper<ProductStockDO>().in(ProductStockDO::getSkuCode, skuCodes));
            for (String skuCode : skuCodes) {
                // 同步缓存
                syncStockToCacheProcessor.syncStock(skuCode);
            }
            productStockLogMapper.delete(new LambdaUpdateWrapper<ProductStockLogDO>().eq(ProductStockLogDO::getSkuCode, skuCodes));
        }
        return Result.success(true);
    }
}
