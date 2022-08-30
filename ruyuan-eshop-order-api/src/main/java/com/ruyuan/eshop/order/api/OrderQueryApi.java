package com.ruyuan.eshop.order.api;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.order.domain.dto.OrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.OrderItemDTO;
import com.ruyuan.eshop.order.domain.dto.OrderListDTO;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import com.ruyuan.eshop.order.domain.request.OrderDetailRequest;

import java.util.List;

/**
 * 订单中心-订单查询业务接口
 *
 * @author zhonghuashishan
 */
public interface OrderQueryApi {

    /**
     * 查询订单列表 v1
     */
    JsonResult<PagingInfo<OrderListDTO>> listOrdersV1(OrderQuery query);

    /**
     * 查询订单列表 v2
     *  @param query      查询入参
     *
     */
    JsonResult<PagingInfo<OrderDetailDTO>> listOrdersV2(OrderQuery query);


    /**
     * 查询订单详情 v1
     */
    JsonResult<OrderDetailDTO> orderDetailV1(String orderId);

    /**
     * 查询订单详情 v2
     */
    JsonResult<OrderDetailDTO> orderDetailV2(OrderDetailRequest request);

    /**
     * 根据订单id查询订单条目
     */
    List<OrderItemDTO> getOrderItemByOrderId(String orderId);

    /**
     * 根据用户id查询订单id
     */
    String getOrderIdByUserId(String userId);
}
