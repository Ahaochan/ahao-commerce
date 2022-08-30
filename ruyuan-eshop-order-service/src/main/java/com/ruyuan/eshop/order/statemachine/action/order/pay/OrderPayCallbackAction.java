package com.ruyuan.eshop.order.statemachine.action.order.pay;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.common.collect.Lists;
import com.ruyuan.eshop.common.constants.RedisLockKeyConstants;
import com.ruyuan.eshop.common.enums.*;
import com.ruyuan.eshop.common.redis.RedisLock;
import com.ruyuan.eshop.common.utils.LoggerFormat;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.dao.OrderPaymentDetailDAO;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.entity.OrderPaymentDetailDO;
import com.ruyuan.eshop.order.domain.request.PayCallbackRequest;
import com.ruyuan.eshop.order.domain.request.SubOrderPaidRequest;
import com.ruyuan.eshop.order.enums.PayStatusEnum;
import com.ruyuan.eshop.order.exception.OrderBizException;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.remote.PayRemote;
import com.ruyuan.eshop.order.statemachine.StateMachineFactory;
import com.ruyuan.eshop.order.statemachine.action.OrderStateAction;
import com.ruyuan.eshop.pay.domain.request.PayRefundRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.ruyuan.eshop.order.exception.OrderErrorCodeEnum.ORDER_CANCEL_PAY_CALLBACK_ERROR;

