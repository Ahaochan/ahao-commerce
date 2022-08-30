package com.ruyuan.eshop.order.dao;

import com.ruyuan.eshop.order.domain.entity.OrderSnapshotDO;

import java.util.List;

/**
 * 订单快照表 DAO
 *
 * @author zhonghuashishan
 */
public interface OrderSnapshotDAO {

    /**
     * 查询order snapshot
     */
    List<OrderSnapshotDO> queryOrderSnapshotByOrderId(String orderId,List<String> rowKeyPrefixList);

    /**
     * 查询order snapshot
     */
    List<OrderSnapshotDO> queryOrderSnapshotByOrderIds(List<String> orderIds);

    /**
     * 批量插入操作
     *
     * @param orderSnapshotDOList 要插入快照的集合
     */
    void batchSave(List<OrderSnapshotDO> orderSnapshotDOList,List<String> orderSnapshotRowKeyPrefixList);
}
