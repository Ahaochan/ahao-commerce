package com.ruyuan.eshop.order.statemachine.action.order.cancel;

import com.ruyuan.eshop.common.constants.RedisLockKeyConstants;
import com.ruyuan.eshop.common.enums.AfterSaleTypeEnum;
import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.common.enums.OrderStatusEnum;
import com.ruyuan.eshop.common.redis.RedisLock;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.fulfill.domain.request.CancelFulfillRequest;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.dao.OrderItemDAO;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.dto.OrderItemDTO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.entity.OrderItemDO;
import com.ruyuan.eshop.order.domain.request.CancelOrderAssembleRequest;
import com.ruyuan.eshop.order.domain.request.CancelOrderRequest;
import com.ruyuan.eshop.order.enums.ExecuteFulfillMarkEnum;
import com.ruyuan.eshop.order.enums.OrderCancelTypeEnum;
import com.ruyuan.eshop.order.exception.OrderBizException;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.remote.FulfillRemote;
import com.ruyuan.eshop.order.service.RocketMqService;
import com.ruyuan.eshop.order.statemachine.StateMachineFactory;
import com.ruyuan.eshop.order.statemachine.action.OrderStateAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;


/**
 * 订单取消入口Action
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
public class OrderCancelAction extends OrderStateAction<CancelOrderRequest> {

    @Autowired
    private RedisLock redisLock;

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private FulfillRemote fulfillRemote;

    @Autowired
    private OrderConverter orderConverter;

    @Resource
    private StateMachineFactory stateMachineFactory;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RocketMqService rocketMqService;

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
    protected OrderInfoDTO onStateChangeInternal(OrderStatusChangeEnum event, CancelOrderRequest context) {

        //  入参检查
        checkCancelOrderRequestParam(context);

        //  分布式锁
        String key = RedisLockKeyConstants.CANCEL_KEY + context.getOrderId();
        boolean lock = redisLock.tryLock(key);
        if (!lock) {
            throw new OrderBizException(OrderErrorCodeEnum.CANCEL_ORDER_REPEAT);
        }

        try {
            //  组装取消订单数据
            CancelOrderAssembleRequest cancelOrderAssembleRequest = buildAssembleRequest(context);

            //  检查订单状态
            checkCancelOrderStatus(cancelOrderAssembleRequest);

            //  设置调用履约接口标记
            setFulfillMark(cancelOrderAssembleRequest);

            //  执行履约取消、更新订单状态、新增订单日志操作
            cancelOrderFulfillmentAndUpdateOrderStatus(cancelOrderAssembleRequest);

            //  向下游发送释放权益资产MQ
            sendReleaseAssetsMq(cancelOrderAssembleRequest);

        } catch (Exception e) {
            throw new OrderBizException(e.getMessage());
        } finally {
            redisLock.unlock(key);
        }

        // 返回null 不发送订单变更事件
        return null;
    }

    private void sendReleaseAssetsMq(CancelOrderAssembleRequest cancelOrderAssembleRequest) {

        rocketMqService.sendReleaseAssetsMessage(cancelOrderAssembleRequest);
    }

    private void setFulfillMark(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        //  订单信息
        OrderInfoDTO orderInfoDTO = cancelOrderAssembleRequest.getOrderInfoDTO();

        //  所有类型的订单 在"已创建"状态 不调用履约
        if (OrderStatusEnum.CREATED.getCode().equals(orderInfoDTO.getOrderStatus())) {
            cancelOrderAssembleRequest.setExecuteFulfillMark(ExecuteFulfillMarkEnum.CANNOT_EXECUTE_FULFILL.getCode());
            return;
        }

        cancelOrderAssembleRequest.setExecuteFulfillMark(ExecuteFulfillMarkEnum.EXECUTE_FULFILL.getCode());
    }

    /**
     * 执行履约取消、更新订单状态、新增订单日志操作
     */
    public void cancelOrderFulfillmentAndUpdateOrderStatus(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        //  @Transactional无法生效，需要用编程式事务
        transactionTemplate.execute(transactionStatus -> {
            if (ExecuteFulfillMarkEnum.EXECUTE_FULFILL.getCode().equals(cancelOrderAssembleRequest.getExecuteFulfillMark())) {
                // 取消订单调用履约接口
                cancelFulfill(cancelOrderAssembleRequest);
            }
            // 执行具体场景的取消逻辑
            doSpecialOrderCancel(cancelOrderAssembleRequest);
            return true;
        });
    }

    /**
     * 调用履约拦截订单
     */
    private void cancelFulfill(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        //  主单信息
        OrderInfoDTO orderInfoDTO = cancelOrderAssembleRequest.getOrderInfoDTO();
        CancelFulfillRequest cancelFulfillRequest = orderConverter.convertCancelFulfillRequest(orderInfoDTO);

        //  调用履约接口
        fulfillRemote.cancelFulfill(cancelFulfillRequest);
    }

    /**
     * 执行具体场景的取消逻辑
     */
    private void doSpecialOrderCancel(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        OrderCancelTypeEnum cancelType = OrderCancelTypeEnum.getByCode(cancelOrderAssembleRequest.getCancelType());
        OrderStatusEnum orderStatus = OrderStatusEnum.getByCode(cancelOrderAssembleRequest.getOrderInfoDTO().getOrderStatus());

        if (OrderCancelTypeEnum.USER_CANCELED.equals(cancelType) && OrderStatusEnum.CREATED.equals(orderStatus)) {
            // 订单未支付手动取消
            // 状态机流转
            StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.CREATED);
            orderStateMachine.fire(OrderStatusChangeEnum.ORDER_UN_PAID_MANUAL_CANCELLED, cancelOrderAssembleRequest);
        } else if (OrderCancelTypeEnum.USER_CANCELED.equals(cancelType) && OrderStatusEnum.PAID.equals(orderStatus)) {
            // 订单已支付手动取消
            // 状态机流转
            StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.PAID);
            orderStateMachine.fire(OrderStatusChangeEnum.ORDER_PAID_MANUAL_CANCELLED, cancelOrderAssembleRequest);
        } else if (OrderCancelTypeEnum.USER_CANCELED.equals(cancelType) && OrderStatusEnum.FULFILL.equals(orderStatus)) {
            // 订单已履约手动取消
            // 状态机流转
            StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.FULFILL);
            orderStateMachine.fire(OrderStatusChangeEnum.ORDER_FULFILLED_MANUAL_CANCELLED, cancelOrderAssembleRequest);
        } else if (OrderCancelTypeEnum.TIMEOUT_CANCELED.equals(cancelType)) {
            // 订单自动超时未支付订单取消
            // 状态机流转
            StateMachineFactory.OrderStateMachine orderStateMachine = stateMachineFactory.getOrderStateMachine(OrderStatusEnum.CREATED);
            orderStateMachine.fire(OrderStatusChangeEnum.ORDER_UN_PAID_AUTO_TIMEOUT_CANCELLED, cancelOrderAssembleRequest);
        }
    }

    private void checkCancelOrderStatus(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        OrderInfoDTO orderInfoDTO = cancelOrderAssembleRequest.getOrderInfoDTO();
        //  大于已出库状态的订单不能取消, 注:虚拟订单在支付后会自动更新成"已签收",所以已支付的虚拟订单不会被取消
        if (orderInfoDTO.getOrderStatus() >= OrderStatusEnum.OUT_STOCK.getCode()) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_STATUS_CHANGED);
        }
        //  已取消的订单不能重复取消
        if (orderInfoDTO.getOrderStatus().equals(OrderStatusEnum.CANCELLED.getCode())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_STATUS_CANCELED);
        }
    }

    /**
     * 组装 取消订单 数据
     */
    private CancelOrderAssembleRequest buildAssembleRequest(CancelOrderRequest cancelOrderRequest) {
        String orderId = cancelOrderRequest.getOrderId();
        //  查询订单信息
        OrderInfoDO orderInfoDO = orderInfoDAO.getByOrderId(orderId);

        //  查询订单条目
        List<OrderItemDO> orderItemDOList = orderItemDAO.listByOrderId(orderId);
        List<OrderItemDTO> orderItemDTOList = orderConverter.orderItemDO2DTO(orderItemDOList);

        //  组装数据
        OrderInfoDTO orderInfoDTO = orderConverter.orderInfoDO2DTO(orderInfoDO);
        orderInfoDTO.setCancelType(String.valueOf(cancelOrderRequest.getCancelType()));
        CancelOrderAssembleRequest cancelOrderAssembleRequest = orderConverter.convertCancelOrderRequest(cancelOrderRequest);
        cancelOrderAssembleRequest.setOrderId(orderId);
        cancelOrderAssembleRequest.setOrderItemDTOList(orderItemDTOList);
        cancelOrderAssembleRequest.setOrderInfoDTO(orderInfoDTO);
        cancelOrderAssembleRequest.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());

        return cancelOrderAssembleRequest;
    }

    /**
     * 入参检查
     *
     * @param cancelOrderRequest 取消订单入参
     */
    private void checkCancelOrderRequestParam(CancelOrderRequest cancelOrderRequest) {
        ParamCheckUtil.checkObjectNonNull(cancelOrderRequest);

        //  订单ID
        String orderId = cancelOrderRequest.getOrderId();
        ParamCheckUtil.checkStringNonEmpty(orderId, OrderErrorCodeEnum.CANCEL_ORDER_ID_IS_NULL);

        //  业务线标识
        Integer businessIdentifier = cancelOrderRequest.getBusinessIdentifier();
        ParamCheckUtil.checkObjectNonNull(businessIdentifier, OrderErrorCodeEnum.BUSINESS_IDENTIFIER_IS_NULL);

        //  订单取消类型
        Integer cancelType = cancelOrderRequest.getCancelType();
        ParamCheckUtil.checkObjectNonNull(cancelType, OrderErrorCodeEnum.CANCEL_TYPE_IS_NULL);

        //  用户ID
        String userId = cancelOrderRequest.getUserId();
        ParamCheckUtil.checkStringNonEmpty(userId, OrderErrorCodeEnum.USER_ID_IS_NULL);

        //  订单类型
        Integer orderType = cancelOrderRequest.getOrderType();
        ParamCheckUtil.checkObjectNonNull(orderType, OrderErrorCodeEnum.ORDER_TYPE_IS_NULL);

        //  请求入参的订单状态参数校验
        Integer orderStatus = cancelOrderRequest.getOrderStatus();
        ParamCheckUtil.checkObjectNonNull(orderStatus, OrderErrorCodeEnum.ORDER_STATUS_IS_NULL);

        if (orderStatus.equals(OrderStatusEnum.CANCELLED.getCode())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_STATUS_CANCELED);
        }

        if (orderStatus >= OrderStatusEnum.OUT_STOCK.getCode()) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_STATUS_CHANGED);
        }
    }
}
