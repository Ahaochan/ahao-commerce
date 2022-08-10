package moe.ahao.commerce.order;

import moe.ahao.process.engine.wrapper.config.EnableProcessEngine;
import moe.ahao.tend.consistency.core.annotation.EnableTendConsistencyTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBoot方式启动类
 *
 * @author Ahaochan
 */
@EnableTendConsistencyTask
@EnableProcessEngine("ahao-order-process.xml")
@SpringBootApplication(scanBasePackages = "moe.ahao")
@AutoConfigurationPackage(basePackages = "moe.ahao") // 扫描@Mapper注解
public class OrderApplication {
    private static final Logger logger = LoggerFactory.getLogger(OrderApplication.class);

    public static void main(String[] args) {
        try {
            SpringApplication app = new SpringApplication(OrderApplication.class);
            app.run(args);
            logger.info("{}启动成功!", OrderApplication.class.getSimpleName());
        } catch (Exception e) {
            logger.error("{}启动失败!", OrderApplication.class.getSimpleName(), e);
        }
    }
}
