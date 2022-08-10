package moe.ahao.commerce.inventory.infrastructure.gateway.feign;

import moe.ahao.commerce.aftersale.api.AfterSaleFeignApi;
import moe.ahao.commerce.aftersale.api.AfterSaleQueryFeignApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "ahao-commerce-order", contextId = "afterSale", path = AfterSaleFeignApi.PATH)
public interface AfterSaleQueryFeignClient extends AfterSaleQueryFeignApi {
}
