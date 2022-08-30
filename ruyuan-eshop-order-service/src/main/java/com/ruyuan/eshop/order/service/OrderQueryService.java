package com.ruyuan.eshop.order.service;

import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.order.domain.dto.OrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.OrderListDTO;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import com.ruyuan.eshop.order.domain.request.OrderDetailRequest;
import com.ruyuan.eshop.order.enums.OrderQueryDataTypeEnums;

/**
 * <p>
 * 订单查询service
 * </p>
 *
 * @author zhonghuashishan
 */
public interface OrderQueryService {

    /**
     * 校验列表查询参数
     *
     * @param query
     */
    void checkQueryParam(OrderQuery query);

    /**
     * 执行列表查询 v1
     *
     * @param query
     */
    PagingInfo<OrderListDTO> executeListQueryV1(OrderQuery query);

    /**
     * 执行列表查询 v2
     *
     *  @param query          入参
     * @param queryDataTypes 查询项
     */
    PagingInfo<OrderDetailDTO> executeListQueryV2(OrderQuery query,
                                                  OrderQueryDataTypeEnums... queryDataTypes) throws Exception;


    /**
     * 查询订单详情 v1
     *
     * @param orderId
     * @return
     */
    OrderDetailDTO orderDetailV1(String orderId);

    /**
     * 查询订单详情 v2
     *
     * @param request
     * @return
     */
    OrderDetailDTO orderDetailV2(OrderDetailRequest request);
}
