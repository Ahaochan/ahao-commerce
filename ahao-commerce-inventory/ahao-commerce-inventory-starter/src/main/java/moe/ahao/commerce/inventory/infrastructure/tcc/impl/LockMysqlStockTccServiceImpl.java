// package moe.ahao.commerce.inventory.infrastructure.tcc.impl;
//
// import com.alibaba.fastjson.JSONObject;
// import io.seata.rm.tcc.api.BusinessActionContext;
// import lombok.extern.slf4j.Slf4j;
// import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
// import moe.ahao.commerce.common.infrastructure.utils.MdcUtil;
// import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockDO;
// import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockLogDO;
// import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.mapper.ProductStockLogMapper;
// import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.mapper.ProductStockMapper;
// import moe.ahao.commerce.inventory.infrastructure.tcc.DeductStockDTO;
// import moe.ahao.commerce.inventory.infrastructure.tcc.LockMysqlStockTccService;
// import moe.ahao.commerce.inventory.infrastructure.tcc.TccResultHolder;
// import org.redisson.api.RLock;
// import org.redisson.api.RedissonClient;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
//
// import java.math.BigDecimal;
//
// @Slf4j
// @Service
// public class LockMysqlStockTccServiceImpl implements LockMysqlStockTccService {
//     @Autowired
//     private ProductStockMapper productStockMapper;
//     @Autowired
//     private ProductStockLogMapper productStockLogMapper;
//
//     @Autowired
//     private RedissonClient redissonClient;
//
//     @Override
//     @Transactional(rollbackFor = Exception.class)
//     public boolean deductStock(BusinessActionContext actionContext, DeductStockDTO deductStock, String traceId) {
//         String xid = actionContext.getXid();
//         String orderId = deductStock.getOrderId();
//         String skuCode = deductStock.getSkuCode();
//         BigDecimal saleQuantity = deductStock.getSaleQuantity();
//
//         // 标识try阶段开始执行
//         log.info("prepare阶段标记start, 扣减mysql可售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//             deductStock.getProductStockDO().getSaleStockQuantity(),
//             deductStock.getProductStockDO().getSaledStockQuantity(),
//             xid);
//         TccResultHolder.tagTryStart(getClass(), skuCode, xid);
//
//         // 悬挂问题: rollback接口先进行了空回滚, try接口才执行, 导致try接口预留的资源无法被confirm和cancel
//         // 解决方案: 当出现空回滚时, 在数据库中插一条记录, 在try这里判断一下
//         if (this.isEmptyRollback()) {
//             log.info("prepare阶段出现悬挂, 扣减mysql可售库存, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
//             return false;
//         }
//
//         int result = productStockMapper.deductSaleStock(skuCode, saleQuantity);
//         log.info("prepare阶段进行, 扣减mysql可售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//             deductStock.getProductStockDO().getSaleStockQuantity().subtract(saleQuantity),
//             deductStock.getProductStockDO().getSaledStockQuantity(),
//             xid);
//         boolean success = result > 0;
//         if (success) {
//             // 插入一条扣减日志表
//             ProductStockLogDO logDO = this.buildStockLog(deductStock.getProductStockDO(), orderId, skuCode, saleQuantity);
//             log.info("prepare阶段进行, 扣减mysql可售库存插入扣减日志表, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//                 deductStock.getProductStockDO().getSaleStockQuantity().subtract(saleQuantity),
//                 deductStock.getProductStockDO().getSaledStockQuantity(),
//                 xid);
//             productStockLogMapper.insert(logDO);
//
//             // 标识try阶段执行成功
//             TccResultHolder.tagTrySuccess(getClass(), skuCode, xid);
//             log.info("prepare阶段结束, 扣减mysql可售库存, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
//         }
//         return success;
//     }
//
//     @Override
//     public void commit(BusinessActionContext actionContext) {
//         DeductStockDTO deductStock = ((JSONObject) actionContext.getActionContext("deductStock")).toJavaObject(DeductStockDTO.class);
//         String traceId = (String) actionContext.getActionContext("traceId");
//         MdcUtil.setUserTraceId(traceId);
//
//         String xid = actionContext.getXid();
//         String orderId = deductStock.getOrderId();
//         String skuCode = deductStock.getSkuCode();
//         BigDecimal saleQuantity = deductStock.getSaleQuantity();
//
//         log.info("confirm阶段开始, 增加mysql已售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//             deductStock.getProductStockDO().getSaleStockQuantity().subtract(saleQuantity),
//             deductStock.getProductStockDO().getSaledStockQuantity(),
//             xid);
//
//         // 当出现网络异常或者TC Server异常时, 会出现重复调用commit阶段的情况, 所以需要进行幂等判断
//         if (!TccResultHolder.isTrySuccess(getClass(), skuCode, xid)) {
//             log.info("confirm阶段幂等性校验失败, 增加mysql已售库存, 已经执行过commit阶段, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
//             return;
//         }
//
//         productStockMapper.increaseSaledStock(skuCode, saleQuantity);
//         log.info("confirm阶段进行, 增加mysql已售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//             deductStock.getProductStockDO().getSaleStockQuantity().subtract(saleQuantity),
//             deductStock.getProductStockDO().getSaledStockQuantity().add(saleQuantity),
//             xid);
//
//         // 移除标识
//         TccResultHolder.removeResult(getClass(), skuCode, xid);
//         log.info("confirm阶段结束, 增加mysql已售库存, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
//     }
//
//     @Override
//     public void rollback(BusinessActionContext actionContext) {
//         String xid = actionContext.getXid();
//         DeductStockDTO deductStock = ((JSONObject) actionContext.getActionContext("deductStock")).toJavaObject(DeductStockDTO.class);
//         String traceId = (String) actionContext.getActionContext("traceId");
//         MdcUtil.setUserTraceId(traceId);
//         String orderId = deductStock.getOrderId();
//         String skuCode = deductStock.getSkuCode();
//         BigDecimal saleQuantity = deductStock.getSaleQuantity();
//         log.info("cancel阶段开始, 回滚增加mysql可售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//             deductStock.getProductStockDO().getSaleStockQuantity().subtract(saleQuantity),
//             deductStock.getProductStockDO().getSaledStockQuantity(),
//             xid);
//
//         // 空回滚处理
//         if (TccResultHolder.isTagNull(getClass(), skuCode, xid)) {
//             log.info("cancel阶段发生空回滚, 出现空回滚, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
//             this.insertEmptyRollbackTag();
//             return;
//         }
//
//         // 幂等处理
//         // try阶段没有完成的情况下，不必执行回滚，因为try阶段有本地事务，事务失败时已经进行了回滚
//         // 如果try阶段成功，而其他全局事务参与者失败，这里会执行回滚
//         if (!TccResultHolder.isTrySuccess(getClass(), skuCode, xid)) {
//             log.info("cancel阶段幂等性校验失败, 无需回滚, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
//             return;
//         }
//
//
//         String lockKey = RedisLockKeyConstants.DEDUCT_PRODUCT_STOCK_KEY + skuCode;
//         RLock lock = redissonClient.getLock(lockKey);
//         lock.lock();
//         try {
//             // 1. 还原销售库存
//             productStockMapper.restoreSaleStock(skuCode, saleQuantity);
//             log.info("cancel阶段进行, 回滚增加mysql可售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//                 deductStock.getProductStockDO().getSaleStockQuantity(),
//                 deductStock.getProductStockDO().getSaledStockQuantity(),
//                 xid);
//             // 2. 插入一个反做log
//             ProductStockLogDO logDO = this.buildReverseStockLog(orderId, skuCode, saleQuantity);
//             productStockLogMapper.insert(logDO);
//         } finally {
//             lock.unlock();
//         }
//
//         // 移除标识
//         TccResultHolder.removeResult(getClass(), skuCode, xid);
//         log.info("cancel阶段结束, 回滚增加mysql可售库存, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
//     }
//
//     /**
//      * 判断是否发生的空回滚
//      */
//     private Boolean isEmptyRollback() {
//         // 需要查询本地数据库，看是否发生了空回滚
//         return false;
//     }
//
//     /**
//      * 插入空回滚标识
//      */
//     private void insertEmptyRollbackTag() {
//         // 在数据库插入空回滚的标识
//     }
//
//     /**
//      * 构建扣减库存日志
//      */
//     private ProductStockLogDO buildStockLog(ProductStockDO productStockDO, String orderId, String skuCode, BigDecimal saleQuantity) {
//         BigDecimal originSaleStock = productStockDO.getSaleStockQuantity();
//         // 通过扣减log获取原始已销售库存
//         // 1. 查询sku库存最近一笔扣减日志
//         ProductStockLogDO latestLog = productStockLogMapper.selectLastOneBySkuCode(productStockDO.getSkuCode());
//         // 2. 获取原始的已销售库存
//         BigDecimal originSaledStock = latestLog == null ?
//             productStockDO.getSaledStockQuantity() :    // 第一次扣，直接取productStockDO的saledStockQuantity
//             latestLog.getIncreasedSaledStockQuantity(); // 取最近一笔扣减日志的increasedSaledStockQuantity
//
//         ProductStockLogDO logDO = new ProductStockLogDO();
//         logDO.setOrderId(orderId);
//         logDO.setSkuCode(skuCode);
//         logDO.setOriginSaleStockQuantity(originSaleStock);
//         logDO.setOriginSaledStockQuantity(originSaledStock);
//         BigDecimal deductedSaleStockQuantity = originSaleStock.subtract(saleQuantity);
//         logDO.setDeductedSaleStockQuantity(deductedSaleStockQuantity);
//         BigDecimal increasedSaledStockQuantity = originSaledStock.add(saleQuantity);
//         logDO.setIncreasedSaledStockQuantity(increasedSaledStockQuantity);
//         return logDO;
//     }
//
//     /**
//      * 构建逆向扣减库存日志
//      * <p>
//      * eg:
//      * <p>
//      * 1、刚开始的时候，假设sku001的销售库存为1000,已销售库存为0，库存表中的记录如下所示
//      * ｜sku_code ｜ sale_stock ｜ saled_stock｜
//      * ｜ sku001  ｜    1000    ｜    0       ｜
//      * <p>
//      * 2、有一个用户1对sku001下单，下单数量为10，mysql的try执行完后，mysql库存表和库存日志表的记录如下所示
//      * ｜sku_code ｜ sale_stock ｜ saled_stock｜
//      * ｜ sku001  ｜    990     ｜    0      ｜
//      * <p>
//      * ｜sku_code ｜ order_id   ｜ origin_sale_stock ｜ origin_saled_stock｜ deducted_sale_stock ｜ increased_saled_stock ｜
//      * ｜ sku001  ｜    o001    ｜      1000         ｜         0         ｜        990          ｜         10            ｜
//      * <p>
//      * 3、然后在执行redis的try阶段报错，需要回滚，回滚的时候不能简单的将try阶段插入的log给删除，而是要基于最新的log插入一条逆向扣减日志，
//      * 因为rollback是seata server异步调用的，在这个过程中，可能已经有另外的用户已经下单了
//      * <p>
//      * 假设在seata server要对o001订单进行扣库存回滚之前(调用rollback之前)，有一个用户2已经对sku001下单成功了，下单数量为20，此时mysql库存表和库存日志表的记录如下所示
//      * ｜sku_code ｜ sale_stock ｜ saled_stock｜
//      * ｜ sku001  ｜    970     ｜    20      ｜
//      * <p>
//      * ｜sku_code ｜ order_id   ｜ origin_sale_stock ｜ origin_saled_stock｜ deducted_sale_stock ｜ increased_saled_stock ｜
//      * ｜ sku001  ｜    o001    ｜      1000         ｜         0         ｜        990          ｜         10            ｜
//      * ｜ sku001  ｜    o002    ｜      990          ｜         10        ｜        970          ｜         30            ｜
//      * <p>
//      * 4、于是在对o001进行rollback的时候，需要基于最新的扣减库存log进行反做，插入逆向的log：saled_stock+saleQuantity，saled_stock-saleQuantity
//      * rollback执行完毕后，mysql库存表和库存日志表的记录如下所示
//      * ｜sku_code ｜ sale_stock ｜ saled_stock｜
//      * ｜ sku001  ｜    980     ｜    20      ｜
//      * <p>
//      * ｜sku_code ｜ order_id   ｜ origin_sale_stock ｜ origin_saled_stock｜ deducted_sale_stock ｜ increased_saled_stock ｜
//      * ｜ sku001  ｜    o001    ｜      1000         ｜         0         ｜        990          ｜         10            ｜
//      * ｜ sku001  ｜    o002    ｜      990          ｜         10        ｜        970          ｜         30            ｜
//      * ｜ sku001  ｜    o001    ｜      1000         ｜         0         ｜        980          ｜         20            ｜<---逆向扣减日志
//      * <p>
//      * 5、这样，下一笔订单，就可以基于最新的那一条逆向扣减日志去进行扣减扣库存了，
//      * 假设接着有一个用户3已经对sku001下单，并成功，下单数量为30，此时mysql库存表和库存日志表的记录如下所示
//      * ｜sku_code ｜ sale_stock ｜ saled_stock｜
//      * ｜ sku001  ｜    950     ｜    50      ｜
//      * <p>
//      * ｜sku_code ｜ order_id   ｜ origin_sale_stock ｜ origin_saled_stock｜ deducted_sale_stock ｜ increased_saled_stock ｜
//      * ｜ sku001  ｜    o001    ｜      1000         ｜         0         ｜        990          ｜         10            ｜
//      * ｜ sku001  ｜    o002    ｜      990          ｜         10        ｜        970          ｜         30            ｜
//      * ｜ sku001  ｜    o001    ｜      1000         ｜         0         ｜        980          ｜         20            ｜
//      * ｜ sku001  ｜    o003    ｜      980          ｜         20        ｜        950          ｜         50            ｜
//      */
//     private ProductStockLogDO buildReverseStockLog(String orderId, String skuCode, BigDecimal saleQuantity) {
//         //查询sku库存最近一笔扣减日志
//         ProductStockLogDO latestLog = productStockLogMapper.selectLastOneBySkuCode(skuCode);
//
//         ProductStockLogDO logDO = new ProductStockLogDO();
//         logDO.setOrderId(orderId);
//         logDO.setSkuCode(skuCode);
//         BigDecimal originSaleStockQuantity = latestLog.getOriginSaleStockQuantity().add(saleQuantity);
//         logDO.setOriginSaleStockQuantity(originSaleStockQuantity);
//         BigDecimal originSaledStockQuantity = latestLog.getOriginSaledStockQuantity().subtract(saleQuantity);
//         logDO.setOriginSaledStockQuantity(originSaledStockQuantity);
//         BigDecimal deductedSaleStockQuantity = latestLog.getDeductedSaleStockQuantity().add(saleQuantity);
//         logDO.setDeductedSaleStockQuantity(deductedSaleStockQuantity);
//         BigDecimal increasedSaledStockQuantity = latestLog.getIncreasedSaledStockQuantity().subtract(saleQuantity);
//         logDO.setIncreasedSaledStockQuantity(increasedSaledStockQuantity);
//         return logDO;
//     }
// }