/**
 * 订单支付回调Action
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
public class OrderPayCallbackAction extends OrderStateAction<PayCallbackRequest> {

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private PayRemote payRemote;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private RedisLock redisLock;

    @Autowired
    private StateMachineFactory stateMachineFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    protected OrderInfoDTO onStateChangeInternal(OrderStatusChangeEnum event, PayCallbackRequest context) {
        // 提取请求参数中的数据
        String orderId = context.getOrderId();
        Integer payType = context.getPayType();

        // 从数据库中查询出当前订单信息
        OrderInfoDO orderInfoDO = orderInfoDAO.getByOrderId(orderId);
        OrderPaymentDetailDO orderPaymentDetailDO = orderPaymentDetailDAO.getPaymentDetailByOrderId(orderId);

        // 入参检查
        checkPayCallbackRequestParam(context, orderInfoDO, orderPaymentDetailDO);

        // 为支付回调操作进行多重分布式锁加锁
        List<String> redisKeyList = Lists.newArrayList();
        payCallbackMultiLock(redisKeyList, orderId);
        List<OrderInfoDO> subOrders;
        try {
            Integer orderStatus = orderInfoDO.getOrderStatus();
            Integer payStatus = orderPaymentDetailDO.getPayStatus();

            // 幂等性检查
            if (!OrderStatusEnum.CREATED.getCode().equals(orderStatus)) {
                // 异常场景处理
                payCallbackFailure(orderStatus, payStatus, payType, orderPaymentDetailDO, orderInfoDO);
                return null;
            }

            // 设置支付时间
            orderInfoDO.setPayTime(context.getPayTime());

            // 执行正式的订单支付回调处理
            // 如果订单状态是 "已创建"，直接更新订单状态为已支付
            subOrders = updateMasterOrderStatus(orderInfoDO);

            log.info(LoggerFormat.build()
                    .remark("payCallback->response")
                    .finish());
        } catch (Exception e) {
            log.error("payCallback error", e);
            throw new OrderBizException(e.getMessage());
        } finally {
            // 释放分布式锁
            redisLock.unMultiLock(redisKeyList);
        }
        OrderInfoDTO orderInfoDTO = orderConverter.orderInfoDO2DTO(orderInfoDO);
        orderInfoDTO.setSubOrders(subOrders);
        // 返回标准的订单信息
        return orderInfoDTO;
    }

    @Override
    public OrderStatusChangeEnum event() {
        return OrderStatusChangeEnum.ORDER_PAID;
    }


    @Override
    protected void postStateChange(OrderStatusChangeEnum event, OrderInfoDTO context) {
        // 先发送主订单的标准消息
        super.postStateChange(event, context);

        // 不存在子订单并且是虚拟订单，需要执行虚拟订单签收逻辑
        if (context.isVirtual() && !context.hasChild()) {
            doVirtualOrderSignAction(orderConverter.orderInfoDTO2DO(context));
            return;
        }
        // 如果是普通订单没有子单
        else if (!context.hasChild()) {
            return;
        }

        // 存在子订单，需要将父订单状态变更为失效
        doMasterOrderInvalidAction(context);

        // 变更子订单状态
        for (OrderInfoDO subOrder : context.getSubOrders()) {
            // 虚拟子订单状态变更为已签收
            if (OrderTypeEnum.VIRTUAL.getCode().equals(subOrder.getOrderType())) {
                doVirtualOrderSignAction(subOrder);
            }
            // 普通子订单状态变更为已支付
            else {
                doSubOrderPaidAction(subOrder, context.getPayTime());
            }
        }
    }

    /**
     * 通过状态机将子订单已支付
     */
    private void doSubOrderPaidAction(OrderInfoDO subOrder, Date payTime) {
        StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.INVALID);
        orderStateMachine.fire(OrderStatusChangeEnum.SUB_ORDER_PAID
                , new SubOrderPaidRequest(subOrder, payTime));
    }

    /**
     * 通过状态机将主订单失效
     */
    private void doMasterOrderInvalidAction(OrderInfoDTO context) {
        // 存在子订单，需要将父订单状态变更为失效,
        StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.CREATED);
        orderStateMachine.fire(OrderStatusChangeEnum.ORDER_PAID_INVALID, orderConverter.orderInfoDTO2DO(context));
    }


    /**
     * 通过状态机将虚拟订单已签收
     */
    private void doVirtualOrderSignAction(OrderInfoDO order) {
        // 通过状态机将虚拟订单已签收
        StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.PAID);
        orderStateMachine.fire(OrderStatusChangeEnum.VIRTUAL_ORDER_SIGNED, order);
    }

    /**
     * 更新主单状态
     *
     * @param order 订单信息
     */
    public List<OrderInfoDO> updateMasterOrderStatus(OrderInfoDO order) {
        //  @Transactional无法生效，需要用编程式事务
        return transactionTemplate.execute(transactionStatus -> {
            // 1、查询子订单
            String orderId = order.getOrderId();
            List<OrderInfoDO> subOrders = orderInfoDAO.listByParentOrderId(orderId);

            // 2、判断是否存在子订单
            if (!hasChild(subOrders)) {
                // 执行主单已支付的逻辑
                doMasterOrderPaid(order, orderId);
            }
            return subOrders;
        });
    }

    /**
     * 执行主单已支付的逻辑
     */
    private void doMasterOrderPaid(OrderInfoDO order, String orderId) {
        Integer preOrderStatus = order.getOrderStatus();
        Integer currentStatus = OrderStatusEnum.PAID.getCode();
        List<String> orderIdList = Collections.singletonList(orderId);

        // 更新主单订单状态和支付时间
        updateOrderStatusAndPayTime(orderIdList, currentStatus, order.getPayTime());

        // 更新主单支付明细状态和支付时间
        updatePaymentStatusAndPayTime(orderIdList, PayStatusEnum.PAID.getCode(), order.getPayTime());

        // 新增主单订单状态变更日志
        Integer operateType = OrderOperateTypeEnum.PAID_ORDER.getCode();
        String remark = "订单支付回调操作" + preOrderStatus + "-" + currentStatus;
        saveOrderOperateLog(orderId, operateType, preOrderStatus, currentStatus, remark);
    }

    /**
     * 支付回调加分布式锁
     */
    private void payCallbackMultiLock(List<String> redisKeyList, String orderId) {
        // 加支付分布式锁避免支付系统并发回调
        String orderPayKey = RedisLockKeyConstants.ORDER_PAY_KEY + orderId;
        // 加取消订单分布式锁避免支付和取消订单同时操作同一笔订单
        String cancelOrderKey = RedisLockKeyConstants.CANCEL_KEY + orderId;
        redisKeyList.add(orderPayKey);
        redisKeyList.add(cancelOrderKey);
        boolean lock = redisLock.multiLock(redisKeyList);
        if (!lock) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_PAY_CALLBACK_ERROR);
        }
    }

    /**
     * 检查订单支付回调接口入参
     */
    private void checkPayCallbackRequestParam(PayCallbackRequest payCallbackRequest,
                                              OrderInfoDO orderInfoDO,
                                              OrderPaymentDetailDO orderPaymentDetailDO) {
        ParamCheckUtil.checkObjectNonNull(payCallbackRequest);

        // 订单号
        String orderId = payCallbackRequest.getOrderId();
        ParamCheckUtil.checkStringNonEmpty(orderId);

        // 支付金额
        Integer payAmount = payCallbackRequest.getPayAmount();
        ParamCheckUtil.checkObjectNonNull(payAmount);

        // 支付系统交易流水号
        String outTradeNo = payCallbackRequest.getOutTradeNo();
        ParamCheckUtil.checkStringNonEmpty(outTradeNo);

        // 支付类型
        Integer payType = payCallbackRequest.getPayType();
        ParamCheckUtil.checkObjectNonNull(payType);
        if (PayTypeEnum.getByCode(payType) == null) {
            throw new OrderBizException(OrderErrorCodeEnum.PAY_TYPE_PARAM_ERROR);
        }

        // 商户ID
        String merchantId = payCallbackRequest.getMerchantId();
        ParamCheckUtil.checkStringNonEmpty(merchantId);

        // 校验参数
        if (orderInfoDO == null || orderPaymentDetailDO == null) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_INFO_IS_NULL);
        }
        if (!payAmount.equals(orderInfoDO.getPayAmount())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_CALLBACK_PAY_AMOUNT_ERROR);
        }
    }

    /**
     * 支付回调异常的时候处理逻辑
     */
    public void payCallbackFailure(Integer orderStatus,
                                   Integer payStatus,
                                   Integer payType,
                                   OrderPaymentDetailDO orderPaymentDetailDO,
                                   OrderInfoDO orderInfoDO) {
        // 如果订单那状态是取消状态
        // 可能是支付回调前就取消了订单，也有可能支付回调成功后取消了订单
        if (OrderStatusEnum.CANCELLED.getCode().equals(orderStatus)) {
            doPayCallbackFailureCancel(orderInfoDO, orderPaymentDetailDO, payStatus, payType);
        } else {
            // 如果订单状态不是取消状态（那么就是已履约、已出库、配送中等状态）
            doPayCallbackFailureOther(orderInfoDO, orderPaymentDetailDO, payStatus, payType);
        }
    }

    /**
     * 执行支付回调时订单已取消的处理逻辑
     */
    private void doPayCallbackFailureCancel(OrderInfoDO orderInfoDO,
                                            OrderPaymentDetailDO orderPaymentDetailDO,
                                            Integer payStatus,
                                            Integer payType) {
        // 此时如果订单的支付状态是未支付的话
        // 说明用户在取消订单的时候，支付系统还没有完成回调，而支付系统又已经扣了用户的钱，所以要调用一下退款
        if (PayStatusEnum.UNPAID.getCode().equals(payStatus)) {
            // 调用退款
            executeOrderRefund(orderInfoDO, orderPaymentDetailDO);
            throw new OrderBizException(ORDER_CANCEL_PAY_CALLBACK_ERROR);
        }

        // 此时如果订单的支付状态是已支付的话
        // 说明用户在取消订单的时候，订单已经不是"已创建"状态了
        if (PayStatusEnum.PAID.getCode().equals(payStatus)) {
            if (payType.equals(orderPaymentDetailDO.getPayType())) {
                // 非"已创建"状态订单的取消操作本身就会进行退款的
                // 所以如果是同种支付方式，说明用户并没有进行多次支付，是不需要调用退款接口
                throw new OrderBizException(OrderErrorCodeEnum.ORDER_CANCEL_PAY_CALLBACK_PAY_TYPE_SAME_ERROR);
            } else {
                // 而非同种支付方式的话，说明用户还是更换了不同支付方式进行了多次扣款，所以需要调用一下退款接口
                // 调用退款
                executeOrderRefund(orderInfoDO, orderPaymentDetailDO);
                throw new OrderBizException(OrderErrorCodeEnum.ORDER_CANCEL_PAY_CALLBACK_PAY_TYPE_NO_SAME_ERROR);
            }
        }
    }

    /**
     * 执行支付回调时订单已履约、已出库、配送中等状态的处理逻辑
     */
    private void doPayCallbackFailureOther(OrderInfoDO orderInfoDO,
                                           OrderPaymentDetailDO orderPaymentDetailDO,
                                           Integer payStatus,
                                           Integer payType) {
        if (PayStatusEnum.PAID.getCode().equals(payStatus)) {
            // 如果是同种支付方式回调，说明用户是并没有发起重复付款的，只是支付系统多触发了一次回调
            // 这里做好冥等判断，直接return即可，不需要调用退款接口
            if (payType.equals(orderPaymentDetailDO.getPayType())) {
                return;
            }

            // 如果是非同种支付方式，说明用户更换了不同的支付方式发起了重复付款，所以要调用一下退款接口
            // 调用退款
            executeOrderRefund(orderInfoDO, orderPaymentDetailDO);
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_CANCEL_PAY_CALLBACK_REPEAT_ERROR);
        }
    }

    /**
     * 执行订单退款
     */
    private void executeOrderRefund(OrderInfoDO orderInfoDO, OrderPaymentDetailDO orderPaymentDetailDO) {
        PayRefundRequest payRefundRequest = new PayRefundRequest();
        payRefundRequest.setOrderId(orderInfoDO.getOrderId());
        payRefundRequest.setRefundAmount(orderPaymentDetailDO.getPayAmount());
        payRefundRequest.setOutTradeNo(orderPaymentDetailDO.getOutTradeNo());
        payRemote.executeRefund(payRefundRequest);
    }

    /**
     * 判断是否有子单
     */
    private boolean hasChild(List<OrderInfoDO> subOrders) {
        return CollectionUtils.isNotEmpty(subOrders);
    }
}