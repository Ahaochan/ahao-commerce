package com.ruyuan.eshop.order.dao;

import com.ruyuan.eshop.order.domain.entity.OrderOperateLogDO;

import java.util.List;

/**
 * 订单操作日志表 DAO
 *
 * @author zhonghuashishan
 */
public interface OrderOperateLogDAO {

    /**
     * 插入订单操作日志
     *
     * @param log 操作日志DO
     * @return 结果
     */
    boolean save(OrderOperateLogDO log);

    /**
     * 批量插入订单操作日志
     *
     * @param logList 操作日志集合
     */
    void batchSave(List<OrderOperateLogDO> logList);

    /**
     * 根据订单id查询订单操作日志
     *
     * @param orderId 订单id
     * @return 订单操作日志列表
     */
    List<OrderOperateLogDO> listByOrderId(String orderId);

    /**
     * 根据订单id查询订单操作日志
     *
     * @param orderIds 订单ids
     * @return 订单操作日志列表
     */
    List<OrderOperateLogDO> listByOrderIds(List<String> orderIds);
}
