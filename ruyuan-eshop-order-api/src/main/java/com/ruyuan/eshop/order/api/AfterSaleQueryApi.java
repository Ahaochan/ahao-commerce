package com.ruyuan.eshop.order.api;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.order.domain.dto.AfterSaleItemDTO;
import com.ruyuan.eshop.order.domain.dto.AfterSaleOrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.AfterSaleOrderListDTO;
import com.ruyuan.eshop.order.domain.query.AfterSaleQuery;
import com.ruyuan.eshop.order.domain.request.AfterSaleDetailRequest;

/**
 * 订单中心-售后查询业务接口
 *
 * @author zhonghuashishan
 * @version 1.0
 */
public interface AfterSaleQueryApi {

    /**
     * 查询售后列表v1
     *
     * @param query
     * @return
     */
    JsonResult<PagingInfo<AfterSaleOrderListDTO>> listAfterSalesV1(AfterSaleQuery query);

    /**
     * 查询售后列表v2
     *
     * @param query     入参
     * @return
     */
    JsonResult<PagingInfo<AfterSaleOrderDetailDTO>> listAfterSalesV2(AfterSaleQuery query);

    /**
     * 查询售后单详情 v1
     *
     * @param afterSaleId
     * @return
     */
    JsonResult<AfterSaleOrderDetailDTO> afterSaleDetailV1(String afterSaleId);

    /**
     * 查询售后单详情 v2
     *
     * @param request
     * @return
     */
    JsonResult<AfterSaleOrderDetailDTO> afterSaleDetailV2(AfterSaleDetailRequest request);

    /**
     * 根据订单id和skuCode查询订单条目
     */
    AfterSaleItemDTO getOrderItemByOrderIdAndSkuCode(String orderId, String skuCode);

}