package moe.ahao.commerce.inventory.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.inventory.api.command.DeductProductStockCommand;
import moe.ahao.commerce.inventory.infrastructure.exception.InventoryExceptionEnum;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockDO;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockLogDO;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.mapper.ProductStockLogMapper;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.mapper.ProductStockMapper;
import moe.ahao.exception.CommonBizExceptionEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 扣减商品库存处理器
 */
@Slf4j
@Service
public class DeductProductStockAppService {
    @Autowired
    private DeductProductStockAppService _this;

    @Autowired
    private ProductStockMapper productStockMapper;
    @Autowired
    private ProductStockLogMapper productStockLogMapper;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 扣减商品库存
     */
    public boolean deduct(DeductProductStockCommand command) {
        // 1. 检查入参
        this.check(command);

        String orderId = command.getOrderId();
        List<DeductProductStockCommand.OrderItem> orderItems = command.getOrderItems();
        // 保证每次加锁的顺序是相同的, 避免出现死锁的情况
        orderItems.sort(Comparator.comparing(DeductProductStockCommand.OrderItem::getSkuCode));
        for (DeductProductStockCommand.OrderItem orderItem : orderItems) {
            String skuCode = orderItem.getSkuCode();
            String lockKey = RedisLockKeyConstants.DEDUCT_PRODUCT_STOCK_KEY + skuCode;

            // 1. 添加Redis锁扣库存锁
            // 1.1. 防同一笔订单重复扣减
            // 1.2. 重量级锁，保证Mysql+Redis扣库存的原子性，同一时间只能有一个订单来扣，
            //      需要锁查询+扣库存, 获取不到锁, 阻塞等待5秒
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = this.tryLock(lock, 5, TimeUnit.SECONDS);
            if (!locked) {
                log.error("无法获取扣减库存锁, orderId:{}, skuCode:{}", orderId, skuCode);
                throw InventoryExceptionEnum.DEDUCT_PRODUCT_SKU_STOCK_CANNOT_ACQUIRE.msg();
            }
            try {
                // 2. 查询Mysql库存数据
                ProductStockDO productStockDO = productStockMapper.selectOneBySkuCode(skuCode);
                log.info("查询mysql库存数据, orderId:{}, productStockDO:{}", orderId, productStockDO);
                if (productStockDO == null) {
                    log.error("商品库存记录不存在, orderId:{}, skuCode:{}", orderId, skuCode);
                    throw InventoryExceptionEnum.PRODUCT_SKU_STOCK_NOT_FOUND_ERROR.msg();
                }

                // // 3. 查询Redis库存数据, 如果不存在就初始化Redis缓存
                // String productStockKey = RedisCacheSupport.buildProductStockKey(skuCode);
                // Map<String, String> productStockValue = RedisHelper.hmget(productStockKey);
                // if (productStockValue.isEmpty()) {
                //     // 如果查询不到redis库存数据，将mysql库存数据放入redis，以mysql的数据为准
                //     addProductStockProcessor.initRedis(productStockDO);
                // }

                // 4. 查询库存扣减日志, 做幂等性校验
                ProductStockLogDO productStockLog = productStockLogMapper.selectOneByOrderIdAndSkuCode(orderId, skuCode);
                if (productStockLog != null) {
                    log.info("已扣减过，扣减库存日志已存在, orderId={}, skuCode={}", orderId, skuCode);
                    return true;
                }

                // 5. 执行执库存扣减
                BigDecimal saleQuantity = orderItem.getSaleQuantity();
                // DeductStockDTO deductStock = new DeductStockDTO(orderId, skuCode, saleQuantity, productStockDO);
                _this.doDeductWithTxV2(orderId, saleQuantity, productStockDO);
            } finally {
                lock.unlock();
            }
        }
        return true;
    }

