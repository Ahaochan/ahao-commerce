package com.ruyuan.eshop.order.elasticsearch.handler;

import com.ruyuan.eshop.order.elasticsearch.query.AfterSaleListQueryIndex;
import com.ruyuan.eshop.order.elasticsearch.query.OrderListQueryIndex;

import java.util.List;

/**
 * es抽象handler
 *
 * @author zhonghuashishan
 * @version 1.0
 */
public abstract class EsAbstractHandler {

    public void setEsIdOfOrderListQueryIndex(List<OrderListQueryIndex> list) {
        list.forEach(e -> e.setEsId(e.getOrderItemId() + "_" + e.getPayType()));
    }

    public void setEsIdOfAfterSaleListQueryIndex(List<AfterSaleListQueryIndex> list) {
        list.forEach(e -> e.setEsId(e.getAfterSaleId() + "_" + e.getSkuCode()));
    }

}
