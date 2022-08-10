package moe.ahao.commerce.inventory.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.inventory.api.command.AddProductStockCommand;
import moe.ahao.commerce.inventory.infrastructure.exception.InventoryExceptionEnum;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockDO;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.mapper.ProductStockMapper;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 添加商品库存处理器
 */
@Slf4j
@Service
public class AddProductStockAppService {
    @Autowired
    @Lazy
    private AddProductStockAppService _this;

    @Autowired
    private ProductStockMapper productStockMapper;

    @Autowired
    private RedissonClient redissonClient;

    public boolean add(AddProductStockCommand command) {
        // 1. 校验入参
        this.check(command);

        // 2. 查询商品库存
        ProductStockDO productStock = productStockMapper.selectOneBySkuCode(command.getSkuCode());
        if (productStock != null) {
            throw InventoryExceptionEnum.PRODUCT_SKU_STOCK_EXISTED_ERROR.msg();
        }

        // 3. 添加redis锁，防并发
        String lockKey = RedisLockKeyConstants.ADD_PRODUCT_STOCK_KEY + command.getSkuCode();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw InventoryExceptionEnum.ADD_PRODUCT_SKU_STOCK_ERROR.msg();
        }
        try {
            // 4. 执行添加商品库存逻辑
            _this.doAddStockWithTx(command);
        } finally {
            // 5. 解锁
            lock.unlock();
        }
        return true;
    }

    /**
     * 执行添加商品库存逻辑
     */
    @Transactional(rollbackFor = Exception.class)
    public void doAddStockWithTx(AddProductStockCommand command) {
        // 1. 构造商品库存DO
        ProductStockDO productStock = new ProductStockDO();
        productStock.setSkuCode(command.getSkuCode());
        productStock.setSaleStockQuantity(command.getSaleStockQuantity());
        productStock.setSaledStockQuantity(BigDecimal.ZERO);

        // 2. 保存商品库存到mysql
        productStockMapper.insert(productStock);

        // // 3. 保存商品库存到redis
        // this.initRedis(productStock);
    }

    /**
     * 校验添加商品库存入参
     */
    private void check(AddProductStockCommand command) {
        String skuCode = command.getSkuCode();
        if (StringUtils.isEmpty(skuCode)) {
            throw InventoryExceptionEnum.SKU_CODE_IS_EMPTY.msg();
        }
        BigDecimal saleStockQuantity = command.getSaleStockQuantity();
        if (saleStockQuantity == null) {
            throw InventoryExceptionEnum.SALE_STOCK_QUANTITY_IS_EMPTY.msg();
        }
        if (saleStockQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw InventoryExceptionEnum.SALE_STOCK_QUANTITY_CANNOT_BE_NEGATIVE_NUMBER.msg();
        }
    }
}
