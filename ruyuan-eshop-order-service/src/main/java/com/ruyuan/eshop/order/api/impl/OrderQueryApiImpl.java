package com.ruyuan.eshop.order.api.impl;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.order.api.OrderQueryApi;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.dao.OrderItemDAO;
import com.ruyuan.eshop.order.domain.dto.OrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.OrderItemDTO;
import com.ruyuan.eshop.order.domain.dto.OrderListDTO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.entity.OrderItemDO;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import com.ruyuan.eshop.order.domain.request.OrderDetailRequest;
import com.ruyuan.eshop.order.exception.OrderBizException;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.service.OrderQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


/**
 * 订单中心-订单查询业务接口
 *
 * @author zhonghuashishan
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = OrderQueryApi.class)
public class OrderQueryApiImpl implements OrderQueryApi {

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private OrderConverter orderConverter;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    /**
     * 查询订单列表
     */
    @Override
    public JsonResult<PagingInfo<OrderListDTO>> listOrdersV1(OrderQuery query) {
        try {
            // 1、参数校验
            orderQueryService.checkQueryParam(query);

            // 2、查询
            return JsonResult.buildSuccess(orderQueryService.executeListQueryV1(query));

        } catch (OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public JsonResult<PagingInfo<OrderDetailDTO>> listOrdersV2(OrderQuery query) {
        try {
            // 1、参数校验
            orderQueryService.checkQueryParam(query);

            // 2、查询
            return JsonResult.buildSuccess(orderQueryService.executeListQueryV2(query));

        } catch (OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }


    /**
     * 查询订单详情
     */
    @Override
    public JsonResult<OrderDetailDTO> orderDetailV1(String orderId) {
        try {
            //1、参数校验
            ParamCheckUtil.checkStringNonEmpty(orderId, OrderErrorCodeEnum.ORDER_ID_IS_NULL);

            //2、查询
            return JsonResult.buildSuccess(orderQueryService.orderDetailV1(orderId));

        } catch (OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public JsonResult<OrderDetailDTO> orderDetailV2(OrderDetailRequest request) {
        try {
            //1、参数校验
            ParamCheckUtil.checkStringNonEmpty(request.getOrderId(), OrderErrorCodeEnum.ORDER_ID_IS_NULL);

            //2、查询
            return JsonResult.buildSuccess(orderQueryService.orderDetailV2(request));

        } catch (OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public List<OrderItemDTO> getOrderItemByOrderId(String orderId) {
        List<OrderItemDTO> orderItemDTOList = new ArrayList<>();

        List<OrderItemDO> orderItemDOList = orderItemDAO.listByOrderId(orderId);
        for (OrderItemDO orderItemDO : orderItemDOList) {
            OrderItemDTO orderItemDTO = orderConverter.orderItemDO2DTO(orderItemDO);
            orderItemDTOList.add(orderItemDTO);
        }
        return orderItemDTOList;
    }

    @Override
    public String getOrderIdByUserId(String userId) {
        List<OrderInfoDO> orderList = orderInfoDAO.getOrderListByUserId(userId);
        if (orderList.isEmpty()) {
            //  为保证压测流程能够进行下去,返回null时默认给一个数据表中的历史存量数据
            return "1022012265376529110";
        }
        return orderList.get(0).getOrderId();
    }
}
