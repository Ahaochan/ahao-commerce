package moe.ahao.commerce.order.api;

import moe.ahao.commerce.order.api.dto.OrderDetailDTO;
import moe.ahao.commerce.order.api.dto.OrderListDTO;
import moe.ahao.commerce.order.api.query.OrderDetailQuery;
import moe.ahao.commerce.order.api.query.OrderPageQuery;
import moe.ahao.domain.entity.PagingInfo;
import moe.ahao.domain.entity.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 订单中心-订单查询业务接口
 */
public interface OrderQueryFeignApi {
    String PATH = "/api/order";

    /**
     * 查询订单列表
     */
    @PostMapping("/listOrders")
    Result<PagingInfo<OrderDetailDTO>> listOrders(@RequestBody OrderPageQuery query);

    /**
     * 查询订单详情
     */
    @PostMapping("/orderDetail")
    Result<OrderDetailDTO> orderDetail(@RequestBody OrderDetailQuery query);

    /**
     * 根据订单id查询订单条目
     */
    @PostMapping("/orderItemDetail")
    Result<List<OrderDetailDTO.OrderItemDTO>> orderItemDetail(@RequestParam String orderId);
}
