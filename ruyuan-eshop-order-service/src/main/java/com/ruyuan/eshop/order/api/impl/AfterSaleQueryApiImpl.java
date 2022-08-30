package com.ruyuan.eshop.order.api.impl;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.enums.AfterSaleItemTypeEnum;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.order.api.AfterSaleQueryApi;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.AfterSaleItemDAO;
import com.ruyuan.eshop.order.domain.dto.AfterSaleItemDTO;
import com.ruyuan.eshop.order.domain.dto.AfterSaleOrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.AfterSaleOrderListDTO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleItemDO;
import com.ruyuan.eshop.order.domain.query.AfterSaleQuery;
import com.ruyuan.eshop.order.domain.request.AfterSaleDetailRequest;
import com.ruyuan.eshop.order.exception.OrderBizException;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.service.AfterSaleQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单中心-售后查询业务接口
 *
 * @author zhonghuashishan
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = AfterSaleQueryApi.class)
public class AfterSaleQueryApiImpl implements AfterSaleQueryApi {

    @Autowired
    private AfterSaleQueryService afterSaleQueryService;

    @Autowired
    private OrderConverter orderConverter;

    @Autowired
    private AfterSaleItemDAO afterSaleItemDAO;

    @Override
    public JsonResult<PagingInfo<AfterSaleOrderListDTO>> listAfterSalesV1(AfterSaleQuery query) {
        try {
            //1、参数校验
            afterSaleQueryService.checkQueryParam(query);

            //2、查询
            return JsonResult.buildSuccess(afterSaleQueryService.executeListQueryV1(query));

        } catch (OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public JsonResult<PagingInfo<AfterSaleOrderDetailDTO>> listAfterSalesV2(AfterSaleQuery query) {
        try {
            //1、参数校验
            afterSaleQueryService.checkQueryParam(query);

            //2、查询
            return JsonResult.buildSuccess(afterSaleQueryService.executeListQueryV2(query, query.getQueryDataTypes()));

        } catch (OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public JsonResult<AfterSaleOrderDetailDTO> afterSaleDetailV1(String afterSaleId) {
        try {
            //1、参数校验
            ParamCheckUtil.checkObjectNonNull(afterSaleId, OrderErrorCodeEnum.AFTER_SALE_ID_IS_NULL);

            //2、查询
            return JsonResult.buildSuccess(afterSaleQueryService.afterSaleDetailV1(afterSaleId));

        } catch (OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public JsonResult<AfterSaleOrderDetailDTO> afterSaleDetailV2(AfterSaleDetailRequest request) {
        try {
            //1、参数校验
            ParamCheckUtil.checkObjectNonNull(request.getAfterSaleId(), OrderErrorCodeEnum.AFTER_SALE_ID_IS_NULL);

            //2、查询
            return JsonResult.buildSuccess(afterSaleQueryService
                    .afterSaleDetailV2(request.getAfterSaleId(), request.getQueryDataTypes()));

        } catch (OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public AfterSaleItemDTO getOrderItemByOrderIdAndSkuCode(String orderId, String skuCode) {
        List<AfterSaleItemDO> afterSaleItemDOList = afterSaleItemDAO.getOrderIdAndSkuCode(orderId, skuCode);
        //  从售后单list中选出唯一的条目售后单
        List<AfterSaleItemDO> resultList = afterSaleItemDOList.stream()
                .filter(afterSaleItemDO ->
                        AfterSaleItemTypeEnum.AFTER_SALE_ORDER_ITEM.getCode().equals(afterSaleItemDO.getAfterSaleItemType()))
                .collect(Collectors.toList());
        AfterSaleItemDO afterSaleItemDO = resultList.get(0);

        return orderConverter.convertAfterSaleItemDTO(afterSaleItemDO);
    }
}