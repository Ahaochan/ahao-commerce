package com.ruyuan.eshop.order.elasticsearch.handler.order;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.order.dao.*;
import com.ruyuan.eshop.order.domain.entity.OrderDeliveryDetailDO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.entity.OrderItemDO;
import com.ruyuan.eshop.order.domain.entity.OrderPaymentDetailDO;
import com.ruyuan.eshop.order.elasticsearch.EsClientService;
import com.ruyuan.eshop.order.elasticsearch.handler.EsAbstractHandler;
import com.ruyuan.eshop.order.elasticsearch.query.OrderListQueryIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单新增service：
 * <p>
 * 当order_info被创建的时候，需要构建OrderListQueryListIndex
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class EsOrderInfoInsertHandler extends EsAbstractHandler {

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private OrderAmountDAO orderAmountDAO;

    @Autowired
    private OrderAmountDetailDAO orderAmountDetailDAO;

    @Autowired
    private EsClientService esClientService;

    @Autowired
    private EsOrderListQueryIndexHandler esOrderListQueryIndexHandler;


    /**
     * 构建OrderListQueryListIndex并同步到es
     */
    public void sync(List<String> orderIds, long timestamp) throws Exception {

        // 查询订单
        List<OrderInfoDO> orders = orderInfoDAO.listByOrderIds(orderIds);
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }
        sync(orders, orderIds, timestamp);
    }


    /**
     * 构建OrderListQueryListIndex并同步到es
     */
    public void sync(List<OrderInfoDO> orders, List<String> orderIds, long timestamp) throws Exception {

        // 1、查询订单条目
        List<OrderItemDO> orderItems = orderItemDAO.listByOrderIds(orderIds);

        // 2、查询订单配送信息
        List<OrderDeliveryDetailDO> orderDeliveryDetails = orderDeliveryDetailDAO.listByOrderIds(orderIds);

        // 3、查询订单支付信息
        List<OrderPaymentDetailDO> orderPaymentDetails = orderPaymentDetailDAO.listByOrderIds(orderIds);

        // 4、构建orderListQueryIndex并同步到es
        List<OrderListQueryIndex> orderListQueryIndices =
                esOrderListQueryIndexHandler.buildOrderListQueryIndex(orders, orderDeliveryDetails, orderItems, orderPaymentDetails);
        esOrderListQueryIndexHandler.sycToEs(orderListQueryIndices);
    }
}
