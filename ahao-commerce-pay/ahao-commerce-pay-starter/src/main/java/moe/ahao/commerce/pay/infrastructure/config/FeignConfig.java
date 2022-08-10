package moe.ahao.commerce.pay.infrastructure.config;

import moe.ahao.commerce.pay.infrastructure.gateway.feign.AfterSaleFeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {AfterSaleFeignClient.class})
public class FeignConfig {
}