    /**
     * 检查锁定商品库存入参
     */
    private void check(DeductProductStockCommand deductProductStockRequest) {
        String orderId = deductProductStockRequest.getOrderId();
        if (StringUtils.isEmpty(orderId)) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }
        List<DeductProductStockCommand.OrderItem> orderItems = deductProductStockRequest.getOrderItems();
        if (CollectionUtils.isEmpty(orderItems)) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }
    }

    private boolean tryLock(RLock lock, long waitTime, TimeUnit timeUnit) {
        try {
            return lock.tryLock(waitTime, timeUnit);
        } catch (InterruptedException e) {
            log.error("无法获取释放库存锁{}", lock.getName(), e);
            return false;
        }
    }

    /**
     * 执行扣减商品库存逻辑
     *
     * 外部不能有@Transactional的原因分析
     * try阶段因为外部事务，当try阶段结束时，行锁一直不能释放
     * confirm阶段是seata server远程调用的，所以seata内部会自己开一个事务，来竞争行锁
     * 相当于有两个事务在竞争同一个skuCode=1的行锁
     *
     * 等到try阶段的事务超时，try阶段的事务回滚了，然后confirm阶段的事务拿到行锁，提交事务成功
     */
    // @Autowired
    // private LockMysqlStockTccService lockMysqlStockTccService;
    // @Autowired
    // private LockRedisStockTccService lockRedisStockTccService;
    // @Autowired
    // private SyncStockToCacheProcessor syncStockToCacheProcessor;
    // @GlobalTransactional(rollbackFor = Exception.class)
    // public void doDeductWithTxV1(DeductStockDTO deductStock) {
    //     String skuCode = deductStock.getSkuCode();
    //     String traceId = MdcUtil.getOrInitTraceId();
    //     // 1. 执行执行mysql库存扣减
    //     boolean result = lockMysqlStockTccService.deductStock(null, deductStock, traceId);
    //     if (!result) {
    //         throw InventoryExceptionEnum.DEDUCT_PRODUCT_SKU_STOCK_ERROR.msg();
    //     }
    //
    //     // 2. 执行redis库存扣减
    //     result = lockRedisStockTccService.deductStock(null, deductStock, traceId);
    //     if (!result) {
    //         // 3. 更新失败, 以mysql数据为准
    //         log.info("执行redis库存扣减失败, deductStock={}", deductStock);
    //         syncStockToCacheProcessor.syncStock(skuCode);
    //     }
    // }

    @Transactional(rollbackFor = Exception.class)
    public void doDeductWithTxV2(String orderId, BigDecimal saleQuantity, ProductStockDO deductStock) {
        String skuCode = deductStock.getSkuCode();
        // 1. 扣减mysql商品库存
        int result = productStockMapper.deductProductStock(skuCode, saleQuantity);
        boolean success = result > 0;
        if (!success) {
            throw InventoryExceptionEnum.DEDUCT_PRODUCT_SKU_STOCK_ERROR.msg();
        }
        // 2. 增加库存扣减日志表
        ProductStockLogDO logDO = this.buildStockLog(orderId, saleQuantity, deductStock);
        productStockLogMapper.insert(logDO);
    }

    /**
     * 构建扣减库存日志
     */
    private ProductStockLogDO buildStockLog(String orderId, BigDecimal saleQuantity, ProductStockDO productStockDO) {
        String skuCode = productStockDO.getSkuCode();
        BigDecimal originSaleStock = productStockDO.getSaleStockQuantity();
        // 通过扣减log获取原始已销售库存
        // 1. 查询sku库存最近一笔扣减日志
        ProductStockLogDO latestLog = productStockLogMapper.selectLastOneBySkuCode(skuCode);
        // 2. 获取原始的已销售库存
        BigDecimal originSaledStock = latestLog == null ?
            productStockDO.getSaledStockQuantity() :    // 第一次扣，直接取productStockDO的saledStockQuantity
            latestLog.getIncreasedSaledStockQuantity(); // 取最近一笔扣减日志的increasedSaledStockQuantity

        ProductStockLogDO logDO = new ProductStockLogDO();
        logDO.setOrderId(orderId);
        logDO.setSkuCode(skuCode);
        logDO.setOriginSaleStockQuantity(originSaleStock);
        logDO.setOriginSaledStockQuantity(originSaledStock);
        BigDecimal deductedSaleStockQuantity = originSaleStock.subtract(saleQuantity);
        logDO.setDeductedSaleStockQuantity(deductedSaleStockQuantity);
        BigDecimal increasedSaledStockQuantity = originSaledStock.add(saleQuantity);
        logDO.setIncreasedSaledStockQuantity(increasedSaledStockQuantity);
        return logDO;
    }
}
