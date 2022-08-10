// package moe.ahao.commerce.inventory.infrastructure.tcc.impl;
//
// import com.alibaba.fastjson.JSONObject;
// import io.seata.rm.tcc.api.BusinessActionContext;
// import lombok.extern.slf4j.Slf4j;
// import moe.ahao.commerce.common.infrastructure.utils.MdcUtil;
// import moe.ahao.commerce.inventory.infrastructure.cache.LuaScript;
// import moe.ahao.commerce.inventory.infrastructure.cache.RedisCacheSupport;
// import moe.ahao.commerce.inventory.infrastructure.tcc.DeductStockDTO;
// import moe.ahao.commerce.inventory.infrastructure.tcc.LockRedisStockTccService;
// import moe.ahao.commerce.inventory.infrastructure.tcc.TccResultHolder;
// import moe.ahao.util.spring.redis.RedisHelper;
// import org.springframework.data.redis.core.script.DefaultRedisScript;
// import org.springframework.stereotype.Service;
//
// import java.math.BigDecimal;
// import java.util.Arrays;
//
// @Slf4j
// @Service
// public class LockRedisStockTccServiceImpl implements LockRedisStockTccService {
//
//     @Override
//     public boolean deductStock(BusinessActionContext actionContext, DeductStockDTO deductStock, String traceId) {
//         String xid = actionContext.getXid();
//         String orderId = deductStock.getOrderId();
//         String skuCode = deductStock.getSkuCode();
//         BigDecimal saleQuantity = deductStock.getSaleQuantity();
//
//         // 标识try阶段开始执行
//         log.info("prepare阶段标记start, 扣减redis可售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//             deductStock.getProductStockDO().getSaleStockQuantity(),
//             deductStock.getProductStockDO().getSaledStockQuantity(),
//             xid);
//         TccResultHolder.tagTryStart(getClass(), skuCode, xid);
//
//         // 悬挂问题: rollback接口先进行了空回滚, try接口才执行, 导致try接口预留的资源无法被confirm和cancel
//         // 解决方案: 当出现空回滚时, 在数据库中插一条记录, 在try这里判断一下
//         if (this.isEmptyRollback()) {
//             log.info("prepare阶段出现悬挂, 扣减redis可售库存, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
//             return false;
//         }
//
//         String luaScript = LuaScript.DEDUCT_SALE_STOCK;
//         String saleStockKey = RedisCacheSupport.SALE_STOCK;
//         String productStockKey = RedisCacheSupport.buildProductStockKey(skuCode);
//         Long result = RedisHelper.getStringRedisTemplate().execute(new DefaultRedisScript<>(luaScript, Long.class),
//             Arrays.asList(productStockKey, saleStockKey), String.valueOf(saleQuantity));
//         log.info("prepare阶段进行, 扣减redis可售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//             deductStock.getProductStockDO().getSaleStockQuantity().subtract(saleQuantity),
//             deductStock.getProductStockDO().getSaledStockQuantity(),
//             xid);
//
//         // 标识try阶段执行成功
//         boolean success = result != null && result > 0;
//         if (success) {
//             TccResultHolder.tagTrySuccess(getClass(), skuCode, xid);
//             log.info("prepare阶段结束, 扣减redis可售库存, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
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
//         log.info("confirm阶段开始, 增加redis已售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//             deductStock.getProductStockDO().getSaleStockQuantity().subtract(saleQuantity),
//             deductStock.getProductStockDO().getSaledStockQuantity(),
//             xid);
//
//         // 当出现网络异常或者TC Server异常时, 会出现重复调用commit阶段的情况, 所以需要进行幂等判断
//         if (!TccResultHolder.isTrySuccess(getClass(), skuCode, xid)) {
//             log.info("confirm阶段幂等性校验失败, 增加redis已售库存, 已经执行过commit阶段, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
//             return;
//         }
//
//         String luaScript = LuaScript.INCREASE_SALED_STOCK;
//         String saledStockKey = RedisCacheSupport.SALED_STOCK;
//         String productStockKey = RedisCacheSupport.buildProductStockKey(skuCode);
//         RedisHelper.getStringRedisTemplate().execute(new DefaultRedisScript<>(luaScript, Long.class),
//             Arrays.asList(productStockKey, saledStockKey), String.valueOf(saleQuantity));
//         log.info("confirm阶段进行, 增加redis已售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//             deductStock.getProductStockDO().getSaleStockQuantity().subtract(saleQuantity),
//             deductStock.getProductStockDO().getSaledStockQuantity().add(saleQuantity),
//             xid);
//
//         // 移除标识
//         TccResultHolder.removeResult(getClass(), skuCode, xid);
//         log.info("confirm阶段结束, 增加redis已售库存, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
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
//         log.info("cancel阶段开始, 回滚增加redis可售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
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
//         String luaScript = LuaScript.RESTORE_SALE_STOCK;
//         String saleStockKey = RedisCacheSupport.SALE_STOCK;
//         String productStockKey = RedisCacheSupport.buildProductStockKey(skuCode);
//         RedisHelper.getStringRedisTemplate().execute(new DefaultRedisScript<>(luaScript, Long.class),
//             Arrays.asList(productStockKey, saleStockKey), String.valueOf(saleQuantity));
//         log.info("cancel阶段进行, 回滚增加redis可售库存, orderId:{}, skuCode:{}, 可售库存:{}, 已售库存:{}, xid:{}", orderId, skuCode,
//             deductStock.getProductStockDO().getSaleStockQuantity(),
//             deductStock.getProductStockDO().getSaledStockQuantity(),
//             xid);
//
//         // 移除标识
//         TccResultHolder.removeResult(getClass(), skuCode, xid);
//         log.info("cancel阶段结束, 回滚增加redis可售库存, orderId:{}, skuCode:{}, xid:{}", orderId, skuCode, xid);
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
// }
