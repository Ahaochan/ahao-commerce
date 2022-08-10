package moe.ahao.commerce.inventory.application;

import moe.ahao.commerce.inventory.api.dto.ProductStockDTO;
import moe.ahao.commerce.inventory.infrastructure.cache.RedisCacheSupport;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockDO;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.mapper.ProductStockMapper;
import moe.ahao.util.spring.redis.RedisHelper;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static moe.ahao.commerce.inventory.infrastructure.cache.RedisCacheSupport.SALED_STOCK;
import static moe.ahao.commerce.inventory.infrastructure.cache.RedisCacheSupport.SALE_STOCK;

@Service
public class InventoryQueryService {
    @Autowired
    private ProductStockMapper productStockMapper;

    @Deprecated
    public Map<String, ProductStockDTO> queryV1(String skuCode) {
        Map<String, ProductStockDTO> result = new HashMap<>();
        result.put("mysql", this.getMySQLData(skuCode));
        // result.put("redis", this.getRedisData(skuCode));
        return result;
    }

    public ProductStockDTO queryV2(String skuCode) {
        return this.getMySQLData(skuCode);
    }

    private ProductStockDTO getMySQLData(String skuCode) {
        ProductStockDTO dto = new ProductStockDTO();
        dto.setSkuCode(skuCode);

        ProductStockDO productStock = productStockMapper.selectOneBySkuCode(skuCode);
        if (productStock == null) {
            return dto;
        }
        dto.setSaleStockQuantity(productStock.getSaleStockQuantity());
        dto.setSaledStockQuantity(productStock.getSaledStockQuantity());
        return dto;
    }

    private ProductStockDTO getRedisData(String skuCode) {
        ProductStockDTO dto = new ProductStockDTO();
        dto.setSkuCode(skuCode);

        String productStockKey = RedisCacheSupport.buildProductStockKey(skuCode);
        Map<String, String> productStockValue = RedisHelper.hmget(productStockKey);
        if(MapUtils.isEmpty(productStockValue)) {
            return dto;
        }
        Optional.ofNullable(productStockValue.get(SALE_STOCK)).map(BigDecimal::new).ifPresent(dto::setSaleStockQuantity);
        Optional.ofNullable(productStockValue.get(SALED_STOCK)).map(BigDecimal::new).ifPresent(dto::setSaledStockQuantity);
        return dto;
    }
}
