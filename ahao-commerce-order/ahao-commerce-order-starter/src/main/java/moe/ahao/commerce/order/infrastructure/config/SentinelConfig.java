package moe.ahao.commerce.order.infrastructure.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.RequestOriginParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;

@Configuration(proxyBeanMethods = false)
public class SentinelConfig {

    @Bean
    public RequestOriginParser requestOriginParser() {
        return request -> request.getHeader("user_id");
    }
}
