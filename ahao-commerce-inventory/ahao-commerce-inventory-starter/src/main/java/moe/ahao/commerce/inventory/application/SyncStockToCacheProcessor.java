package moe.ahao.commerce.inventory.application;


import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.inventory.infrastructure.cache.RedisCacheSupport;
import moe.ahao.commerce.inventory.infrastructure.exception.InventoryExceptionEnum;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockDO;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.mapper.ProductStockMapper;
import moe.ahao.util.spring.redis.RedisHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 同步商品sku库存到缓存处理器
 */
@Slf4j
@Service
public class SyncStockToCacheProcessor {
    @Autowired
    private ProductStockMapper productStockMapper;

    public Boolean syncStock(String skuCode) {
        // 1. 校验参数
        if (StringUtils.isEmpty(skuCode)) {
            throw InventoryExceptionEnum.SKU_CODE_IS_EMPTY.msg();
        }

        // 2. 查询商品库存
        ProductStockDO productStock = productStockMapper.selectOneBySkuCode(skuCode);
        if (productStock == null) {
            throw InventoryExceptionEnum.PRODUCT_SKU_STOCK_NOT_FOUND_ERROR.msg();
        }

        // 3. 删除缓存数据
        String redisKey = RedisCacheSupport.buildProductStockKey(productStock.getSkuCode());
        RedisHelper.del(redisKey);

        // 4. 保存商品库存到redis
        this.initRedis(productStock);
        return true;
    }

    /**
     * 保存商品库存到redis
     */
    public void initRedis(ProductStockDO productStock) {
        String productStockKey = RedisCacheSupport.buildProductStockKey(productStock.getSkuCode());
        Map<String, String> productStockValue = RedisCacheSupport.buildProductStockValue(productStock.getSaleStockQuantity(), productStock.getSaledStockQuantity());

        RedisHelper.hmset1(productStockKey, productStockValue);
    }
}
