package com.ruyuan.eshop.order.elasticsearch.handler.order;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.elasticsearch.EsClientService;
import com.ruyuan.eshop.order.elasticsearch.handler.EsAbstractHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单更新service：
 * <p>
 * 当order_info被更新的时候，需要更新OrderListQueryListIndex
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class EsOrderUpdateHandler extends EsAbstractHandler {

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private EsClientService esClientService;

    @Autowired
    private EsOrderListQueryIndexHandler esOrderListQueryIndexHandler;

    /**
     * 将订单同步至es
     */
    public void sync(List<String> orderIds, long timestamp) throws Exception {
        // 1、查询订单
        List<OrderInfoDO> orders = orderInfoDAO.listByOrderIds(orderIds);
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }

        // 2、异步构建OrderQueryListIndex，并同步到es里面去
        esOrderListQueryIndexHandler.asyncBuildAndSynToEs(orders, orderIds, timestamp);
    }
}
