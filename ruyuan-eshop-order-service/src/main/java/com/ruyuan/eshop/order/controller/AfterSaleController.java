package com.ruyuan.eshop.order.controller;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.order.api.AfterSaleApi;
import com.ruyuan.eshop.order.api.AfterSaleQueryApi;
import com.ruyuan.eshop.order.domain.dto.AfterSaleOrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.AfterSaleOrderListDTO;
import com.ruyuan.eshop.order.domain.dto.LackDTO;
import com.ruyuan.eshop.order.domain.query.AfterSaleQuery;
import com.ruyuan.eshop.order.domain.request.*;
import com.ruyuan.eshop.order.service.OrderAfterSaleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单售后流程controller
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@RestController
@RequestMapping("/afterSale")
@Slf4j
public class AfterSaleController {

    @Autowired
    private OrderAfterSaleService orderAfterSaleService;

    @DubboReference(version = "1.0.0", retries = 0)
    private AfterSaleApi afterSaleApi;

    @DubboReference(version = "1.0.0")
    private AfterSaleQueryApi afterSaleQueryApi;

    /**
     * 用户手动取消订单
     */
    @PostMapping("/cancelOrder")
    public JsonResult<Boolean> cancelOrder(@RequestBody CancelOrderRequest cancelOrderRequest) {
        return orderAfterSaleService.cancelOrder(cancelOrderRequest);
    }

    /**
     * 用户发起退货售后
     */
    @PostMapping("/applyAfterSale")
    public JsonResult<Boolean> applyAfterSale(@RequestBody ReturnGoodsOrderRequest returnGoodsOrderRequest) {
        return orderAfterSaleService.processApplyAfterSale(returnGoodsOrderRequest);
    }

    /**
     * 缺品请求
     */
    @PostMapping("/lackItem")
    public JsonResult<LackDTO> lackItem(@RequestBody LackRequest request) {
        return afterSaleApi.lackItem(request);
    }

    /**
     * 用户撤销售后申请
     */
    @PostMapping("/revokeAfterSale")
    public JsonResult<Boolean> revokeAfterSale(@RequestBody RevokeAfterSaleRequest request) {
        return afterSaleApi.revokeAfterSale(request);
    }

    /**
     * 查询售后列表 v1
     */
    @PostMapping("/v1/listAfterSales")
    public JsonResult<PagingInfo<AfterSaleOrderListDTO>> listAfterSalesV1(@RequestBody AfterSaleQuery query) {
        return afterSaleQueryApi.listAfterSalesV1(query);
    }

    /**
     * 查询售后列表 v2
     */
    @PostMapping("/v2/listAfterSales")
    public JsonResult<PagingInfo<AfterSaleOrderDetailDTO>> listAfterSalesV2(@RequestBody AfterSaleQuery query) {
        return afterSaleQueryApi.listAfterSalesV2(query);
    }

    /**
     * 查询售后单详情 v1
     */
    @GetMapping("/v1/afterSaleDetail")
    public JsonResult<AfterSaleOrderDetailDTO> afterSaleDetail(String afterSaleId) {
        return afterSaleQueryApi.afterSaleDetailV1(afterSaleId);
    }

    /**
     * 查询售后单详情 v2
     */
    @PostMapping("/v2/afterSaleDetail")
    public JsonResult<AfterSaleOrderDetailDTO> afterSaleDetailV2(@RequestBody AfterSaleDetailRequest request) {
        return afterSaleQueryApi.afterSaleDetailV2(request);
    }
}
