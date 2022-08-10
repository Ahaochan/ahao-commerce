package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.pay;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.*;
import moe.ahao.commerce.order.api.command.PayCallbackCommand;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.OrderStateAction;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.OrderStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import moe.ahao.commerce.order.infrastructure.enums.PayStatusEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.PayGateway;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderPaymentDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderPaymentDetailMapper;
import moe.ahao.commerce.pay.api.command.RefundOrderCommand;
import moe.ahao.exception.CommonBizExceptionEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * 订单支付回调Action
 */
@Slf4j
@Component
public class OrderPayCallbackAction extends OrderStateAction<PayCallbackCommand> {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderPaymentDetailMapper orderPaymentDetailMapper;

    @Autowired
    private PayGateway payGateway;

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StateMachineFactory stateMachineFactory;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_PAID;
    }

    @Override
    protected String onStateChangeInternal(OrderStatusChangeEnum event, PayCallbackCommand command) {
        // 1. 入参检查
        String orderId = command.getOrderId();
        Integer payType = command.getPayType();
        this.check(command);

        // 2. 为支付回调操作进行多重分布式锁加锁
        //    加支付分布式锁避免支付系统并发回调
        String orderPayLockKey = RedisLockKeyConstants.ORDER_PAY_KEY + orderId;
        RLock orderPayLock = redissonClient.getLock(orderPayLockKey);
        //    加取消订单分布式锁避免支付和取消订单同时操作同一笔订单
        String cancelOrderLockKey = RedisLockKeyConstants.CANCEL_KEY + orderId;
        RLock cancelOrderLock = redissonClient.getLock(cancelOrderLockKey);
        RLock multiLock = redissonClient.getMultiLock(orderPayLock, cancelOrderLock);
        boolean locked = multiLock.tryLock();
        if (!locked) {
            throw OrderExceptionEnum.ORDER_PAY_CALLBACK_ERROR.msg();
        }
        try {
            // 3. 开启业务流程编排
            this.doPayCallback(command);
        } finally {
            // . 释放分布式锁
            multiLock.unlock();
        }
        return command.getOrderId();
    }

    private String doPayCallback(PayCallbackCommand command) {
        // @Transactional无法生效，需要用编程式事务
        // TODO 子流程异常怎么办???
        return transactionTemplate.execute(transactionStatus -> {
            // 1. 参数校验
            String orderId = command.getOrderId();
            Integer payType = command.getPayType();
            OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
            OrderPaymentDetailDO orderPaymentDetail = orderPaymentDetailMapper.selectOneByOrderId(orderId);
            if (orderInfo == null || orderPaymentDetail == null) {
                throw OrderExceptionEnum.ORDER_INFO_IS_NULL.msg();
            }
            if (command.getPayAmount().compareTo(orderInfo.getPayAmount()) != 0) {
                throw OrderExceptionEnum.ORDER_CALLBACK_PAY_AMOUNT_ERROR.msg();
            }

            Integer orderStatus = orderInfo.getOrderStatus();
            Integer payStatus = orderPaymentDetail.getPayStatus();
            // 2. 幂等性检查, 只有已创建的订单才能进行支付回调
            boolean isCreated = OrderStatusEnum.CREATED.getCode().equals(orderStatus);
            if (!isCreated) {
                this.payCallbackFailure(orderStatus, payStatus, payType, orderPaymentDetail, orderInfo);
                return null;
            }

            // 3. 如果没有拆单过, 就直接修改主单状态为已支付
            orderInfo.setPayTime(new Date());
            List<OrderInfoDO> subOrders = orderInfoMapper.selectListByParentOrderId(orderId);
            if (CollectionUtils.isEmpty(subOrders)) {
                // 3.1. 执行主单已支付的逻辑
                this.doMasterOrderPaid(orderInfo, orderId);
                // 3.2. 虚拟订单直接扭转为签收
                this.doVirtualOrderSignAction(orderInfo);
                return orderId;
            }

            // 4. 如果发生过拆单, 将父订单状态变更为失效
            this.doMasterOrderInvalidAction(orderInfo);

            // 5. 遍历子订单, 和主单一样的处理逻辑
            for (OrderInfoDO subOrderInfo : subOrders) {
                subOrderInfo.setPayTime(orderInfo.getPayTime());
                // 5.1. 执行子单已支付的逻辑
                this.doSubOrderPaidAction(subOrderInfo);
                // 5.2. 虚拟订单直接扭转为签收
                this.doVirtualOrderSignAction(subOrderInfo);
            }
            return orderId;
        });
    }

    /**
     * 检查订单支付回调接口入参
     */
    private void check(PayCallbackCommand command) {
        if (command == null) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }

        // 订单号
        String orderId = command.getOrderId();
        if (StringUtils.isEmpty(orderId)) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }

        // 支付金额
        BigDecimal payAmount = command.getPayAmount();
        if (payAmount == null) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }

        // 支付系统交易流水号
        String outTradeNo = command.getOutTradeNo();
        if (StringUtils.isEmpty(outTradeNo)) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }

        // 支付类型
        Integer payType = command.getPayType();
        if (payType == null) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }
        PayTypeEnum payTypeEnum = PayTypeEnum.getByCode(payType);
        if (payTypeEnum == null) {
            throw OrderExceptionEnum.PAY_TYPE_PARAM_ERROR.msg();
        }

        // 商户ID
        String merchantId = command.getMerchantId();
        if (StringUtils.isEmpty(merchantId)) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }
    }

    /**
     * 支付回调异常的时候处理逻辑
     */
    public void payCallbackFailure(Integer orderStatus, Integer payStatus, Integer payType, OrderPaymentDetailDO orderPaymentDetailDO, OrderInfoDO orderInfoDO) {
        // 如果订单状态是取消状态, 可能是支付回调前就取消了订单，也有可能支付回调成功后取消了订单
        if (OrderStatusEnum.CANCELLED.getCode().equals(orderStatus)) {
            this.doPayCallbackFailureCancel(payStatus, payType, orderPaymentDetailDO);
        }
        // 如果订单状态不是取消状态, 那么就是已支付、已履约、已出库、配送中等状态
        else {
            this.doPayCallbackFailureOther(payStatus, payType, orderPaymentDetailDO);
        }
    }

    /**
     * 执行支付回调时订单已取消的处理逻辑
     */
    private void doPayCallbackFailureCancel(Integer payStatus, Integer payType, OrderPaymentDetailDO orderPaymentDetailDO) {
        // 1. 如果支付回调时（说明支付系统已经扣了用户的钱）, 订单状态是已取消, 并且支付状态是未支付（说明支付系统还没有完成回调）
        //    说明当时用户在取消订单的时候, 支付系统还没有完成回调. 而支付系统已经扣了用户的钱, 所以现在回调这里要调用一下退款.
        if (PayStatusEnum.UNPAID.getCode().equals(payStatus)) {
            this.executeOrderRefund(orderPaymentDetailDO);
            throw OrderExceptionEnum.ORDER_CANCEL_PAY_CALLBACK_ERROR.msg();
        }
        // 2. 如果支付回调时（说明支付系统已经扣了用户的钱）, 订单状态是已取消, 并且支付状态是已支付（说明支付系统已经回调过一次了）
        //    说明当时用户当时在取消订单的时候, 订单已经不是"已创建"状态了, 可能是已支付之后的状态了.
        if (PayStatusEnum.PAID.getCode().equals(payStatus)) {
            boolean isSamePayType = payType.equals(orderPaymentDetailDO.getPayType());
            if (isSamePayType) {
                // 2.1. 如果是相同的支付方式, 说明用户是没有发起重复付款的, 只是之前的那次支付重复回调了.
                //      取消的订单是不会变成已支付的. 这种场景是支付完成后，执行了取消订单的操作，取消订单本身就会进行退款，所以这里不用进行退款
                throw OrderExceptionEnum.ORDER_CANCEL_PAY_CALLBACK_PAY_TYPE_SAME_ERROR.msg();
            } else {
                // 2.2. 如果不是相同的支付方式, 说明用户更换了不同的支付方式进行了重复付款
                //      正常的支付方式走上面的逻辑, 这种不同的支付方式需要调用一下退款
                //      而非同种支付方式的话，说明用户还是更换了不同支付方式进行了多次扣款，所以需要调用一下退款接口
                this.executeOrderRefund(orderPaymentDetailDO);
                throw OrderExceptionEnum.ORDER_CANCEL_PAY_CALLBACK_PAY_TYPE_NO_SAME_ERROR.msg();
            }
        }
    }

    /**
     * 执行支付回调时订单已履约、已出库、配送中等状态的处理逻辑
     */
    private void doPayCallbackFailureOther(Integer payStatus, Integer payType, OrderPaymentDetailDO orderPaymentDetailDO) {
        // 2. 如果订单状态不是取消状态, 那么就是已支付、已履约、已出库、配送中等状态
        if (PayStatusEnum.PAID.getCode().equals(payStatus)) {
            // 2.1. 如果是相同的支付方式, 说明用户是没有发起重复付款的, 只是重复回调了, 做好幂等直接return就好了.
            boolean isSamePayType = payType.equals(orderPaymentDetailDO.getPayType());
            if (isSamePayType) {
                return;
            }

            // 2.2. 如果不是相同的支付方式, 说明用户还是更换了不同支付方式进行了多次扣款，所以需要调用一下退款接口
            this.executeOrderRefund(orderPaymentDetailDO);
            throw OrderExceptionEnum.ORDER_CANCEL_PAY_CALLBACK_REPEAT_ERROR.msg();
        }
    }

    /**
     * 执行订单退款
     */
    private void executeOrderRefund(OrderPaymentDetailDO orderPaymentDetailDO) {
        RefundOrderCommand command = new RefundOrderCommand();
        command.setOrderId(orderPaymentDetailDO.getOrderId());
        command.setRefundAmount(orderPaymentDetailDO.getPayAmount());
        command.setOutTradeNo(orderPaymentDetailDO.getOutTradeNo());

        payGateway.executeRefund(command);
    }

    /**
     * 执行主单已支付的逻辑
     */
    private void doMasterOrderPaid(OrderInfoDO orderInfo, String orderId) {
        Integer fromStatus = orderInfo.getOrderStatus();
        Integer toStatus = OrderStatusEnum.PAID.getCode();
        List<String> orderIdList = Collections.singletonList(orderId);

        // 更新主单订单状态和支付时间
        super.updateOrderStatusAndPayTime(orderIdList, fromStatus, toStatus, orderInfo.getPayTime());

        // 更新主单支付明细状态和支付时间
        super.updatePaymentStatusAndPayTime(orderIdList, PayStatusEnum.PAID.getCode(), orderInfo.getPayTime());

        // 新增主单订单状态变更日志
        OrderOperateTypeEnum operateType = OrderOperateTypeEnum.PAID_ORDER;
        String remark = "订单支付回调操作" + fromStatus + "-" + toStatus;
        super.saveOrderOperateLog(orderId, operateType, fromStatus, toStatus, remark);
    }

    /**
     * 通过状态机将主订单作废
     */
    private void doMasterOrderInvalidAction(OrderInfoDO orderInfo) {
        OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.CREATED);
        orderStateMachine.fire(OrderStatusChangeEnum.ORDER_PAID_INVALID, orderInfo);
    }

    /**
     * 通过状态机将子订单已支付
     */
    private void doSubOrderPaidAction(OrderInfoDO subOrderInfo) {
        OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.INVALID);
        orderStateMachine.fire(OrderStatusChangeEnum.SUB_ORDER_PAID, subOrderInfo);
    }

    /**
     * 通过状态机将虚拟订单已签收
     */
    private void doVirtualOrderSignAction(OrderInfoDO orderInfo) {
        // 通过状态机将虚拟订单已签收
        if (OrderTypeEnum.VIRTUAL.getCode().equals(orderInfo.getOrderType())) {
            OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.PAID);
            orderStateMachine.fire(OrderStatusChangeEnum.VIRTUAL_ORDER_SIGNED, orderInfo);
        }
    }
}
