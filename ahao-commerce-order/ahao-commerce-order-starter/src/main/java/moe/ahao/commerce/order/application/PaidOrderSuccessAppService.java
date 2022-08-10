package moe.ahao.commerce.order.application;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.OrderTypeEnum;
import moe.ahao.commerce.common.infrastructure.event.PaidOrderSuccessEvent;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.FulfillGateway;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class PaidOrderSuccessAppService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderFulFillService orderFulFillService;

    @Autowired
    private FulfillGateway fulfillGateway;

    @Autowired
    private RedissonClient redissonClient;

    public void consumer(PaidOrderSuccessEvent event) {
        // 1. 参数校验
        String orderId = event.getOrderId();
        log.info("消费已支付消息, 触发订单履约，orderId:{}", orderId);
        OrderInfoDO orderInfoDO = orderInfoMapper.selectOneByOrderId(orderId);
        if (orderInfoDO == null) {
            throw OrderExceptionEnum.ORDER_INFO_IS_NULL.msg();
        }

        // 2. 判断是否可以触发履约, 没有子订单并且不是虚拟订单可以履约
        //    无效主单过来是不能触发履约, 但是你的子单支付事件也会过来, 触发子单履约
        List<OrderInfoDO> subOrders = orderInfoMapper.selectListByParentOrderId(orderId);
        if (!canTriggerFulfill(orderInfoDO, subOrders)) {
            return;
        }

        // 3. 加分布式锁, 结合履约前置状态校验, 防止消息重复消费
        String lockKey = RedisLockKeyConstants.ORDER_FULFILL_KEY + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            log.error("消费已支付消息, 触发订单履约异常, 获取不到分布式锁{}, orderId:{}", lockKey, orderId);
            throw OrderExceptionEnum.ORDER_FULFILL_ERROR.msg();
        }

        try {
            // 4. 进行订单履约逻辑
            orderFulFillService.triggerOrderFulFill(orderId);

            // 5. 将订单推送至履约
            ReceiveFulfillCommand receiveFulfillCommand = orderFulFillService.buildReceiveFulFillRequest(orderInfoDO);
            fulfillGateway.receiveOrderFulFill(receiveFulfillCommand);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 判断是否可以触发履约
     */
    private boolean canTriggerFulfill(OrderInfoDO order, List<OrderInfoDO> subOrders) {
        return CollectionUtils.isEmpty(subOrders) && OrderTypeEnum.canFulfillTypes().contains(order.getOrderType());
    }
}
