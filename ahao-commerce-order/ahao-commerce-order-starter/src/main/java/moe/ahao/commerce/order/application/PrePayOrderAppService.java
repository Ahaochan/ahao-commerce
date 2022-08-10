package moe.ahao.commerce.order.application;

import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.common.enums.PayTypeEnum;
import moe.ahao.commerce.order.api.command.PrePayOrderCommand;
import moe.ahao.commerce.order.api.dto.PrePayOrderDTO;
import moe.ahao.commerce.common.enums.BusinessIdentifierEnum;
import moe.ahao.commerce.order.infrastructure.enums.PayStatusEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.PayGateway;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderPaymentDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderPaymentDetailMapper;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.OrderStateMachine;
import moe.ahao.commerce.order.infrastructure.component.statemachine.factory.StateMachineFactory;
import moe.ahao.commerce.pay.api.command.PayOrderCommand;
import moe.ahao.commerce.pay.api.dto.PayOrderDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrePayOrderAppService {
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

    /**
     * 预支付订单
     */
    @Transactional(rollbackFor = Exception.class)
    public PrePayOrderDTO prePayOrder(PrePayOrderCommand command) {
        // 1. 入参检查
        this.check(command);

        // 2. 加分布式锁（与订单支付回调时加的是同一把锁）
        String orderId = command.getOrderId();
        String lockKey = RedisLockKeyConstants.ORDER_PAY_KEY + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw OrderExceptionEnum.ORDER_PRE_PAY_ERROR.msg();
        }

        try {
            // 3. 冥等性检查
            BigDecimal payAmount = command.getPayAmount();
            this.checkPrePayOrderInfo(orderId, payAmount);

            // 4. 调用支付系统进行预支付
            PrePayOrderDTO payOrderDTO = this.doPrePay(command);

            // 5. 状态流转是created to created, 状态是不会变化, 拿到订单预支付的事件, 更新订单表与支付信息表
            OrderStatusChangeEnum statusChangeEnum = OrderStatusChangeEnum.ORDER_PREPAY;
            OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(statusChangeEnum.getFromStatus());
            orderStateMachine.fire(statusChangeEnum, payOrderDTO);

            return payOrderDTO;
        } finally {
            // 6. 释放分布式锁
            lock.unlock();
        }
    }

    /**
     * 检查预支付接口入参
     */
    private void check(PrePayOrderCommand command) {
        String orderId = command.getOrderId();
        if (StringUtils.isEmpty(orderId)) {
            throw OrderExceptionEnum.ORDER_ID_IS_NULL.msg();
        }
        BigDecimal payAmount = command.getPayAmount();
        if (payAmount == null) {
            throw OrderExceptionEnum.PAY_TYPE_PARAM_ERROR.msg();
        }

        String userId = command.getUserId();
        if (StringUtils.isEmpty(userId)) {
            throw OrderExceptionEnum.USER_ID_IS_NULL.msg();
        }

        Integer businessIdentifier = command.getBusinessIdentifier();
        if (businessIdentifier == null) {
            throw OrderExceptionEnum.BUSINESS_IDENTIFIER_IS_NULL.msg();
        }
        BusinessIdentifierEnum businessIdentifierEnum = BusinessIdentifierEnum.getByCode(businessIdentifier);
        if (businessIdentifierEnum == null) {
            throw OrderExceptionEnum.BUSINESS_IDENTIFIER_ERROR.msg();
        }

        Integer payType = command.getPayType();
        if (payType == null) {
            throw OrderExceptionEnum.PAY_TYPE_PARAM_ERROR.msg();
        }
        PayTypeEnum payTypeEnum = PayTypeEnum.getByCode(payType);
        if (payTypeEnum == null) {
            throw OrderExceptionEnum.PAY_TYPE_PARAM_ERROR.msg();
        }
    }

    /**
     * 预支付订单的前置检查
     */
    private void checkPrePayOrderInfo(String orderId, BigDecimal payAmount) {
        // 查询订单信息
        OrderInfoDO orderInfoDO = orderInfoMapper.selectOneByOrderId(orderId);
        OrderPaymentDetailDO orderPaymentDetailDO = orderPaymentDetailMapper.selectOneByOrderId(orderId);
        if (orderInfoDO == null || orderPaymentDetailDO == null) {
            throw OrderExceptionEnum.ORDER_INFO_IS_NULL.msg();
        }

        // 检查订单支付金额
        if (payAmount.compareTo(orderInfoDO.getPayAmount()) != 0) {
            throw OrderExceptionEnum.ORDER_PAY_AMOUNT_ERROR.msg();
        }

        // 判断一下订单状态, 只有待支付状态才能发起预支付
        boolean isCreated = OrderStatusEnum.CREATED.getCode().equals(orderInfoDO.getOrderStatus());
        if (!isCreated) {
            throw OrderExceptionEnum.ORDER_STATUS_ERROR.msg();
        }

        // 判断一下支付状态, 只有待支付状态才能发起预支付
        boolean isPaid = PayStatusEnum.PAID.getCode().equals(orderPaymentDetailDO.getPayStatus());
        if (isPaid) {
            throw OrderExceptionEnum.ORDER_PAY_STATUS_IS_PAID.msg();
        }

        // 判断是否超过了支付超时时间
        Date curDate = new Date();
        if (curDate.after(orderInfoDO.getExpireTime())) {
            throw OrderExceptionEnum.ORDER_PRE_PAY_EXPIRE_ERROR.msg();
        }
    }

    private PrePayOrderDTO doPrePay(PrePayOrderCommand command) {
        PayOrderCommand payOrderCommand = new PayOrderCommand();
        payOrderCommand.setUserId(command.getUserId());
        payOrderCommand.setBusinessIdentifier(command.getBusinessIdentifier());
        payOrderCommand.setPayType(command.getPayType());
        payOrderCommand.setOrderId(command.getOrderId());
        payOrderCommand.setPayAmount(command.getPayAmount());
        payOrderCommand.setCallbackUrl(command.getCallbackUrl());
        payOrderCommand.setCallbackFailUrl(command.getCallbackFailUrl());
        payOrderCommand.setOpenid(command.getOpenid());
        payOrderCommand.setSubject(command.getSubject());
        payOrderCommand.setItemInfo(command.getItemInfo());

        PayOrderDTO payOrderDTO = payGateway.payOrder(payOrderCommand);

        PrePayOrderDTO dto = new PrePayOrderDTO();
        dto.setOrderId(payOrderDTO.getOrderId());
        dto.setOutTradeNo(payOrderDTO.getOutTradeNo());
        dto.setPayType(payOrderDTO.getPayType());
        dto.setPayData(payOrderDTO.getPayData());
        return dto;
    }



    /**
     * 更新主订单支付信息
     */
    private void updateMasterOrderPaymentInfo(String orderId, Integer payType, Date payTime, String outTradeNo) {
        List<String> orderIds = Collections.singletonList(orderId);
        // 更新订单表支付信息
        updateOrderInfo(orderIds, payType, payTime);
        // 更新支付明细信息
        updateOrderPaymentDetail(orderIds, payType, payTime, outTradeNo);
    }

    /**
     * 更新子订单支付信息
     */
    private void updateSubOrderPaymentInfo(String orderId, Integer payType, Date payTime, String outTradeNo) {
        // 判断是否存在子订单，不存在则不处理
        List<OrderInfoDO> subOrderList = orderInfoMapper.selectListByParentOrderId(orderId);
        if (CollectionUtils.isEmpty(subOrderList)) {
            return;
        }
        List<String> subOrderIdList = subOrderList.stream().map(OrderInfoDO::getOrderId).collect(Collectors.toList());

        // 更新子订单支付信息
        updateOrderInfo(subOrderIdList, payType, payTime);

        // 更新子订单支付明细信息
        updateOrderPaymentDetail(subOrderIdList, payType, payTime, outTradeNo);
    }

    /**
     * 更新订单信息表
     */
    private void updateOrderInfo(List<String> orderIds, Integer payType, Date payTime) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return;
        }
        if (orderIds.size() == 1) {
            orderInfoMapper.updatePrePayInfoByOrderId(orderIds.get(0), payType, payTime);
        } else {
            orderInfoMapper.updatePrePayInfoByOrderIds(orderIds, payType, payTime);
        }

    }

    /**
     * 更新订单支付明细表
     */
    private void updateOrderPaymentDetail(List<String> orderIds, Integer payType, Date payTime, String outTradeNo) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return;
        }
        OrderPaymentDetailDO orderPaymentDetailDO = new OrderPaymentDetailDO();
        orderPaymentDetailDO.setPayTime(payTime);
        orderPaymentDetailDO.setPayType(payType);
        orderPaymentDetailDO.setOutTradeNo(outTradeNo);
        if (orderIds.size() == 1) {
            orderPaymentDetailMapper.updatePrePayInfoByOrderId(orderIds.get(0), payType, payTime, outTradeNo);
        } else {
            orderPaymentDetailMapper.updatePrePayInfoByOrderIds(orderIds, payType, payTime, outTradeNo);
        }
    }
}
