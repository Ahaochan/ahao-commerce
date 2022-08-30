package com.ruyuan.eshop.order.elasticsearch.handler.order;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.order.dao.OrderPaymentDetailDAO;
import com.ruyuan.eshop.order.domain.entity.OrderPaymentDetailDO;
import com.ruyuan.eshop.order.elasticsearch.EsClientService;
import com.ruyuan.eshop.order.elasticsearch.handler.EsAbstractHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单配送信息更新service：
 * <p>
 * 当order_delivery_detail被更新的时候，需要更新OrderListQueryListIndex
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class EsOrderPaymentDetailUpdateHandler extends EsAbstractHandler {

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private EsClientService esClientService;

    @Autowired
    private EsOrderListQueryIndexHandler esOrderListQueryIndexHandler;

    /**
     * 构建OrderListQueryListIndex并同步到es
     * <p>
     */
    public void sync(List<String> orderIds, long timestamp) throws Exception {
        // 1、查询订单支付明细
        List<OrderPaymentDetailDO> orderPaymentDetails = orderPaymentDetailDAO.listByOrderIds(orderIds);
        if (CollectionUtils.isEmpty(orderPaymentDetails)) {
            return;
        }

        // 2、异步构建OrderListQueryListIndex并同步到es
        esOrderListQueryIndexHandler.asyncBuildAndSynToEs(orderIds, timestamp);
    }
}
