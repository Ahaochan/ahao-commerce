package moe.ahao.commerce.order.adapter.schedule;

import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ElasticsearchCheckIndexExistsTask implements ApplicationRunner {
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Reflections reflections = new Reflections("moe.ahao.commerce");
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Document.class);
        for (Class<?> clazz : classes) {
            boolean exists = elasticsearchRestTemplate.indexOps(clazz).exists();
            if(!exists) {
                throw new Exception(clazz.getName() + "的es索引不存在, 请手动初始化");
            }
        }
    }
}
