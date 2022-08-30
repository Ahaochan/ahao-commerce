package com.ruyuan.eshop.order.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.common.dao.BaseDAO;
import com.ruyuan.eshop.order.domain.entity.OrderOperateLogDO;
import com.ruyuan.eshop.order.mapper.OrderOperateLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单操作日志表 mysql DAO
 *
 * @author zhonghuashishan
 */
@Slf4j
@Repository
public class OrderOperateLogMysqlDAO extends BaseDAO<OrderOperateLogMapper, OrderOperateLogDO> implements OrderOperateLogDAO {

    @Autowired
    private OrderOperateLogMapper orderOperateLogMapper;

    /**
     * 插入订单操作日志
     *
     * @param log 操作日志DO
     * @return 结果
     */
    @Override
    public boolean save(OrderOperateLogDO log) {
        return super.save(log);
    }

    /**
     * 批量插入订单操作日志
     *
     * @param logList 操作日志集合
     */
    @Override
    public void batchSave(List<OrderOperateLogDO> logList) {
        if (CollectionUtils.isEmpty(logList)) {
            return;
        }
        this.saveBatch(logList);
    }

    /**
     * 根据订单id查询订单操作日志
     *
     * @param orderId 订单id
     * @return 订单操作日志列表
     */
    @Override
    public List<OrderOperateLogDO> listByOrderId(String orderId) {
        LambdaQueryWrapper<OrderOperateLogDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderOperateLogDO::getOrderId, orderId);
        return list(queryWrapper);
    }

    @Override
    public List<OrderOperateLogDO> listByOrderIds(List<String> orderIds) {
        LambdaQueryWrapper<OrderOperateLogDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(OrderOperateLogDO::getOrderId, orderIds);
        return list(queryWrapper);
    }
}
