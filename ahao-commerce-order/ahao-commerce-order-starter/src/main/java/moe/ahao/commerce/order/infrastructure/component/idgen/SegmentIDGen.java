package moe.ahao.commerce.order.infrastructure.component.idgen;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.OrderAutoNoRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderAutoNoDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 号段ID生成器组件
 */
@Slf4j
@Service
public class SegmentIDGen {
    /**
     * 下一次异步更新比率因子
     */
    public static final double NEXT_INIT_FACTOR = 0.9;

    private final ExecutorService threadPoolExecutor = new ThreadPoolExecutor(1, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new UpdateThreadFactory());

    @Autowired
    private OrderAutoNoRepository orderAutoNoRepository;

    @Autowired
    private SegmentBizTagCache cache;

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
    public Long genNewNo(String bizTag) {
        // 1. 校验, 保证号段缓存SegmentIDCache初始化完毕, 并且存在当前bizTag业务类型
        if (!cache.isInitOk()) {
            throw new RuntimeException("SegmentIDCache未完成初始化");
        }
        if (!cache.containsKey(bizTag)) {
            throw new RuntimeException("SegmentIDCache不包含bizTag:" + bizTag);
        }

        // 2. 获取这个业务标识的SegmentBuffer双缓冲
        SegmentBuffer buffer = cache.getValue(bizTag);

        // 多个线程并发的积压在这里，都发现此时你的双缓冲的分段数据，max、step还没初始化
        // 积压在sync加锁这里，只有一个人可以进去，就会完成db里的分段数据，加载到双缓冲里来
        // 后续的线程加锁进来，双缓冲已经完成了分段数据初始化
        if (!buffer.isInitOk()) {
            synchronized (buffer) {
                if (!buffer.isInitOk()) {
                    try {
                        this.updateSegmentFromDb(bizTag, buffer.getCurrent());
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
        // 必须要确保这次序列号要生成
        while (true) {
            // 1. 先加读锁, 允许并发读
            buffer.rLock().lock();
            try {
                final SegmentBuffer.Segment segment = buffer.getCurrent();

                // boolean nextBufferNotReady = !buffer.isNextReady();
                // boolean idleNotEnough = segment.getIdle() < NEXT_INIT_FACTOR * segment.getStep();
                // boolean asyncUpdateNotRunning = buffer.getThreadRunning().compareAndSet(false, true);
                if (!buffer.isNextReady() && (segment.getIdle() < NEXT_INIT_FACTOR * segment.getStep())
                    && buffer.getThreadRunning().compareAndSet(false, true)) {
                    // 只有当前buffer剩余号段不足, 下一个buffer没有准备好, 并且下一个buffer异步更新任务还未启动的时候
                    // 才会触发下一个buffer的更新操作, 只会有一个线程进来这里
                    this.asyncUpdate(buffer);
                }

                // 2. 取号, 如果在max范围内, 就可以直接返回了, 这里使用Atomic的CAS支持并发读
                long value = segment.getValue().getAndIncrement();
                if (value < segment.getMax()) {
                    return value;
                }
            } finally {
                // 2. 释放读锁
                buffer.rLock().unlock();
            }

            // 3. 如果执行到这里, 说明value >= max了, 此时肯定有一个后台线程在异步更新下一个号段的内容
            //    那么就等待100秒, 自旋10000次, 每10毫秒唤醒一次, 这里一定会有多线程阻塞在这里
            this.waitAndSleep(buffer);

            // 4. 异步任务执行完毕了, 说明下一个号段已经缓冲好了, 就只允许一个线程加写锁, 去切换双缓冲
            buffer.wLock().lock();
            try {
                // 4.2. 第一个拿到写锁的线程切换了双缓冲, 后续的线程就直接判断value < max, 如果是就直接返回了
                final SegmentBuffer.Segment segment = buffer.getCurrent();
                long value = segment.getValue().getAndIncrement();
                if (value < segment.getMax()) {
                    return value;
                }
                // 4.1. 放进来一个线程, 去切换双缓冲
                if (buffer.isNextReady()) {
                    buffer.switchPos();
                    buffer.setNextReady(false);
                } else {
                    // 如果下一个号段还没准备好, 说明已经异步更新失败了, 这里就抛出异常
                    log.error("Both two segments in {} are not ready!", buffer);
                    throw new RuntimeException("next not ready");
                }
            } finally {
                // 5. 释放写锁
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
            SegmentBuffer.Segment next = buffer.getSegments()[buffer.nextPos()]; // next segment
            boolean updateOk = false;
            try {
                this.updateSegmentFromDb(buffer.getBizTag(), next);
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
    public void updateSegmentFromDb(String bizTag, SegmentBuffer.Segment segment) {
        SegmentBuffer buffer = segment.getBuffer();
        OrderAutoNoDO orderAutoNoDO;
        if (!buffer.isInitOk()) {
            // 如果双缓冲号段没有初始化过, 说明是第一次调用, 进行懒加载, 从数据库获取号段信息
            orderAutoNoDO = orderAutoNoRepository.updateMaxIdAndGet(bizTag);
            buffer.setStep(orderAutoNoDO.getStep());
            buffer.setMinStep(orderAutoNoDO.getStep());
        } else if (buffer.getUpdateTimestamp() == 0) {
            // 如果双缓冲号段是第一次初始化, 它的updateTimeStamp是0, 就和上面一样, 只是会更新下时间戳
            orderAutoNoDO = orderAutoNoRepository.updateMaxIdAndGet(bizTag);
            buffer.setUpdateTimestamp(System.currentTimeMillis());
            buffer.setStep(orderAutoNoDO.getStep());
            buffer.setMinStep(orderAutoNoDO.getStep());
        } else {
            // 如果双缓冲号段已经更新过n次了, 就动态计算步长, 更新到数据库里
            int nextStep = buffer.calculateNextStep();
            orderAutoNoDO = orderAutoNoRepository.updateMaxIdWithStepByAndGet(bizTag, nextStep);
            buffer.setUpdateTimestamp(System.currentTimeMillis());
            buffer.setStep(nextStep); // 动态算出来的步长
            buffer.setMinStep(orderAutoNoDO.getStep());
        }

        // must set value before set max
        // max=10000，step=10000，段起始值是max-step=0
        long value = orderAutoNoDO.getMaxId() - buffer.getStep();
        segment.getValue().set(value); // 当前的分段的起始序列号
        segment.setMax(orderAutoNoDO.getMaxId()); // 当前的分段的最大序号
        segment.setStep(buffer.getStep()); // 当前的分段的步长
        log.info("updateSegmentFromDb, bizTag: {}, cost:0, segment:{}", bizTag, segment);
    }

    private static final class UpdateThreadFactory implements ThreadFactory {
        private static final AtomicInteger threadInitNumber = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Thread-Segment-Update-" + threadInitNumber.getAndIncrement());
        }
    }
}
