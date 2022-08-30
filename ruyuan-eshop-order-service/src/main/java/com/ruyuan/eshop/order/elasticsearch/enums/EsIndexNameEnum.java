package com.ruyuan.eshop.order.elasticsearch.enums;

import com.ruyuan.eshop.order.domain.entity.*;
import com.ruyuan.eshop.order.elasticsearch.query.AfterSaleListQueryIndex;
import com.ruyuan.eshop.order.elasticsearch.query.OrderListQueryIndex;
import lombok.Getter;

/**
 * es index name枚举
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Getter
public enum EsIndexNameEnum {

    /**
     * 订单列表查询es index
     */
    ORDER_LIST_QUERY_INDEX("es_order_list_query_index", OrderListQueryIndex.class),

    /**
     * 售后单列表查询es index
     */
    AFTER_SALE_LIST_QUERY_INDEX("es_after_sale_list_query_index", AfterSaleListQueryIndex.class),

    ;

    EsIndexNameEnum(String indexName, Class<?> clazz) {
        this.name = indexName;
        this.clazz = clazz;
    }

    private String name;
    private Class<?> clazz;

    public static EsIndexNameEnum parseName(String indexName) {
        for (EsIndexNameEnum element : EsIndexNameEnum.values()) {
            if (indexName.equals(element.getName())) {
                return element;
            }
        }
        return null;
    }
}
