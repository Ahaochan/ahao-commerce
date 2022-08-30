package com.ruyuan.eshop.order;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.common.base.Stopwatch;
import com.ruyuan.eshop.order.builder.FullOrderData;
import com.ruyuan.eshop.order.utils.mock.PrepareOrderFullDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = OrderApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class PrepareOrderFullDataUtilsTest {

    @Autowired
    private PrepareOrderFullDataUtils prepareOrderFullDataUtils;


    @Test
    public void test() {
        int totalOrderNum = 100000; // 总订单数1亿
        int threadCount = 10; // 并发线程数，放到阿里云上去执行，这个值可以扩大,建议变成100
        int everyOrderNum = totalOrderNum/threadCount; // 每个线程要插入的订单数量
        final CountDownLatch latch = new CountDownLatch(threadCount);
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Worker(latch, everyOrderNum, prepareOrderFullDataUtils)).start();
        }
        try {
            latch.await();
            log.info("所有线程执行完毕，cost={}s",stopwatch.elapsed(TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    class Worker implements Runnable {
        int orderNum = 0;
        CountDownLatch latch;
        private PrepareOrderFullDataUtils prepareOrderFullDataUtils;

        public Worker(CountDownLatch latch, int orderNum, PrepareOrderFullDataUtils prepareOrderFullDataUtils) {
            this.orderNum = orderNum;
            this.prepareOrderFullDataUtils = prepareOrderFullDataUtils;
            this.latch = latch;
        }

        @Override
        public void run() {
            log.info("线程正在执行。。",Thread.currentThread().getName());
            List<FullOrderData> fullOrderDataList = new ArrayList<>();
            // 生成用户订单
            for (int i = 0; i < orderNum; i++) {
                FullOrderData fullOrderData = prepareOrderFullDataUtils.genFullMasterOrderData();
                fullOrderDataList.add(fullOrderData);
                if (fullOrderDataList.size() == 100) {
                    prepareOrderFullDataUtils.batchInsertOrderFullData(fullOrderDataList);
                    fullOrderDataList.clear();
                }
            }
            if (CollectionUtils.isNotEmpty(fullOrderDataList)) {
                prepareOrderFullDataUtils.batchInsertOrderFullData(fullOrderDataList);
                fullOrderDataList.clear();
            }

            latch.countDown();
        }
    }

}
