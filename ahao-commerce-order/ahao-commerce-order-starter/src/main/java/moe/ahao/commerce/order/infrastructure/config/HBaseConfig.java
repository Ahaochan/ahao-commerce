package moe.ahao.commerce.order.infrastructure.config;

import lombok.Data;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * HBase的配置
 */
@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HBaseConfig.HBaseProperties.class)
public class HBaseConfig {

    /**
     * 配置hbase
     *
     * @return hbase的配置
     */
    @Bean
    public Configuration configuration(HBaseProperties hBaseProperties) {
        Configuration configuration = HBaseConfiguration.create();
        Map<String, String> config = hBaseProperties.getConfig();
        config.forEach(configuration::set);
        return configuration;
    }

    /**
     * 获取hbase的连接
     *
     * @return hbase连接
     * @throws IOException 异常
     */
    @Bean
    public Connection getConnection(Configuration configuration) throws IOException {
        return ConnectionFactory.createConnection(configuration);
    }

    /**
     * 从配置文件中读取HBase配置信息
     * 配置文件格式:hbase.config.*=xxx
     */
    @Data
    @ConfigurationProperties(prefix = "hbase")
    public static class HBaseProperties {
        private Map<String, String> config;
    }
}
