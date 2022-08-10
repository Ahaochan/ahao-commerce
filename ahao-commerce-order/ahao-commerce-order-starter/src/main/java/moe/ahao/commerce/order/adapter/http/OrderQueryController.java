package moe.ahao.commerce.order.adapter.http;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.api.OrderQueryFeignApi;
import moe.ahao.commerce.order.api.dto.OrderDetailDTO;
import moe.ahao.commerce.order.api.dto.OrderListDTO;
import moe.ahao.commerce.order.api.query.OrderDetailQuery;
import moe.ahao.commerce.order.api.query.OrderPageQuery;
import moe.ahao.commerce.order.application.OrderQueryService;
import moe.ahao.domain.entity.PagingInfo;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 订单中心-订单查询业务接口
 */
@Slf4j
@RestController
@RequestMapping(OrderQueryFeignApi.PATH)
public class OrderQueryController implements OrderQueryFeignApi {
    @Autowired
    private OrderQueryService orderQueryService;

    /**
     * 查询订单列表
     */
    @Override
    public Result<PagingInfo<OrderDetailDTO>> listOrders(@RequestBody OrderPageQuery query) {
        PagingInfo<OrderDetailDTO> result = orderQueryService.queryV2(query);
        return Result.success(result);
    }

    /**
     * 查询订单详情
     */
    @Override
    public Result<OrderDetailDTO> orderDetail(OrderDetailQuery query) {
        OrderDetailDTO result = orderQueryService.orderDetail(query.getOrderId());
        return Result.success(result);
    }

    @Override
    public Result<List<OrderDetailDTO.OrderItemDTO>> orderItemDetail(String orderId) {
        List<OrderDetailDTO.OrderItemDTO> orderItemList = orderQueryService.orderItemDetail(orderId);
        return Result.success(orderItemList);
    }
}
