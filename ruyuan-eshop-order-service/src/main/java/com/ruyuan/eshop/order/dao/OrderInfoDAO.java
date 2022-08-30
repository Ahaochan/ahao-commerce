package com.ruyuan.eshop.order.dao;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyuan.eshop.common.dao.BaseDAO;
import com.ruyuan.eshop.common.enums.DeleteStatusEnum;
import com.ruyuan.eshop.order.domain.dto.OrderExtJsonDTO;
import com.ruyuan.eshop.order.domain.dto.OrderListDTO;
import com.ruyuan.eshop.order.domain.dto.OrderListQueryDTO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.mapper.OrderInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单DAO
 * </p>
 *
 * @author zhonghuashishan
 */
@Repository
@Slf4j
public class OrderInfoDAO extends BaseDAO<OrderInfoMapper, OrderInfoDO> {

    @Autowired(required = false)
    private OrderInfoMapper orderInfoMapper;

    /**
     * 根据订单号查询订单号
     */
    public List<OrderInfoDO> listByOrderIds(List<String> orderIds) {
        LambdaQueryWrapper<OrderInfoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(OrderInfoDO::getOrderId, orderIds);
        return list(queryWrapper);
    }

    /**
     * 软删除订单
     *
     * @param orderIds 订单Ids
     */
    public void softRemoveOrders(List<String> orderIds) {
        LambdaUpdateWrapper<OrderInfoDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(OrderInfoDO::getDeleteStatus, DeleteStatusEnum.YES.getCode())
                .in(OrderInfoDO::getOrderId, orderIds);
        this.update(updateWrapper);
    }

    /**
     * 根据订单号查询订单
     */
    public OrderInfoDO getByOrderId(String orderId) {
        LambdaQueryWrapper<OrderInfoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfoDO::getOrderId, orderId);
        List<OrderInfoDO> list = list(queryWrapper);
        if(CollectionUtils.isNotEmpty(list)) {
            return list.get(0);
        }
        return null;
    }

    /**
     * 根据订单号更新订单
     */
    public boolean updateByOrderId(OrderInfoDO orderInfoDO, String orderId) {
        LambdaUpdateWrapper<OrderInfoDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderInfoDO::getOrderId, orderId);
        return update(orderInfoDO, updateWrapper);
    }


    /**
     * 根据订单号或者父单号更新订单
     */
    public boolean updateByOrderIdOrParentId(OrderInfoDO orderInfoDO, String orderId) {
        LambdaUpdateWrapper<OrderInfoDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderInfoDO::getOrderId, orderId)
                .or()
                .eq(OrderInfoDO::getParentOrderId, orderId);
        return update(orderInfoDO, updateWrapper);
    }


    /**
     * 根据父订单号更新订单
     */
    public boolean updateBatchByOrderIds(OrderInfoDO orderInfoDO, List<String> orderIds) {
        LambdaUpdateWrapper<OrderInfoDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(OrderInfoDO::getOrderId, orderIds);
        return update(orderInfoDO, updateWrapper);
    }

    /**
     * 统计子订单数量
     */
    public List<String> listSubOrderIds(String orderId) {
        LambdaQueryWrapper<OrderInfoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(OrderInfoDO::getOrderId);
        queryWrapper.eq(OrderInfoDO::getParentOrderId, orderId);
        return list(queryWrapper).stream().map(OrderInfoDO::getOrderId).collect(Collectors.toList());
    }

    /**
     * 根据父订单号查询子订单号
     */
    public List<OrderInfoDO> listByParentOrderId(String orderId) {
        LambdaQueryWrapper<OrderInfoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfoDO::getParentOrderId, orderId);
        return list(queryWrapper);
    }

    /**
     * 根据条件分页查询订单列表
     */
    public Page<OrderListDTO> listByPage(OrderListQueryDTO query) {
        Page<OrderListDTO> page = new Page<>(query.getPageNo(), query.getPageSize());
        return orderInfoMapper.listByPage(page, query);
    }

    /**
     * 更新订单扩展信息
     */
    public boolean updateOrderExtJson(String orderId, OrderExtJsonDTO extJson) {
        String extJsonStr = JSONObject.toJSONString(extJson);
        LambdaUpdateWrapper<OrderInfoDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(OrderInfoDO::getExtJson, extJsonStr)
                .eq(OrderInfoDO::getOrderId, orderId);
        return this.update(updateWrapper);
    }


    public boolean updateOrderStatus(String orderId, Integer fromStatus, Integer toStatus) {
        LambdaUpdateWrapper<OrderInfoDO> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.set(OrderInfoDO::getOrderStatus, toStatus)
                .eq(OrderInfoDO::getOrderId, orderId)
                .eq(OrderInfoDO::getOrderStatus, fromStatus);

        return update(updateWrapper);
    }


    /**
     * 根据orderId查询全部主单和子单
     */
    public List<OrderInfoDO> getAllByOrderId(String orderId) {
        LambdaQueryWrapper<OrderInfoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfoDO::getOrderId, orderId)
                .or()
                .eq(OrderInfoDO::getParentOrderId, orderId);
        return list(queryWrapper);
    }

    /**
     * 分页查询订单
     * <p>
     * 仅适用于单库单表，且主键id连续的情况，解决深分页的问题
     *
     * @param offset 偏移量，从0开始
     * @param limit  limit
     * @return 结果
     */
    public List<OrderInfoDO> getPageBy(long offset, long limit) {
        return new LambdaQueryChainWrapper<>(orderInfoMapper)
                .ge(OrderInfoDO::getId, offset + 1).le(OrderInfoDO::getId, offset + limit).list();
    }

    /**
     * 根据userId查询order list
     */
    public List<OrderInfoDO> getOrderListByUserId(String userId) {
        LambdaQueryWrapper<OrderInfoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfoDO::getUserId, userId);
        return list(queryWrapper);
    }
}
