package moe.ahao.commerce.order.adapter.http.migrate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.domain.entity.BaseDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 抽象迁移基类
 */
@Slf4j
public abstract class AbstractMigrateToEsHandler<T> {
    private static final long limit = 2000L;
    private static final int partitionSize = 1000;

    private final ThreadPoolTaskExecutor taskExecutor;
    public AbstractMigrateToEsHandler() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maximumPoolSize = corePoolSize << 1;
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maximumPoolSize);
        executor.setQueueCapacity(10000);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix(this.getHandlerName() + "async-task-thread-pool==>");
        executor.initialize();
        this.taskExecutor = executor;
    }

    public void execute(Content content) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            //默认增量刷
            long offset = content.getOffset();
            long endOffset = this.baseMapper().selectCount(Wrappers.emptyWrapper());

            // 分页查询订单
            long total = 0;
            do {
                // 分页查询
                Wrapper<T> queryWrapper = new QueryWrapper<T>()
                    .ge("id", offset + 1)
                    .le("id", offset + limit);
                List<T> orders = this.baseMapper().selectList(queryWrapper);

                if(orders.size() <= 0 || offset > endOffset) {
                    break;
                }

                List<List<T>> lists = Lists.partition(orders, partitionSize);
                for (List<T> list : lists) {
                    taskExecutor.execute(() -> {
                        try {
                            this.syncData(list);
                        } catch (Exception e) {
                            log.error("handler:{}，数据迁移至es异常:{}", getHandlerName(), list, e);
                        }
                    });
                    total += list.size();
                }
            } while (true);
        } catch (Exception e) {
            log.error("handler:{}，数据迁移至es异常:{} error={}", getHandlerName(), e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            log.info("handler:{}，运行时长:{}ms", getHandlerName(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    protected abstract void syncData(List<T> list) throws Exception;
    protected abstract BaseMapper<T> baseMapper();

    private String getHandlerName() {
        return this.getClass().getSimpleName();
    }

    @Data
    public static class Content {
        /**
         * 起始offset
         */
        private long offset = 0;
    }
}
