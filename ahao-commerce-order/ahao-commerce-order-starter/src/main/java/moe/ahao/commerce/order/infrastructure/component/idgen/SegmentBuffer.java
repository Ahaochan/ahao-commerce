package moe.ahao.commerce.order.infrastructure.component.idgen;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 号段内存缓冲组件
 */
@Slf4j
@Data
public class SegmentBuffer {
    /**
     * 最大步长不超过100,0000
     */
    private static final int MAX_STEP = 1000000;
    /**
     * 默认一个Segment会维持的时间为15分钟
     * <p>
     * 如果在15分钟内Segment就消耗完了, 则步长要扩容一倍, 但不能超过MAX_STEP
     * 如果在超过15*2=30分钟才将Segment消耗完, 则步长要缩容一倍, 但不能低于MIN_STEP，MIN_STEP的值为数据库中初始的step字段值
     */
    private static final long SEGMENT_DURATION = 15 * 60 * 1000L;
    /**
     * 更新因子
     * <p>
     * 更新因子=2时，表示成倍扩容或者折半缩容
     */
    private static final int EXPAND_FACTOR = 2;

    /**
     * 业务标识
     */
    private String bizTag;

    /**
     * 双buffer
     */
    private Segment[] segments;

    /**
     * 当前的使用的segment的index
     */
    private volatile int currentPos;

    /**
     * 下一个segment是否处于可切换状态
     */
    private volatile boolean nextReady;

    /**
     * 是否初始化完成
     */
    private volatile boolean initOk;

    /**
     * 线程是否在运行中
     */
    private final AtomicBoolean threadRunning;

    private final ReadWriteLock lock;

    private volatile int step;
    private volatile int minStep;
    private volatile long updateTimestamp;

    public SegmentBuffer() {
        // 双缓冲机制, segment buffer里, 持有两个segment
        segments = new Segment[]{new Segment(this), new Segment(this)};
        currentPos = 0;
        nextReady = false;
        initOk = false; // 业务标识, 双缓冲里面的核心分段数据, 是懒加载的, 不会说一开始就从db里加载
        threadRunning = new AtomicBoolean(false);
        lock = new ReentrantReadWriteLock();
    }

    public Segment getCurrent() {
        return segments[currentPos];
    }

    public Lock rLock() {
        return lock.readLock();
    }

    public Lock wLock() {
        return lock.writeLock();
    }

    public int nextPos() {
        return (currentPos + 1) % 2;
    }

    public void switchPos() {
        currentPos = nextPos();
    }

    /**
     * 动态步长算法，成倍扩容，折半缩容：
     * 高并发下, 号段会快速用完, 此时会自适应调整步长, 避免号段很快用完
     *
     * @return 返回新的步长
     */
    public int calculateNextStep() {
        // 拿出来当前的step, 作为next step
        int nextStep = this.getStep();
        long duration = System.currentTimeMillis() - this.getUpdateTimestamp();
        if (duration < SEGMENT_DURATION) {
            // 如果双缓冲切换时间间隔小于15分钟, 就说明并发上来了, 就对步长乘以2扩容, 避免频繁切换
            nextStep = Math.min(MAX_STEP, nextStep * EXPAND_FACTOR); // 10000 -> 20000
        } else if (duration < SEGMENT_DURATION * EXPAND_FACTOR) {
            // 如果双缓冲切换时间间隔在15-30分钟之间, 就保持这个步长, 不做任何处理
        } else {
            // 如果双缓冲切换时间间隔超过30分钟, 说明并发下来了, 就不需要这么长的号段了, 就对步长除以2缩容
            nextStep = Math.max(this.getMinStep(), nextStep / EXPAND_FACTOR);
        }
        log.info("号段双缓冲计算步长, bizTag:{}, step:{}, nextStep:{}, duration:{}min", bizTag, this.getStep(), nextStep, (double) duration / (1000 * 60));
        return nextStep;
    }

    /**
     * 号段
     */
    @Data
    public static class Segment {
        /**
         * 值
         */
        private AtomicLong value = new AtomicLong(0);
        /**
         * 最大
         */
        private volatile long max;
        /**
         * 步长
         */
        private volatile int step;
        /**
         * 号段缓存器
         */
        private SegmentBuffer buffer;

        public Segment(SegmentBuffer buffer) {
            this.buffer = buffer;
        }

        public long getIdle() {
            return this.getMax() - getValue().get();
        }
    }
}
