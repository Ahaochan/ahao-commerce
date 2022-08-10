package moe.ahao.commerce.aftersale.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.CancelOrderCommand;
import moe.ahao.commerce.aftersale.infrastructure.enums.OrderCancelTypeEnum;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderCancelScheduledTaskDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderCancelScheduledTaskMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.OrderStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CancelOrderAppService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderCancelScheduledTaskMapper orderCancelScheduledTaskMapper;

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StateMachineFactory stateMachineFactory;

    public boolean autoCancel(String orderId) {
        // 1. 分布式锁(与预支付、订单支付回调加的是同一把锁)
        String lockKey = RedisLockKeyConstants.ORDER_PAY_KEY + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw OrderExceptionEnum.ORDER_CANNOT_OPERATED_IN_MULTIPLE_PLACES.msg();
        }

        try {
            // 这里先不用加事务
            // 1. 避免影响状态机内部的事务, 状态机内部还会有一轮校验
            // 2. 这里的校验只是对自动取消进行校验, 不会影响订单
            // 3. 哪怕异常, 重试即可, 如果已经扭转完状态, 然后删除任务失败, 下次过来发现状态不对, 也可以删除任务

            // 1. 查询订单实时状态
            OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
            if (orderInfo == null) {
                return false;
            }
            OrderCancelScheduledTaskDO orderCancelScheduledTask = orderCancelScheduledTaskMapper.selectOneByOrderId(orderId);
            if (orderCancelScheduledTask == null) {
                return false;
            }
            // 订单非"已创建"状态, 说明有了其他的操作, 可以删除掉兜底记录, 结束流程
            if (!OrderStatusEnum.CREATED.getCode().equals(orderInfo.getOrderStatus())) {
                // 删除任务记录
                orderCancelScheduledTaskMapper.deleteByOrderId(orderId);
                return false;
            }
            // 未超时, 就不处理了
            if (System.currentTimeMillis() <= orderInfo.getExpireTime().getTime()) {
                return false;
            }

            // 2. 执行取消订单
            CancelOrderCommand command = new CancelOrderCommand();
            command.setOrderId(orderId);
            command.setCancelType(OrderCancelTypeEnum.TIMEOUT_CANCELED.getCode());
            this.cancel(command);

            // 3. 删除任务记录
            orderCancelScheduledTaskMapper.deleteByOrderId(orderId);
            return true;
        } finally {
            // 释放分布式锁
            lock.unlock();
        }
    }

    public boolean cancel(CancelOrderCommand command) {
        OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.CANCELLED);
        orderStateMachine.fire(OrderStatusChangeEnum.ORDER_CANCEL, command);
        return true;
    }
}
