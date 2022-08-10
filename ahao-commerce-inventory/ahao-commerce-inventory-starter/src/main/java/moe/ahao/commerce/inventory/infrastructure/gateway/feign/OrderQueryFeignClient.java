package moe.ahao.commerce.inventory.infrastructure.gateway.feign;

import moe.ahao.commerce.aftersale.api.AfterSaleFeignApi;
import moe.ahao.commerce.order.api.OrderQueryFeignApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "ahao-commerce-order", contextId = "order", path = AfterSaleFeignApi.PATH)
public interface OrderQueryFeignClient extends OrderQueryFeignApi {
}
