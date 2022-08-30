package com.ruyuan.eshop.order.converter;

import com.ruyuan.eshop.order.elasticsearch.query.AfterSaleListQueryIndex;
import com.ruyuan.eshop.order.elasticsearch.query.OrderListQueryIndex;
import org.mapstruct.Mapper;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface EsEntityConverter {


    /**
     * 对象转换
     *
     * @param queryIndex 对象
     * @return 对象
     */
    OrderListQueryIndex copyOrderListQueryIndex(OrderListQueryIndex queryIndex);


    /**
     * 对象转换
     *
     * @param queryIndex 对象
     * @return 对象
     */
    AfterSaleListQueryIndex copyAfterSaleListQueryIndex(AfterSaleListQueryIndex queryIndex);
}
