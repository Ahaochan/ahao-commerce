package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.cancel;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.CancelOrderCommand;
import moe.ahao.commerce.aftersale.infrastructure.enums.OrderCancelTypeEnum;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.AfterSaleTypeEnum;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.common.infrastructure.event.ReleaseAssetsEvent;
import moe.ahao.commerce.fulfill.api.command.CancelFulfillCommand;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.OrderStateAction;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.FulfillGateway;
import moe.ahao.commerce.order.infrastructure.publisher.OrderEventPublisher;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderItemDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderItemMapper;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.OrderStateMachine;
import moe.ahao.exception.CommonBizExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;


/**
 * 订单取消入口Action
 */
@Slf4j
@Component
public class OrderCancelAction extends OrderStateAction<CancelOrderCommand> {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private FulfillGateway fulfillGateway;

    @Autowired
    private StateMachineFactory stateMachineFactory;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_CANCEL;
    }

    /**
     * 取消订单只允许整笔取消,包含2套场景：
     * 场景1、超时未支付取消：操作的order是父单,入口是延迟MQ和XXL-JOB定时任务 (操作的数据类型见：下单后)
     * 场景2、用户手动取消: 操作的order是任意1笔(普通订单 or 预售订单 or 虚拟订单),入口是AfterSaleController#cancelOrder方法 (操作的数据类型见：支付后)
     * <p>
     * 拆单依据:
     * 一笔订单购买不同类型的商品,生单时会拆单,反之不拆
     * <p>
     * 正向生单拆单后,订单数据的状态变化：
     * (下单后)                   (支付后 状态>= 20)         (自动取消订单后)
     * +─────+─────+             +─────+─────+             +─────+─────+
     * | 类型 | 状态|             | 类型 | 状态|             | 类型 | 状态 |
     * +─────+─────+             +─────+─────+             +─────+─────+
     * | 父单 | 10  |            | 父单 | 127 |             | 父单 | 70  |
     * | 普通 | 127 |    =>      | 普通 | 20  |     =>      | 普通 | 70  |
     * | 预售 | 127 |            | 预售 | 20  |             | 预售 | 70  |
     * | 虚拟 | 127 |            | 虚拟 | 20  |             | 虚拟 | 70  |
     * +─────+─────+             +─────+─────+             +─────+─────+
     * <p>
     * 注:普通/预售/虚拟 不同订单和订单条目的关系都是 1(订单) v 多(条目)
     */
    @Override
    protected String onStateChangeInternal(OrderStatusChangeEnum event, CancelOrderCommand command) {
        // 1. 入参检查
        this.check(command);

        // 2. 分布式锁
        String orderId = command.getOrderId();
        String lockKey = RedisLockKeyConstants.CANCEL_KEY + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw OrderExceptionEnum.CANCEL_ORDER_REPEAT.msg();
        }

        try {
            // 3. 拦截履约, 更新订单状态
            this.cancel(command);
            // 4. 向下游发送释放权益资产MQ
            OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
            ReleaseAssetsEvent releaseAssetsEvent = new ReleaseAssetsEvent();
            releaseAssetsEvent.setOrderId(orderId);
            releaseAssetsEvent.setUserId(orderInfo.getUserId());
            releaseAssetsEvent.setCouponId(orderInfo.getCouponId());
            orderEventPublisher.sendReleaseAssetsMessage(releaseAssetsEvent);
        } finally {
            lock.unlock();
        }
        // 返回null 不发送订单变更事件
        return null;
    }

    /**
     * 入参检查
     */
    private void check(CancelOrderCommand command) {
        if (command == null) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }

        // 订单id
        String orderId = command.getOrderId();
        if (StringUtils.isEmpty(orderId)) {
            throw OrderExceptionEnum.CANCEL_ORDER_ID_IS_NULL.msg();
        }

        // 订单取消类型
        Integer cancelType = command.getCancelType();
        if (cancelType == null) {
            throw OrderExceptionEnum.CANCEL_TYPE_IS_NULL.msg();
        }
    }

    private void cancel(CancelOrderCommand command) {
        // 执行履约取消、更新订单状态、新增订单日志操作
        // @Transactional无法生效，需要用编程式事务
        transactionTemplate.execute(transactionStatus -> {
            // 1. 校验
            String orderId = command.getOrderId();
            OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
            Integer orderStatus = orderInfo.getOrderStatus();
            // 已取消的订单不能重复取消
            if (orderStatus.equals(OrderStatusEnum.CANCELLED.getCode())) {
                throw OrderExceptionEnum.ORDER_STATUS_CANCELED.msg();
            }
            // 大于已出库状态的订单不能取消, 注:虚拟订单在支付后会自动更新成"已签收", 所以已支付的虚拟订单不会被取消
            if (orderStatus >= OrderStatusEnum.OUT_STOCK.getCode()) {
                throw OrderExceptionEnum.ORDER_STATUS_CHANGED.msg();
            }
            List<OrderItemDO> orderItemDOList = orderItemMapper.selectListByOrderId(orderId);
            Integer afterSaleType = AfterSaleTypeEnum.RETURN_MONEY.getCode();

            // 检查订单状态
            orderInfo.setCancelType(command.getCancelType());

            // 设置调用履约接口标记
            boolean fulfilled = OrderStatusEnum.CREATED.getCode().equals(orderStatus);
            if (!fulfilled) {
                log.info("取消订单拦截履约, 当前订单{}状态为:{}, 无需拦截履约", orderId, orderStatus);
            } else {
                log.info("取消订单拦截履约, 当前订单{}状态为:{}, 开始拦截履约", orderId, orderStatus);
                // 取消订单调用履约接口
                // 如果取消履约成功, 也需要发送一个消息来更新订单状态为已取消, 避免取消履约成功, 更新订单状态失败
                this.cancelFulfill(orderInfo);
            }
            // 执行具体场景的取消逻辑
            this.doSpecialOrderCancel(orderInfo);
            return true;
        });
    }

    /**
     * 调用履约拦截订单
     */
    private void cancelFulfill(OrderInfoDO orderInfo) {
        CancelFulfillCommand command = new CancelFulfillCommand();
        command.setBusinessIdentifier(orderInfo.getBusinessIdentifier());
        command.setOrderId(orderInfo.getOrderId());
        command.setParentOrderId(orderInfo.getParentOrderId());
        command.setBusinessOrderId(orderInfo.getBusinessOrderId());
        command.setOrderType(orderInfo.getOrderType());
        command.setOrderStatus(orderInfo.getOrderStatus());
        command.setCancelType(orderInfo.getCancelType());
        command.setCancelTime(orderInfo.getCancelTime());
        command.setSellerId(orderInfo.getSellerId());
        command.setUserId(orderInfo.getUserId());
        command.setPayType(orderInfo.getPayType());
        command.setTotalAmount(orderInfo.getTotalAmount());
        command.setPayAmount(orderInfo.getPayAmount());
        command.setCouponId(orderInfo.getCouponId());
        command.setPayTime(orderInfo.getPayTime());
        // TODO command.setCancelDeadlineTime(orderInfo.getCancelTime());
        command.setSellerRemark(orderInfo.getUserRemark());
        command.setUserRemark(orderInfo.getUserRemark());
        command.setUserType(orderInfo.getOrderType());
        command.setDeleteStatus(orderInfo.getDeleteStatus());
        command.setCommentStatus(orderInfo.getCommentStatus());

        fulfillGateway.cancelFulfill(command);
    }

    /**
     * 执行具体场景的取消逻辑
     */
    private void doSpecialOrderCancel(OrderInfoDO orderInfo) {
        Integer cancelType = orderInfo.getCancelType();
        Integer orderStatus = orderInfo.getOrderStatus();

        if (Objects.equals(OrderCancelTypeEnum.USER_CANCELED.getCode(), cancelType) && Objects.equals(OrderStatusEnum.CREATED.getCode(), orderStatus)) {
            // 订单未支付手动取消
            OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.CREATED);
            orderStateMachine.fire(OrderStatusChangeEnum.ORDER_UN_PAID_MANUAL_CANCELLED, orderInfo);
        } else if (Objects.equals(OrderCancelTypeEnum.USER_CANCELED.getCode(), cancelType) && Objects.equals(OrderStatusEnum.PAID.getCode(), orderStatus)) {
            // 订单已支付手动取消
            OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.PAID);
            orderStateMachine.fire(OrderStatusChangeEnum.ORDER_PAID_MANUAL_CANCELLED, orderInfo);
        } else if (Objects.equals(OrderCancelTypeEnum.USER_CANCELED.getCode(), cancelType) && Objects.equals(OrderStatusEnum.FULFILL.getCode(), orderStatus)) {
            // 订单已履约手动取消
            OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.FULFILL);
            orderStateMachine.fire(OrderStatusChangeEnum.ORDER_FULFILLED_MANUAL_CANCELLED, orderInfo);
        } else if (Objects.equals(OrderCancelTypeEnum.TIMEOUT_CANCELED.getCode(), cancelType)) {
            // 订单自动超时未支付订单取消
            OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.CREATED);
            orderStateMachine.fire(OrderStatusChangeEnum.ORDER_UN_PAID_AUTO_TIMEOUT_CANCELLED, orderInfo);
        }
    }
}
