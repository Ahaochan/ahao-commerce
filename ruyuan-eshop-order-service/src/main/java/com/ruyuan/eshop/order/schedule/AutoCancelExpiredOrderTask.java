package com.ruyuan.eshop.order.schedule;


import com.ruyuan.eshop.order.dao.OrderCancelScheduledTaskDAO;
import com.ruyuan.eshop.order.domain.entity.OrderCancelScheduledTaskDO;
import com.ruyuan.eshop.order.service.OrderAfterSaleService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 自动取消超时订单任务
 *
 * @author zhonghuashishan
 */
@Slf4j
@Component
public class AutoCancelExpiredOrderTask {

    @Autowired
    private OrderAfterSaleService orderAfterSaleService;

    @Autowired
    private OrderCancelScheduledTaskDAO orderCancelScheduledTaskDAO;

    /**
     * 方案说明：
     * 由第一版本的"扫表取消" 改为 第二版本的"取消订单定时任务兜底"
     * 第一版本:每30min扫表,查询出所有未支付订单,调用取消订单接口
     * 第二版本:每1min定时执行一次,与延迟MQ共用同一套超时未支付取消订单逻辑
     * <p>
     * 业务主体说明：
     * 执行超时未支付取消订单业务逻辑的主体还是延迟MQ
     * 定时任务在此作为一个兜底的方案,在例如MQ宕机等极端情况下让系统维持取消订单业务的正常执行,避免用户未支付订单到期后没有取消
     * <p>
     * 业务场景说明：
     * 第一种场景：
     * 订单支付的时间单位是整时整点,例如：
     * A订单创建时间是10：00：00,支付截止时间就是10：30：00
     * 延迟MQ和XXL-JOB定时任务都会在10：30：00执行取消,争抢分布式锁,谁有锁谁执行
     * <p>
     * 第二种场景：
     * 订单支付时间有毫秒偏差,例如：
     * B订单创建时间11：01：01,支付截止时间就是11：31：01
     * 正常情况下延迟MQ会在11：31：01执行取消
     * 如果MQ挂了，定时任务因为在XXL-JOB中配置的是每1min执行一次，所以该订单会在11：32：00由定时任务执行取消
     * <p>
     * 第一版扫表方案,详见:AutoCancelExpiredOrderTaskTest
     */
    @XxlJob("autoCancelExpiredScheduledTask")
    public void autoCancelExpiredScheduledTask() {
        int shardIndex = Optional.of(XxlJobHelper.getShardIndex()).orElse(0);
        int totalShardNum = Optional.of(XxlJobHelper.getShardTotal()).orElse(0);

        //  查询所有任务表里的记录
        List<OrderCancelScheduledTaskDO> orderCancelScheduledTaskDOList = orderCancelScheduledTaskDAO.getUnPaidOrderRecord();
        List<String> orderIdList = orderCancelScheduledTaskDOList.stream()
                .map(OrderCancelScheduledTaskDO::getOrderId).collect(Collectors.toList());

        for (String orderId : orderIdList) {
            if (totalShardNum <= 0) {
                //  不进行分片
                orderAfterSaleService.verifyBeforeOrderCancellation(orderId);
            } else {
                //  分片
                int hash = hash(orderId) % totalShardNum;
                if (hash == shardIndex) {
                    orderAfterSaleService.verifyBeforeOrderCancellation(orderId);
                }
            }
        }
        XxlJobHelper.handleSuccess();
    }

    /**
     * hash
     */
    private int hash(String orderId) {
        //  解决取模可能为负数的情况
        return orderId.hashCode() & Integer.MAX_VALUE;
    }
}
