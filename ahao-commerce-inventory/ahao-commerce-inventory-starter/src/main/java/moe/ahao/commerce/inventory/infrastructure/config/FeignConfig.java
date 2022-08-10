package moe.ahao.commerce.inventory.infrastructure.config;

import moe.ahao.commerce.inventory.infrastructure.gateway.feign.AfterSaleQueryFeignClient;
import moe.ahao.commerce.inventory.infrastructure.gateway.feign.OrderQueryFeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {AfterSaleQueryFeignClient.class, OrderQueryFeignClient.class})
public class FeignConfig {
}
