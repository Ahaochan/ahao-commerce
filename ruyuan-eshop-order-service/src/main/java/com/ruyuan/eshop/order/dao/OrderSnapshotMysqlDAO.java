package com.ruyuan.eshop.order.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.common.dao.BaseDAO;
import com.ruyuan.eshop.order.domain.entity.OrderSnapshotDO;
import com.ruyuan.eshop.order.mapper.OrderSnapshotMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单快照表 mysql DAO
 *
 * @author zhonghuashishan
 */
@Slf4j
@Repository
public class OrderSnapshotMysqlDAO extends BaseDAO<OrderSnapshotMapper, OrderSnapshotDO> implements OrderSnapshotDAO {

    @Autowired
    private OrderSnapshotMapper orderSnapshotMapper;

    @Override
    public List<OrderSnapshotDO> queryOrderSnapshotByOrderId(String orderId, List<String> rowKeyPrefixList) {
        LambdaQueryWrapper<OrderSnapshotDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderSnapshotDO::getOrderId, orderId);
        return list(queryWrapper);
    }

    @Override
    public List<OrderSnapshotDO> queryOrderSnapshotByOrderIds(List<String> orderIds) {
        LambdaQueryWrapper<OrderSnapshotDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(OrderSnapshotDO::getOrderId, orderIds);
        return list(queryWrapper);
    }

    @Override
    public void batchSave(List<OrderSnapshotDO> orderSnapshotDOList,List<String> orderSnapshotRowKeyPrefixList) {
        if(CollectionUtils.isEmpty(orderSnapshotDOList)) {
            return;
        }
        this.saveBatch(orderSnapshotDOList);
    }
}
