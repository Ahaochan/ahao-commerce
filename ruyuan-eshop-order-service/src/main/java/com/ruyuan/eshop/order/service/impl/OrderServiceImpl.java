package com.ruyuan.eshop.order.service.impl;

import com.ruyuan.eshop.common.constants.RedisLockKeyConstants;
import com.ruyuan.eshop.common.enums.DeleteStatusEnum;
import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.common.enums.OrderStatusEnum;
import com.ruyuan.eshop.common.enums.PayTypeEnum;
import com.ruyuan.eshop.common.redis.RedisLock;
import com.ruyuan.eshop.common.utils.LoggerFormat;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.OrderDeliveryDetailDAO;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.dao.OrderPaymentDetailDAO;
import com.ruyuan.eshop.order.domain.dto.CreateOrderDTO;
import com.ruyuan.eshop.order.domain.dto.GenOrderIdDTO;
import com.ruyuan.eshop.order.domain.dto.PrePayOrderDTO;
import com.ruyuan.eshop.order.domain.entity.OrderDeliveryDetailDO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.entity.OrderPaymentDetailDO;
import com.ruyuan.eshop.order.domain.request.*;
import com.ruyuan.eshop.order.enums.OrderNoTypeEnum;
import com.ruyuan.eshop.order.enums.PayStatusEnum;
import com.ruyuan.eshop.order.exception.OrderBizException;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.manager.OrderNoManager;
import com.ruyuan.eshop.order.remote.PayRemote;
import com.ruyuan.eshop.order.service.OrderService;
import com.ruyuan.eshop.order.statemachine.StateMachineFactory;
import com.ruyuan.eshop.pay.domain.dto.PayOrderDTO;
import com.ruyuan.eshop.pay.domain.request.PayOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shardingsphere.api.hint.HintManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private OrderNoManager orderNoManager;

    @Autowired
    private RedisLock redisLock;

    @Autowired
    private PayRemote payRemote;

    @Autowired
    private OrderConverter orderConverter;

    @Autowired
    private StateMachineFactory stateMachineFactory;

    /**
     * 生成订单号接口
     *
     * @param genOrderIdRequest 生成订单号入参
     * @return 订单号
     */
    @Override
    public GenOrderIdDTO genOrderId(GenOrderIdRequest genOrderIdRequest) {
        log.info(LoggerFormat.build()
                .remark("genOrderId->request")
                .data("request", genOrderIdRequest)
                .finish());

        // 参数检查
        String userId = genOrderIdRequest.getUserId();
        ParamCheckUtil.checkStringNonEmpty(userId);
        Integer businessIdentifier = genOrderIdRequest.getBusinessIdentifier();
        ParamCheckUtil.checkObjectNonNull(businessIdentifier);

        String orderId = orderNoManager.genOrderId(OrderNoTypeEnum.SALE_ORDER.getCode(), userId);
        GenOrderIdDTO genOrderIdDTO = new GenOrderIdDTO();
        genOrderIdDTO.setOrderId(orderId);

        log.info(LoggerFormat.build()
                .remark("genOrderId->response")
                .data("response", genOrderIdDTO)
                .finish());
        return genOrderIdDTO;
    }

    /**
     * 提交订单/生成订单接口
     *
     * @param createOrderRequest 提交订单请求入参
     * @return 订单号
     */
    @Override
    public CreateOrderDTO createOrder(CreateOrderRequest createOrderRequest) {
        log.info(LoggerFormat.build()
                .remark("createOrder->request")
                .data("request", createOrderRequest)
                .finish());

        // 状态机流转
        StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.NULL);
        orderStateMachine.fire(OrderStatusChangeEnum.ORDER_CREATED, createOrderRequest);

        // 返回订单信息
        CreateOrderDTO createOrderDTO = new CreateOrderDTO();
        createOrderDTO.setOrderId(createOrderRequest.getOrderId());
        return createOrderDTO;
    }


    /**
     * 预支付订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrePayOrderDTO prePayOrder(PrePayOrderRequest prePayOrderRequest) {

        HintManager hintManager = HintManager.getInstance();
        try {
            // 订单预支付流程事务既有写，也有读，并且是读操作在前，
            // 强制所有的查询从主库查询
            hintManager.setMasterRouteOnly();

            log.info(LoggerFormat.build()
                    .remark("prePayOrder->request")
                    .data("request", prePayOrderRequest)
                    .finish());

            // 提取业务参数
            String orderId = prePayOrderRequest.getOrderId();
            Integer payAmount = prePayOrderRequest.getPayAmount();

            // 入参检查
            checkPrePayOrderRequestParam(prePayOrderRequest, orderId, payAmount);

            // 加分布式锁（与订单支付回调时加的是同一把锁）
            String key = RedisLockKeyConstants.ORDER_PAY_KEY + orderId;
            prePayOrderLock(key);
            try {
                // 幂等性检查
                checkPrePayOrderInfo(orderId, payAmount);

                // 调用支付系统进行预支付
                PayOrderRequest payOrderRequest = orderConverter.convertPayOrderRequest(prePayOrderRequest);
                PayOrderDTO payOrderDTO = payRemote.payOrder(payOrderRequest);

                // 状态机流转 -> 更新支付信息
                OrderStatusChangeEnum statusChangeEnum = OrderStatusChangeEnum.ORDER_PREPAY;
                StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(statusChangeEnum.getFromStatus());
                orderStateMachine.fire(statusChangeEnum, payOrderDTO);

                // 返回结果
                PrePayOrderDTO prePayOrderDTO = orderConverter.convertPrePayOrderRequest(payOrderDTO);
                log.info(LoggerFormat.build()
                        .remark("prePayOrder->response")
                        .data("response", prePayOrderDTO)
                        .finish());
                return prePayOrderDTO;
            } finally {
                // 释放分布式锁
                redisLock.unlock(key);
            }

        }finally {
            hintManager.close();
        }
    }

    /**
     * 预支付加分布式锁
     */
    private void prePayOrderLock(String key) {
        boolean lock = redisLock.tryLock(key);
        if (!lock) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_PRE_PAY_ERROR);
        }
    }

    /**
     * 预支付订单的前置检查
     */
    private void checkPrePayOrderInfo(String orderId, Integer payAmount) {
        // 查询订单信息
        OrderInfoDO orderInfoDO = orderInfoDAO.getByOrderId(orderId);
        OrderPaymentDetailDO orderPaymentDetailDO = orderPaymentDetailDAO.getPaymentDetailByOrderId(orderId);
        if (orderInfoDO == null || orderPaymentDetailDO == null) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_INFO_IS_NULL);
        }

        // 检查订单支付金额
        if (!payAmount.equals(orderInfoDO.getPayAmount())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_PAY_AMOUNT_ERROR);
        }

        // 判断一下订单状态
        if (!OrderStatusEnum.CREATED.getCode().equals(orderInfoDO.getOrderStatus())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 判断一下支付状态
        if (PayStatusEnum.PAID.getCode().equals(orderPaymentDetailDO.getPayStatus())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_PAY_STATUS_IS_PAID);
        }

        // 判断是否超过了支付超时时间
        Date curDate = new Date();
        if (curDate.after(orderInfoDO.getExpireTime())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_PRE_PAY_EXPIRE_ERROR);
        }
    }

    /**
     * 检查预支付接口入参
     */
    private void checkPrePayOrderRequestParam(PrePayOrderRequest prePayOrderRequest, String orderId, Integer payAmount) {
        String userId = prePayOrderRequest.getUserId();
        ParamCheckUtil.checkStringNonEmpty(userId, OrderErrorCodeEnum.USER_ID_IS_NULL);

        String businessIdentifier = prePayOrderRequest.getBusinessIdentifier();
        ParamCheckUtil.checkStringNonEmpty(businessIdentifier, OrderErrorCodeEnum.BUSINESS_IDENTIFIER_ERROR);

        Integer payType = prePayOrderRequest.getPayType();
        ParamCheckUtil.checkObjectNonNull(payType, OrderErrorCodeEnum.PAY_TYPE_PARAM_ERROR);
        if (PayTypeEnum.getByCode(payType) == null) {
            throw new OrderBizException(OrderErrorCodeEnum.PAY_TYPE_PARAM_ERROR);
        }

        ParamCheckUtil.checkStringNonEmpty(orderId, OrderErrorCodeEnum.ORDER_ID_IS_NULL);
        ParamCheckUtil.checkObjectNonNull(payAmount, OrderErrorCodeEnum.PAY_TYPE_PARAM_ERROR);
    }

    /**
     * 支付回调
     * 支付回调有2把分布式锁的原因说明：同一笔订单在同一时间只能支付or取消
     * 不可以同时对一笔订单，既发起支付，又发起取消
     */
    @Override
    public void payCallback(PayCallbackRequest payCallbackRequest) {
        log.info(LoggerFormat.build()
                .remark("payCallback->request")
                .data("request", payCallbackRequest)
                .finish());

        HintManager hintManager = HintManager.getInstance();
        try {
            
            // 订单支付回调流程事务既有写，也有读，并且是读操作在前，
            // 强制所有的查询从主库查询
            hintManager.setMasterRouteOnly();

            // 状态机流转
            OrderStatusChangeEnum event = OrderStatusChangeEnum.ORDER_PAID;
            StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(event.getFromStatus());
            orderStateMachine.fire(event, payCallbackRequest);

        }finally {
            hintManager.close();
        }
    }


    @Override
    public void removeOrders(List<String> orderIds) {
        //1、根据id查询订单
        List<OrderInfoDO> orders = orderInfoDAO.listByOrderIds(orderIds);
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }

        //2、校验订单是否可以移除
        orders.forEach(order -> {
            if (!canRemove(order)) {
                throw new OrderBizException(OrderErrorCodeEnum.ORDER_CANNOT_REMOVE);
            }
        });

        //3、对订单进行软删除
        orderInfoDAO.softRemoveOrders(orderIds);
    }

    private boolean canRemove(OrderInfoDO order) {
        return OrderStatusEnum.canRemoveStatus().contains(order.getOrderStatus()) &&
                DeleteStatusEnum.NO.getCode().equals(order.getDeleteStatus());
    }

    @Override
    public void adjustDeliveryAddress(AdjustDeliveryAddressRequest request) {
        //1、根据id查询订单
        OrderInfoDO order = orderInfoDAO.getByOrderId(request.getOrderId());
        ParamCheckUtil.checkObjectNonNull(order, OrderErrorCodeEnum.ORDER_NOT_FOUND);

        //2、校验订单是否未出库
        if (!OrderStatusEnum.unOutStockStatus().contains(order.getOrderStatus())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_NOT_ALLOW_TO_ADJUST_ADDRESS);
        }

        //3、查询订单配送信息
        OrderDeliveryDetailDO orderDeliveryDetail = orderDeliveryDetailDAO.getByOrderId(request.getOrderId());
        if (null == orderDeliveryDetail) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_DELIVERY_NOT_FOUND);
        }

        //4、校验配送信息是否已经被修改过一次
        if (orderDeliveryDetail.getModifyAddressCount() > 0) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_DELIVERY_ADDRESS_HAS_BEEN_ADJUSTED);
        }

        //5、更新配送地址信息
        orderDeliveryDetailDAO.updateDeliveryAddress(orderDeliveryDetail.getOrderId()
                , orderDeliveryDetail.getModifyAddressCount(), request);
    }
}