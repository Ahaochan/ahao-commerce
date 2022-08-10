package moe.ahao.commerce.order.infrastructure.component.idgen;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.OrderAutoNoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class SegmentBizTagCache implements ApplicationListener<ContextRefreshedEvent> {
    @Autowired
    private OrderAutoNoRepository orderAutoNoRepository;

    // 多线程写和读的可见性，用了volatile来保证这个东西
    @Getter
    private volatile boolean initOk = false;

    // 每个业务标识都有一个双缓冲
    private final Map<String, SegmentBuffer> cache = new ConcurrentHashMap<>();

    // Spring容器回调这个方法, 进行初始化
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        checkAndInit();
    }

    public boolean containsKey(String bizCode) {
        checkAndInit();
        return cache.containsKey(bizCode);
    }

    public SegmentBuffer getValue(String bizCode) {
        checkAndInit();
        return cache.get(bizCode);
    }

    /**
     * 初始化数据
     * 1. 初始化cache
     */
    private void checkAndInit() {
        if (!initOk) {
            synchronized (this) {
                if (!initOk) {
                    log.info("SegmentIDCache初始化 ...");
                    // 确保加载到kv后才初始化成功
                    this.updateCacheFromDb(); // 从数据库里加载序列号到缓存里, 才算初始化成功了
                    initOk = true;
                    log.info("SegmentIDCache初始化成功");
                }
            }
        }
    }

    /**
     * 更新缓存key
     * 1. 获取所有biz
     * 2. 维护cache的增加，初始化segment
     * 3. 维护cache的删除。
     */
    private void updateCacheFromDb() {
        try {
            // 1. 获取数据库中的全量biz_tag, biz_tag通过insert语句手动插入数据库
            List<String> dbBizTags = orderAutoNoRepository.getBizTagList();
            log.info("SegmentIDCache从数据库中更新缓存, dbBizTags:{}", dbBizTags);
            if (CollectionUtils.isEmpty(dbBizTags)) {
                return;
            }
            // 2. 将新的biz_tag维护到cache中
            List<String> cacheBizTags = new ArrayList<>(cache.keySet());
            Set<String> insertBizTags = new HashSet<>(dbBizTags);
            for (String bizTag : cacheBizTags) {
                insertBizTags.remove(bizTag);
            }
            for (String bizTag : insertBizTags) {
                SegmentBuffer buffer = new SegmentBuffer();
                buffer.setBizTag(bizTag);

                SegmentBuffer.Segment segment = buffer.getCurrent();
                segment.setValue(new AtomicLong(0));
                segment.setMax(0);
                segment.setStep(0);
                cache.put(bizTag, buffer);
                log.info("SegmentIDCache 新增 bizTag:{}, SegmentBuffer {}", bizTag, buffer);
            }
            // 3. 将不存在的biz_tag从cache中移除
            Set<String> removeBizTags = new HashSet<>(cacheBizTags);
            for (String bizTag : dbBizTags) {
                removeBizTags.remove(bizTag);
            }
            for (String bizTag : removeBizTags) {
                cache.remove(bizTag);
                log.info("SegmentIDCache 移除 bizTag:{}", bizTag);
            }
        } catch (Exception e) {
            log.warn("SegmentIDCache更新缓存异常", e);
        }
    }
}
