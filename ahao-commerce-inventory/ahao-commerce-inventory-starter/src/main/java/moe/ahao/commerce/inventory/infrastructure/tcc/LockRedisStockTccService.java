// package moe.ahao.commerce.inventory.infrastructure.tcc;
//
// import io.seata.rm.tcc.api.BusinessActionContext;
// import io.seata.rm.tcc.api.BusinessActionContextParameter;
// import io.seata.rm.tcc.api.LocalTCC;
// import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
//
// /**
//  * 锁定redis库存 Seata TCC模式 service
//  */
// @LocalTCC
// public interface LockRedisStockTccService {
//     /**
//      * 一阶段方法：扣减销售库存（saleStockQuantity-saleQuantity）
//      */
//     @TwoPhaseBusinessAction(name = "lockRedisStockTccService", commitMethod = "commit", rollbackMethod = "rollback")
//     boolean deductStock(BusinessActionContext actionContext,
//                         @BusinessActionContextParameter(paramName = "deductStock") DeductStockDTO deductStock,
//                         @BusinessActionContextParameter(paramName = "traceId") String traceId);
//
//     /**
//      * 二阶段方法：增加已销售库存（saledStockQuantity+saleQuantity）
//      */
//     void commit(BusinessActionContext actionContext);
//
//     /**
//      * 回滚：增加销售库存（saleStockQuantity+saleQuantity）
//      */
//     void rollback(BusinessActionContext actionContext);
// }
