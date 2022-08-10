package moe.ahao.commerce.pay.infrastructure.gateway.feign;

import moe.ahao.commerce.aftersale.api.AfterSaleFeignApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "ahao-commerce-order", path = AfterSaleFeignApi.PATH)
public interface AfterSaleFeignClient extends AfterSaleFeignApi {
}
