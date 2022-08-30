package com.ruyuan.eshop.order.dao;

import com.ruyuan.eshop.common.dao.BaseDAO;
import com.ruyuan.eshop.common.exception.BaseBizException;
import com.ruyuan.eshop.order.domain.entity.OrderAutoNoDO;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.mapper.OrderAutoNoMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 订单编号表 DAO
 * </p>
 *
 * @author zhonghuashishan
 */
@Repository
public class OrderAutoNoDAO extends BaseDAO<OrderAutoNoMapper, OrderAutoNoDO> {

    @Transactional(rollbackFor = Exception.class)
    public OrderAutoNoDO updateMaxIdAndGet(String bizTag) {
        int ret = baseMapper.updateMaxId(bizTag);
        if (ret != 1) {
            throw new BaseBizException(OrderErrorCodeEnum.ORDER_AUTO_NO_GEN_ERROR);
        }
        return baseMapper.findByBizTag(bizTag);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderAutoNoDO updateMaxIdByDynamicStepAndGet(String bizTag, int nextStep) {
        int ret = baseMapper.updateMaxIdByDynamicStep(bizTag, nextStep);
        if (ret != 1) {
            throw new BaseBizException(OrderErrorCodeEnum.ORDER_AUTO_NO_GEN_ERROR);
        }
        return baseMapper.findByBizTag(bizTag);
    }
}