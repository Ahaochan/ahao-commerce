package moe.ahao.commerce.inventory.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.inventory.api.command.ModifyProductStockCommand;
import moe.ahao.commerce.inventory.infrastructure.exception.InventoryExceptionEnum;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockDO;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.mapper.ProductStockMapper;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class ModifyProductStockAppService {
    @Autowired
    private ModifyProductStockAppService _this;
    @Autowired
    private ProductStockMapper productStockMapper;
    @Autowired
    private RedissonClient redissonClient;

    public Boolean doModify(ModifyProductStockCommand command) {
        // 1. 校验入参
        this.check(command);

        // 2. 查询商品库存
        ProductStockDO productStock = productStockMapper.selectOneBySkuCode(command.getSkuCode());
        if (productStock == null) {
            throw InventoryExceptionEnum.PRODUCT_SKU_STOCK_NOT_FOUND_ERROR.msg();
        }

        // 3. 校验库存被扣减后是否小于0
        BigDecimal stockIncremental = command.getStockIncremental();
        BigDecimal saleStockQuantity = productStock.getSaleStockQuantity();
        if (saleStockQuantity.add(stockIncremental).compareTo(BigDecimal.ZERO) < 0) {
            throw InventoryExceptionEnum.SALE_STOCK_QUANTITY_CANNOT_BE_NEGATIVE_NUMBER.msg();
        }

        // 4. 加分布式锁, 保证mysql和redis数据的一致性
        String lockKey = RedisLockKeyConstants.MODIFY_PRODUCT_STOCK_KEY + command.getSkuCode();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw InventoryExceptionEnum.INCREASE_PRODUCT_SKU_STOCK_ERROR.msg();
        }
        try {
            _this.doModifyWithTx(productStock, stockIncremental);
        } finally {
            lock.unlock();
        }

        return true;
    }

    /**
     * 校验调整商品库存入参
     */
    private void check(ModifyProductStockCommand command) {
        String skuCode = command.getSkuCode();
        if (StringUtils.isEmpty(skuCode)) {
            throw InventoryExceptionEnum.SKU_CODE_IS_EMPTY.msg();
        }
        BigDecimal stockIncremental = command.getStockIncremental();
        if (stockIncremental == null) {
            throw InventoryExceptionEnum.SALE_STOCK_INCREMENTAL_QUANTITY_IS_EMPTY.msg();
        }
        if (stockIncremental.compareTo(BigDecimal.ZERO) == 0) {
            throw InventoryExceptionEnum.SALE_STOCK_INCREMENTAL_QUANTITY_CANNOT_BE_ZERO.msg();
        }
    }

    /**
     * 调整商品库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void doModifyWithTx(ProductStockDO productStock, BigDecimal stockIncremental) {
        // 1. 更新mysql商品可销售库存数量
        String skuCode = productStock.getSkuCode();
        BigDecimal originSaleStockQuantity = productStock.getSaleStockQuantity();
        int num = productStockMapper.modifyProductStock(skuCode, originSaleStockQuantity, stockIncremental);
        if (num <= 0) {
            throw InventoryExceptionEnum.INCREASE_PRODUCT_SKU_STOCK_ERROR.msg();
        }

        // // 2. lua脚本更新redis商品可销售库存数量
        // String luaScript = LuaScript.MODIFY_PRODUCT_STOCK;
        // String productStockKey = RedisCacheSupport.buildProductStockKey(skuCode);
        // String saleStockKey = RedisCacheSupport.SALE_STOCK;
        // Long result = RedisHelper.getStringRedisTemplate().execute(new DefaultRedisScript<>(luaScript, Long.class),
        //     Arrays.asList(productStockKey, saleStockKey), String.valueOf(originSaleStockQuantity), String.valueOf(stockIncremental));
        //
        // // 3. redis更新异常, 以mysql的数据为准
        // if (result == null || result < 0) {
        //     RedisHelper.del(saleStockKey);
        //
        //     ProductStockDO productStockInDB = productStockMapper.selectOneBySkuCode(skuCode);
        //     addProductStockProcessor.initRedis(productStockInDB);
        // }
    }
}
