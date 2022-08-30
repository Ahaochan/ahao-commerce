package com.ruyuan.eshop.order.generator.service;

import com.ruyuan.eshop.order.dao.OrderAutoNoDAO;
import com.ruyuan.eshop.order.domain.entity.OrderAutoNoDO;
import com.ruyuan.eshop.order.generator.SegmentIDGen;
import com.ruyuan.eshop.order.generator.core.Segment;
import com.ruyuan.eshop.order.generator.core.SegmentBuffer;
import com.ruyuan.eshop.order.generator.core.SegmentIDCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.*;

/**
 * 号段ID生成器组件
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Service
public class SegmentIDGenImpl implements SegmentIDGen {

    /**
     * 最大步长不超过100,0000
     */
    private static final int MAX_STEP = 1000000;

    /**
     * 默认一个Segment会维持的时间为15分钟
     * <p>
     * 如果在15分钟内Segment就消耗完了，则步长要扩容一倍，但不能超过MAX_STEP
     * 如果在超过15*2=30分钟才将Segment消耗完，则步长要缩容一倍，但不能低于MIN_STEP，MIN_STEP的值为数据库中初始的step字段值
     */
    private static final long SEGMENT_DURATION = 15 * 60 * 1000L;

    /**
     * 更新因子
     * <p>
     * 更新因子=2时，表示成倍扩容或者折半缩容
     */
    private static final int EXPAND_FACTOR = 2;

    /**
     * 下一次异步更新比率因子
     */
    public static final double NEXT_INIT_FACTOR = 0.9;

    private final ExecutorService threadPoolExecutor = new ThreadPoolExecutor(1, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new UpdateThreadFactory());

    @Autowired
    private OrderAutoNoDAO orderAutoNoDAO;

    @Resource
    private SegmentIDCache cache;

    /**
     * 1. 如果没有初始化，报错
     * 2. 如果不包含业务code，报错
     * 3. 获取已缓存的segmentBuffer
     * 4. 如果segmentBuffer没有初始化，则初始化：
     * 4.1 double lock
     * 4.2 调用更新方法，并设置初始化
     * 4.3 异常报错
     * 5. 从buffer中获取id
     * <p>
     * 生成新的ID
     *
     * @param bizTag 业务标识
     * @return 返回
     */
    @Override
    public Long genNewNo(String bizTag) {
        if (!cache.isInitOk()) {
            throw new RuntimeException("not init");
        }

        if (!cache.containsKey(bizTag)) {
            throw new RuntimeException("not contains key:" + bizTag);
        }

        SegmentBuffer buffer = cache.getValue(bizTag);
        if (!buffer.isInitOk()) {
            synchronized (buffer) {
                if (!buffer.isInitOk()) {
                    try {
                        updateSegmentFromDb(bizTag, buffer.getCurrent());
                        log.info("Init buffer. Update leafkey {} {} from db", bizTag, buffer.getCurrent());
                        buffer.setInitOk(true);
                    } catch (Exception e) {
                        log.warn("Init buffer {} exception", buffer.getCurrent(), e);
                        throw new RuntimeException("init error:" + bizTag);
                    }
                }
            }
        }
        return getIdFromSegmentBuffer(buffer);
    }

    /**
     * 1. 加读锁
     * 2. 如果 nextSegment没有初始化，且当前segment使用超过10%，且没有线程在处理中，则进行异步更新初始化
     * 3(out). 获取最新id，如果小于maxid，则返回
     * 4. 解开读锁
     * 5. 等待线程处理(获取的value，大于max，则等待其他线程更新完毕。最多等待10ms)
     * 6. 开写锁。
     * 7(out). 再次获取最新id，如果长于max，返回
     * 8. 如果nextok，切换缓冲区（等待下次循环），否则抛异常
     * 9. 解开写锁。
     * <p>
     * 从segment缓冲中获取id
     *
     * @param buffer 缓存
     * @return 返回
     */
    private Long getIdFromSegmentBuffer(SegmentBuffer buffer) {
        while (true) {
            buffer.rLock().lock();
            try {
                final Segment segment = buffer.getCurrent();
                if (!buffer.isNextReady() && (segment.getIdle() < NEXT_INIT_FACTOR * segment.getStep())
                        && buffer.getThreadRunning().compareAndSet(false, true)) {
                    asyncUpdate(buffer);
                }
                long value = segment.getValue().getAndIncrement();
                if (value < segment.getMax()) {
                    return value;
                }
            } finally {
                buffer.rLock().unlock();
            }

            //获取的value，大于max，则等待其他线程更新完毕。最多等待100s
            waitAndSleep(buffer);
            buffer.wLock().lock();
            try {
                final Segment segment = buffer.getCurrent();
                long value = segment.getValue().getAndIncrement();
                if (value < segment.getMax()) {
                    return value;
                }
                if (buffer.isNextReady()) {
                    buffer.switchPos();
                    buffer.setNextReady(false);
                } else {
                    log.error("Both two segments in {} are not ready!", buffer);
                    throw new RuntimeException("next not ready");
                }
            } finally {
                buffer.wLock().unlock();
            }
        }
    }

    /**
     * 异步更新初始化
     * 1 获取下一个segment.
     * 2 更新db.
     * 3 如果更新正常，开写锁，更新nextready、running等
     * 4 如果异常。设置runing
     *
     * @param buffer 缓存
     */
    private void asyncUpdate(SegmentBuffer buffer) {
        long submitTime = System.currentTimeMillis();
        threadPoolExecutor.execute(() -> {
            long executeTime = System.currentTimeMillis();
            Segment next = buffer.getSegments()[buffer.nextPos()];
            boolean updateOk = false;
            try {
                updateSegmentFromDb(buffer.getBizTag(), next);
                updateOk = true;
            } catch (Exception e) {
                log.warn("{} updateSegmentFromDb exception", buffer.getBizTag(), e);
            } finally {
                long finishTime = System.currentTimeMillis();
                log.info("update segment {} from db {}。st:{}, et:{}, ft:{}", buffer.getBizTag(),
                        next, submitTime, executeTime, finishTime);
                if (updateOk) {
                    buffer.wLock().lock();
                    buffer.setNextReady(true);
                    buffer.getThreadRunning().set(false);
                    buffer.wLock().unlock();
                } else {
                    buffer.getThreadRunning().set(false);
                }
            }
        });
    }

    /**
     * 自旋10000次之后，睡眠10毫秒
     */
    private void waitAndSleep(SegmentBuffer buffer) {
        int roll = 0;
        while (buffer.getThreadRunning().get()) {
            roll += 1;
            if (roll > 10000) {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                    break;
                } catch (InterruptedException e) {
                    log.warn("Thread {} Interrupted", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

    /**
     * 1. 如果没有init
     * 1.1 更新maxId，并获取最新no
     * 1.2 将Step、MinStep 设置成db中的step
     * 2. 如果更新时间为0
     * 2.1 更新maxId，并获取最新no
     * 2.2 更新update时间
     * 2.3 将Step、MinStep 设置成db中的step
     * 3. 其他
     * 3.1 动态计算步长
     * 3.2 更新db步长等
     * 3.3 更新buffer的更新时间、步长、min步长
     * 4. 更新segment信息
     * 5. 统计数据
     * <p>
     * 从db中更新号段
     *
     * @param bizTag  业务标识
     * @param segment 段
     */
    public void updateSegmentFromDb(String bizTag, Segment segment) {
        SegmentBuffer buffer = segment.getBuffer();
        OrderAutoNoDO orderAutoNoDO;
        if (!buffer.isInitOk()) {
            orderAutoNoDO = orderAutoNoDAO.updateMaxIdAndGet(bizTag);
            buffer.setStep(orderAutoNoDO.getStep());
            buffer.setMinStep(orderAutoNoDO.getStep());
        } else if (buffer.getUpdateTimestamp() == 0) {
            orderAutoNoDO = orderAutoNoDAO.updateMaxIdAndGet(bizTag);
            buffer.setUpdateTimestamp(System.currentTimeMillis());
            buffer.setStep(orderAutoNoDO.getStep());
            buffer.setMinStep(orderAutoNoDO.getStep());
        } else {
            int nextStep = calculateNextStep(bizTag, buffer);
            orderAutoNoDO = orderAutoNoDAO.updateMaxIdByDynamicStepAndGet(bizTag, nextStep);
            buffer.setUpdateTimestamp(System.currentTimeMillis());
            buffer.setStep(nextStep);
            buffer.setMinStep(orderAutoNoDO.getStep());
        }
        // must set value before set max
        long value = orderAutoNoDO.getMaxId() - buffer.getStep();
        segment.getValue().set(value);
        segment.setMax(orderAutoNoDO.getMaxId());
        segment.setStep(buffer.getStep());
        log.info("updateSegmentFromDb, bizTag: {}, cost:0, segment:{}", bizTag, segment);
    }

    /**
     * 动态步长算法，成倍扩容，折半缩容：
     * 1 如果更新间隔小于间隔阈值
     * 1.1 如果 *2 大于 阈值，不处理。不小于，则step翻倍。
     * 2 如果更新时间 小于 间隔时间*2，不处理。
     * 3 如果更新时间 大于 间隔时间*2：
     * 3.1 如果step折半小于miniStep，则折半；
     * <p>
     * 计算新的步长
     *
     * @param bizCode 业务code
     * @param buffer  缓存
     * @return 返回
     */
    private int calculateNextStep(String bizCode, SegmentBuffer buffer) {
        long duration = System.currentTimeMillis() - buffer.getUpdateTimestamp();
        int nextStep = buffer.getStep();
        if (duration < SEGMENT_DURATION) {
            nextStep = Math.min(MAX_STEP, nextStep * EXPAND_FACTOR);
        } else if (duration < SEGMENT_DURATION * EXPAND_FACTOR) {
            // do nothing with nextStep
        } else {
            nextStep = Math.max(buffer.getMinStep(), nextStep / EXPAND_FACTOR);
        }
        log.info("leafKey[{}], step[{}], duration[{}mins], nextStep[{}]", bizCode, buffer.getStep(),
                String.format("%.2f", ((double) duration / (1000 * 60))), nextStep);
        return nextStep;
    }

    public static class UpdateThreadFactory implements ThreadFactory {
        private static int threadInitNumber = 0;

        private static synchronized int nextThreadNum() {
            return threadInitNumber++;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Thread-Segment-Update-" + nextThreadNum());
        }
    }
}
