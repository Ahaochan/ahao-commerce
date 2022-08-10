package moe.ahao.commerce.order.infrastructure.component.statemachine.action.aftersale;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.CreateReturnGoodsAfterSaleCommand;
import moe.ahao.commerce.aftersale.infrastructure.enums.RefundStatusEnum;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.AfterSaleLogDAO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleRefundDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleItemMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleRefundMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.service.AfterSaleItemMybatisService;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.*;
import moe.ahao.commerce.customer.api.event.CustomerReceiveAfterSaleEvent;
import moe.ahao.commerce.market.api.dto.UserCouponDTO;
import moe.ahao.commerce.market.api.query.GetUserCouponQuery;
import moe.ahao.commerce.order.application.GenOrderIdAppService;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.AfterSaleStateAction;
import moe.ahao.commerce.order.infrastructure.enums.AccountTypeEnum;
import moe.ahao.commerce.order.infrastructure.enums.OrderIdTypeEnum;
import moe.ahao.commerce.order.infrastructure.enums.ReturnGoodsTypeEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.CouponGateway;
import moe.ahao.commerce.order.infrastructure.publisher.OrderEventPublisher;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderAmountDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderItemDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderPaymentDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderAmountMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderItemMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderPaymentDetailMapper;
import moe.ahao.exception.CommonBizExceptionEnum;
import moe.ahao.util.commons.lang.RandomHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 创建售后信息Action
 */
@Component
@Slf4j
public class AfterSaleCreatedInfoAction extends AfterSaleStateAction<CreateReturnGoodsAfterSaleCommand> {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderPaymentDetailMapper orderPaymentDetailMapper;
    @Autowired
    private OrderAmountMapper orderAmountMapper;
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;
    @Autowired
    private AfterSaleItemMapper afterSaleItemMapper;
    @Autowired
    private AfterSaleItemMybatisService afterSaleItemMybatisService;
    @Autowired
    private AfterSaleRefundMapper afterSaleRefundMapper;
    @Autowired
    private AfterSaleLogDAO afterSaleLogDAO;

    @Autowired
    private GenOrderIdAppService genOrderIdAppService;

    @Resource
    private TransactionTemplate transactionTemplate;
    @Autowired
    private OrderEventPublisher orderEventPublisher;
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CouponGateway couponGateway;

    /**
     * 用于生成优惠券和运费的售后单的退货数量,默认退优惠券和退运费的数量都是1
     */
    private static final BigDecimal AFTER_SALE_RETURN_QUANTITY = BigDecimal.ONE;

    @Override
    public AfterSaleStatusChangeEnum event() {
        return AfterSaleStatusChangeEnum.AFTER_SALE_CREATED;
    }

    /**
     * 业务更新说明：更新售后业务,将尾笔订单条目的验证标准细化到sku数量维度
     * <p>
     * 售后单生成规则说明：<br>
     * 1.当前售后条目非尾笔,只生成一笔订单条目的售后单<br>
     * 2.当前售后条目尾笔,生成3笔售后单,分别是:订单条目售后单、优惠券售后单、运费售后单
     * <p>
     * 业务场景说明：<br>
     * 场景1：订单有2笔条目,A条目商品总数量:10,B条目商品总数量:1<br>
     * 第一次：A发起售后,售后数量1,已退数量1<br>
     * 第二次：A发起售后,售后数量2,已退数量3<br>
     * 第三次：A发起售后,售后数量7,已退数量10,A条目全部退完<br>
     * 第四次：B发起售后,售后数量1,已退数量1,本次售后条目是当前订单的最后一条,补退优惠券和运费<br>
     * <p>
     * 场景2：订单有1笔条目,条目商品总数量和申请售后数量相同,直接全部退掉,补退优惠券和运费
     * <p>
     * 场景3：订单有1笔条目,条目商品总数量2<br>
     * 第一次：条目申请售后,售后数量1<br>
     * 第二次：条目申请售后,售后数量1,本次售后条目是当前订单的最后一条,补退优惠券和运费
     */
    @Override
    protected String onStateChangeInternal(AfterSaleStatusChangeEnum event, CreateReturnGoodsAfterSaleCommand command) {
        //  1、请求参数校验
        this.check1(command);

        // 2. 加分布式锁
        String orderId = command.getOrderId();
        String lockKey = RedisLockKeyConstants.REFUND_KEY + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw OrderExceptionEnum.PROCESS_AFTER_SALE_RETURN_GOODS.msg();
        }
        try {
            // 3. 创建售后单相关信息
            CustomerReceiveAfterSaleEvent customerReceiveAfterSaleEvent = this.doCreate(command);

            // 4. 发送售后单给客服系统审核
            orderEventPublisher.sendAfterSaleRefundMessage(customerReceiveAfterSaleEvent);
            return customerReceiveAfterSaleEvent.getAfterSaleId();
        } finally {
            // 4. 释放分布式锁
            lock.unlock();
        }
    }

    private void check1(CreateReturnGoodsAfterSaleCommand command) {
        if (command == null) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }

        String orderId = command.getOrderId();
        if (StringUtils.isEmpty(orderId)) {
            throw OrderExceptionEnum.ORDER_ID_IS_NULL.msg();
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

        Integer returnGoodsCode = command.getReturnGoodsCode();
        if (returnGoodsCode == null) {
            throw OrderExceptionEnum.RETURN_GOODS_CODE_IS_NULL.msg();
        }

        String skuCode = command.getSkuCode();
        if (StringUtils.isEmpty(skuCode)) {
            throw OrderExceptionEnum.SKU_IS_NULL.msg();
        }

        BigDecimal returnNum = command.getReturnQuantity();
        if (returnNum == null) {
            throw OrderExceptionEnum.RETURN_GOODS_NUM_IS_NULL.msg();
        }
    }

    public CustomerReceiveAfterSaleEvent doCreate(CreateReturnGoodsAfterSaleCommand command) {
        // @Transactional无法生效，需要用编程式事务
        return transactionTemplate.execute(transactionStatus -> {
            // 1. 售后单状态验证
            String orderId = command.getOrderId();
            String skuCode = command.getSkuCode();
            String userId = command.getUserId();
            OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
            this.check2(orderInfo, skuCode);

            // 2. 生成售后单号
            String afterSaleId = genOrderIdAppService.generate(OrderIdTypeEnum.AFTER_SALE, userId);

            // 3. 生成售后条目, 如果是最后一笔, 还要生成优惠券售后条目和运费优惠条目
            List<AfterSaleItemDO> afterSaleItems = this.buildAfterSaleItems(command, afterSaleId, orderInfo);
            afterSaleItemMybatisService.saveBatch(afterSaleItems);

            // 4. 新增售后订单表
            AfterSaleInfoDO afterSaleInfo = this.buildReturnGoodsAfterSaleInfo(orderInfo, AfterSaleTypeEnum.RETURN_GOODS, afterSaleId);
            BigDecimal afterSaleApplyRefundAmount = BigDecimal.ZERO;
            BigDecimal afterSaleRealRefundAmount = BigDecimal.ZERO;
            for (AfterSaleItemDO afterSaleItem : afterSaleItems) {
                afterSaleApplyRefundAmount = afterSaleApplyRefundAmount.add(afterSaleItem.getApplyRefundAmount());
                afterSaleRealRefundAmount = afterSaleRealRefundAmount.add(afterSaleItem.getRealRefundAmount());
            }
            afterSaleInfo.setApplyRefundAmount(afterSaleApplyRefundAmount);
            afterSaleInfo.setRealRefundAmount(afterSaleRealRefundAmount);
            afterSaleInfoMapper.insert(afterSaleInfo);
            log.info("新增订单售后记录,订单号:{},售后单号:{},订单售后状态:{}", orderId, afterSaleId, afterSaleInfo.getAfterSaleStatus());

            // 5. 新增售后变更表
            int fromStatus = this.event().getFromStatus().getCode();
            int toStatus = this.event().getToStatus().getCode();
            String remark = ReturnGoodsTypeEnum.AFTER_SALE_RETURN_GOODS.getName();
            afterSaleLogDAO.save(afterSaleInfo, fromStatus, toStatus, remark);

            // 6. 新增售后支付表
            AfterSaleRefundDO afterSaleRefund = this.buildAfterSaleRefund(afterSaleInfo);
            afterSaleRefundMapper.insert(afterSaleRefund);
            log.info("新增售后支付信息, 订单号:{}, 售后单号:{}, 状态:{}", orderId, afterSaleId, afterSaleRefund.getRefundStatus());

            // 7. 发送售后单给客服系统审核
            CustomerReceiveAfterSaleEvent event = new CustomerReceiveAfterSaleEvent();
            event.setUserId(userId);
            event.setOrderId(orderId);
            event.setAfterSaleId(afterSaleId);
            event.setAfterSaleRefundId(afterSaleRefund.getAfterSaleRefundId());
            event.setAfterSaleType(afterSaleInfo.getAfterSaleType());
            event.setReturnGoodAmount(afterSaleRealRefundAmount);
            event.setApplyRefundAmount(afterSaleApplyRefundAmount);
            return event;
        });
    }

    private void check2(OrderInfoDO orderInfo, String skuCode) {
        if (orderInfo == null) {
            throw OrderExceptionEnum.ORDER_INFO_IS_NULL.msg();
        }
        // 只有已签收订单才能申请售后
        if (!Objects.equals(OrderStatusEnum.SIGNED.getCode(), orderInfo.getOrderStatus())) {
            throw OrderExceptionEnum.AFTER_SALE_ORDER_STATUS_ERROR.msg();
        }
        // 虚拟订单不能售后
        if (OrderTypeEnum.VIRTUAL.getCode().equals(orderInfo.getOrderType())) {
            throw OrderExceptionEnum.VIRTUAL_ORDER_CANNOT_AFTER_SALE.msg();
        }
    }

    /**
     * 尾笔条目判断规则：
     * 用当前条目的"商品总数量"和"已售后数量"作比较
     * 1、商品总数量 < 已售后数量 总共才买1个,售后数量却是2个  售后的数据错误
     * 2、商品总数量 > 已售后数量 条目还没有退完,当前不是最后一笔
     * 3、商品总数量 = 已售后数量 本次售后条目退完,但是如果订单有多条目,需要继续验证是否全部条目均已退完
     */
    private List<AfterSaleItemDO> buildAfterSaleItems(CreateReturnGoodsAfterSaleCommand command, String afterSaleId, OrderInfoDO orderInfo) {
        String orderId = command.getOrderId();
        String skuCode = command.getSkuCode();

        List<OrderItemDO> orderItems = orderItemMapper.selectListByOrderId(orderId);
        List<AfterSaleItemDO> afterSaleItemDOList = afterSaleItemMapper.selectListByOrderIdAndSkuCode(orderId, skuCode);

        // 1. 找到本次要退的订单条目
        OrderItemDO orderItem = orderItems.stream().filter(i -> Objects.equals(i.getSkuCode(), skuCode)).findFirst().orElse(null);
        if (orderItem == null) {
            throw OrderExceptionEnum.ORDER_ITEM_IS_NULL.msg();
        }

        // 2. 判断当前sku的售后情况, 是否已经售后完毕
        // sku售出数量
        BigDecimal orderItemSaleQuantity = orderItem.getSaleQuantity();
        // 已售后数量 = 本次请求的售后数量 + 已经售后过的数量
        BigDecimal alreadyReturnQuantity = afterSaleItemDOList.stream().map(AfterSaleItemDO::getReturnQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentReturnQuantity = command.getReturnQuantity();
        BigDecimal returnQuantity = alreadyReturnQuantity.add(currentReturnQuantity);

        int compare = orderItemSaleQuantity.compareTo(returnQuantity);
        // 2.1. sku售出数量 < 已售后数量, 不允许继续售后, 抛出异常
        if (compare < 0) {
            throw OrderExceptionEnum.AFTER_SALE_QUANTITY_IS_ERROR.msg();
        }
        // 2.2. sku售出数量 = 已售后数量, 说明这个sku是最后一次售后, 后面不允许对这个sku售后了
        else if (compare == 0) {
            // 接下来判断这个订单下还有没有可以进行售后的商品, 如果没有, 就退运费和优惠券
            // 取出数据库中该订单已全部退货完毕的售后条目
            List<AfterSaleItemDO> alreadyAllReturnAfterSaleItems = afterSaleItemMapper.selectListByOrderIdAndReturnCompletionMark(orderId, AfterSaleReturnCompletionMarkEnum.ALL_RETURN_GOODS.getCode());
            int alreadyAllReturnAfterSaleItemCount = alreadyAllReturnAfterSaleItems.size();
            int currentAllReturnAfterSaleItemCount = 1;
            int allReturnAfterSaleItemCount = alreadyAllReturnAfterSaleItemCount + currentAllReturnAfterSaleItemCount;
            // 2.2.1. 已退货完毕的条目数 >= 下单的条目数, 说明本次售后是该订单最后一次售后, 后面不允许对这个订单售后了
            if (allReturnAfterSaleItemCount >= orderItems.size()) {
                List<AfterSaleItemDO> afterSaleItems = new ArrayList<>();
                // 尾笔订单, 共记录3笔售后条目、优惠券售后单(如存在)、运费(如存在)
                AfterSaleItemDO afterSaleItem = this.buildAfterSaleItem(command, afterSaleId, orderItem, afterSaleItemDOList);
                afterSaleItems.add(afterSaleItem);

                // 记录优惠券售后单
                AfterSaleItemDO couponAfterSaleItem = this.buildCouponAfterSaleItem(afterSaleId, orderInfo, orderItem);
                if (couponAfterSaleItem != null) {
                    afterSaleItems.add(couponAfterSaleItem);
                }

                // 如果有运费, 就加一条售后条目, 标记为退运费
                AfterSaleItemDO freightAfterSaleItem = this.buildFreightAfterSaleItem(afterSaleId, orderInfo, orderItem);
                if (freightAfterSaleItem != null) {
                    afterSaleItems.add(freightAfterSaleItem);
                }
                return afterSaleItems;
            }
            // 2.2.2. 已退货完毕的条目数 < 下单的条目数, 说明后续还能对这笔订单的其他sku发起售后
            else {
                AfterSaleItemDO afterSaleItem = this.buildAfterSaleItem(command, afterSaleId, orderItem, afterSaleItemDOList);
                return Collections.singletonList(afterSaleItem);
            }
        }
        // 2.3. sku售出数量 > 已售后数量
        else { // if(compare > 0)
            // 说明后续还能对这笔订单的这个sku发起售后
            AfterSaleItemDO afterSaleItem = this.buildAfterSaleItem(command, afterSaleId, orderItem, afterSaleItemDOList);
            return Collections.singletonList(afterSaleItem);
        }
    }

    private AfterSaleItemDO buildCouponAfterSaleItem(String afterSaleId, OrderInfoDO orderInfo, OrderItemDO orderItem) {
        // 记录优惠券售后单
        String orderId = orderInfo.getOrderId();
        String userId = orderInfo.getUserId();
        String couponId = orderInfo.getCouponId();
        // 没有优惠券,不用退
        if (StringUtils.isEmpty(couponId)) {
            return null;
        }
        GetUserCouponQuery userCouponQuery = new GetUserCouponQuery();
        userCouponQuery.setUserId(userId);
        userCouponQuery.setCouponId(couponId);
        UserCouponDTO userCoupon = couponGateway.get(userCouponQuery);
        // 没使用优惠券,不用退
        if (Objects.equals(CouponUsedStatusEnum.UN_USED.getCode(), userCoupon.getUsed())) {
            return null;
        }

        //  订单使用优惠券了,记一条补退优惠券的售后单
        //  创建优惠券售后单 优惠券id作为售后单商品名称
        AfterSaleItemDO afterSaleItem = new AfterSaleItemDO();
        afterSaleItem.setAfterSaleId(afterSaleId);
        afterSaleItem.setOrderId(orderId);
        afterSaleItem.setSkuCode(orderItem.getSkuCode());
        // 如果是优惠券, 就记录优惠券id
        afterSaleItem.setProductName(couponId);
        // afterSaleItem.setProductImg();
        afterSaleItem.setReturnQuantity(AFTER_SALE_RETURN_QUANTITY);
        // 如果是优惠券, 就不记录金额
        // afterSaleItem.setOriginAmount();
        // afterSaleItem.setApplyRefundAmount();
        // afterSaleItem.setRealRefundAmount();
        afterSaleItem.setReturnCompletionMark(AfterSaleReturnCompletionMarkEnum.ALL_RETURN_GOODS.getCode());
        afterSaleItem.setAfterSaleItemType(AfterSaleItemTypeEnum.AFTER_SALE_COUPON.getCode());
        return afterSaleItem;
    }

    private AfterSaleItemDO buildFreightAfterSaleItem(String afterSaleId, OrderInfoDO orderInfo, OrderItemDO orderItem) {
        String orderId = orderInfo.getOrderId();
        OrderAmountDO deliveryAmount = orderAmountMapper.selectOneByOrderIdAndAmountType(orderId, AmountTypeEnum.SHIPPING_AMOUNT.getCode());
        BigDecimal freightAmount = Optional.ofNullable(deliveryAmount).map(OrderAmountDO::getAmount).orElse(BigDecimal.ZERO);
        if (freightAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        AfterSaleItemDO afterSaleItem = new AfterSaleItemDO();
        afterSaleItem.setAfterSaleId(afterSaleId);
        afterSaleItem.setOrderId(orderId);
        afterSaleItem.setSkuCode(orderItem.getSkuCode());
        // 如果是运费, 就记录"运费"
        afterSaleItem.setProductName(AmountTypeEnum.SHIPPING_AMOUNT.getName());
        // afterSaleItem.setProductImg();
        afterSaleItem.setReturnQuantity(AFTER_SALE_RETURN_QUANTITY);
        // 如果是运费, 就记录金额
        afterSaleItem.setOriginAmount(freightAmount);
        afterSaleItem.setApplyRefundAmount(freightAmount);
        afterSaleItem.setRealRefundAmount(freightAmount);
        afterSaleItem.setReturnCompletionMark(AfterSaleReturnCompletionMarkEnum.ALL_RETURN_GOODS.getCode());
        afterSaleItem.setAfterSaleItemType(AfterSaleItemTypeEnum.AFTER_SALE_FREIGHT.getCode());
        return afterSaleItem;
    }

    private AfterSaleItemDO buildAfterSaleItem(CreateReturnGoodsAfterSaleCommand command, String afterSaleId, OrderItemDO orderItem, List<AfterSaleItemDO> afterSaleItemDOList) {
        // 商品总数量
        BigDecimal orderItemSaleQuantity = orderItem.getSaleQuantity();
        // 已售后数量 = 本次请求的售后数量 + 已经售后过的数量
        BigDecimal alreadyReturnQuantity = afterSaleItemDOList.stream().map(AfterSaleItemDO::getReturnQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentReturnQuantity = command.getReturnQuantity();
        BigDecimal returnQuantity = alreadyReturnQuantity.add(currentReturnQuantity);

        // 计算金额公式：单价 = 总价 / 销量
        // 应付金额的单价
        BigDecimal originalUnitPrice = orderItem.getOriginAmount().divide(orderItem.getSaleQuantity(), 6, RoundingMode.HALF_UP);
        BigDecimal originalAmount = originalUnitPrice.multiply(currentReturnQuantity);
        BigDecimal applyRefundAmount = originalUnitPrice.multiply(currentReturnQuantity);
        // 实付金额的单价
        BigDecimal payUnitPrice = orderItem.getPayAmount().divide(orderItem.getSaleQuantity(), 6, RoundingMode.HALF_UP);
        BigDecimal realRefundAmount = payUnitPrice.multiply(currentReturnQuantity);

        //  填充售后条目数据
        String orderId = orderItem.getOrderId();
        String skuCode = orderItem.getSkuCode();
        AfterSaleItemDO afterSaleItem = new AfterSaleItemDO();
        afterSaleItem.setAfterSaleId(afterSaleId);
        afterSaleItem.setOrderId(orderId);
        afterSaleItem.setSkuCode(skuCode);
        afterSaleItem.setProductName(orderItem.getProductName());
        afterSaleItem.setProductImg(orderItem.getProductImg());
        afterSaleItem.setReturnQuantity(currentReturnQuantity);
        afterSaleItem.setOriginAmount(originalAmount);
        afterSaleItem.setApplyRefundAmount(applyRefundAmount);
        afterSaleItem.setRealRefundAmount(realRefundAmount);
        // 初始默认是10购买的sku未全部退货
        afterSaleItem.setReturnCompletionMark(AfterSaleReturnCompletionMarkEnum.NOT_ALL_RETURN_GOODS.getCode());
        afterSaleItem.setAfterSaleItemType(AfterSaleItemTypeEnum.AFTER_SALE_ORDER_ITEM.getCode());

        // 如果当前sku还没售后过, 并且售后数量等于商品数量, 说明是全部退
        if (afterSaleItemDOList.size() == 0 && orderItemSaleQuantity.equals(currentReturnQuantity)) {
            afterSaleItem.setReturnCompletionMark(AfterSaleReturnCompletionMarkEnum.ALL_RETURN_GOODS.getCode());
        }
        // 如果当前sku售后过, 并且售后数量等于商品数量, 说明是全部退
        // 此条目的商品总数量 == 已经售后过的数 + 本次售后条目数 这条全退
        if (afterSaleItemDOList.size() != 0 && orderItemSaleQuantity.equals(returnQuantity)) {
            // 将当前售后单的其他售后单的当前sku条目更新为ALL_RETURN_GOODS
            afterSaleItemMapper.updateReturnCompletionMark(orderId, skuCode, AfterSaleReturnCompletionMarkEnum.ALL_RETURN_GOODS.getCode());
            afterSaleItem.setReturnCompletionMark(AfterSaleReturnCompletionMarkEnum.ALL_RETURN_GOODS.getCode());
        }
        return afterSaleItem;
    }

    /**
     * 售后退货流程 插入订单销售表
     */
    private AfterSaleInfoDO buildReturnGoodsAfterSaleInfo(OrderInfoDO orderInfo, AfterSaleTypeEnum afterSaleType, String afterSaleId) {
        AfterSaleInfoDO afterSaleInfo = new AfterSaleInfoDO();
        afterSaleInfo.setAfterSaleId(afterSaleId);
        afterSaleInfo.setBusinessIdentifier(orderInfo.getBusinessIdentifier());
        afterSaleInfo.setOrderId(orderInfo.getOrderId());
        afterSaleInfo.setUserId(orderInfo.getUserId());
        afterSaleInfo.setOrderType(OrderTypeEnum.NORMAL.getCode());
        afterSaleInfo.setApplySource(AfterSaleApplySourceEnum.USER_RETURN_GOODS.getCode());
        afterSaleInfo.setApplyTime(new Date());
        afterSaleInfo.setApplyReasonCode(AfterSaleReasonEnum.USER.getCode());
        afterSaleInfo.setApplyReason(AfterSaleReasonEnum.USER.getName());
        // 审核信息要等客服系统审核后更新
        // afterSaleInfo.setReviewTime();
        // afterSaleInfo.setReviewSource();
        // afterSaleInfo.setReviewReasonCode();
        // afterSaleInfo.setReviewReason();
        if (AfterSaleTypeEnum.RETURN_GOODS == afterSaleType) {
            // 退货流程 只退订单的一笔条目
            afterSaleInfo.setAfterSaleType(AfterSaleTypeEnum.RETURN_GOODS.getCode());
        } else if (AfterSaleTypeEnum.RETURN_MONEY == afterSaleType) {
            // 退货流程 退订单的全部条目 后续按照整笔退款逻辑处理
            afterSaleInfo.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());
        }
        afterSaleInfo.setAfterSaleTypeDetail(AfterSaleTypeDetailEnum.PART_REFUND.getCode());
        afterSaleInfo.setAfterSaleStatus(event().getToStatus().getCode());
        // 申请退款金额和实际退款金额后面进行计算
        // afterSaleInfo.setApplyRefundAmount();
        // afterSaleInfo.setRealRefundAmount();
        afterSaleInfo.setRemark(ReturnGoodsTypeEnum.AFTER_SALE_RETURN_GOODS.getName());
        return afterSaleInfo;
    }

    private AfterSaleRefundDO buildAfterSaleRefund(AfterSaleInfoDO afterSaleInfo) {
        String orderId = afterSaleInfo.getOrderId();
        String afterSaleId = afterSaleInfo.getAfterSaleId();

        AfterSaleRefundDO afterSaleRefund = new AfterSaleRefundDO();
        afterSaleRefund.setAfterSaleRefundId(afterSaleId + "_refund");
        afterSaleRefund.setAfterSaleId(afterSaleId);
        afterSaleRefund.setOrderId(orderId);
        afterSaleRefund.setAfterSaleBatchNo(orderId + RandomHelper.getString(10, RandomHelper.DIST_NUMBER));
        afterSaleRefund.setAccountType(AccountTypeEnum.THIRD.getCode());
        afterSaleRefund.setRefundStatus(RefundStatusEnum.UN_REFUND.getCode());
        afterSaleRefund.setRefundAmount(afterSaleInfo.getApplyRefundAmount());
        // 实际退款的时候再记录退款时间
        // afterSaleRefund.setRefundPayTime();
        afterSaleRefund.setRemark(RefundStatusEnum.UN_REFUND.getName());

        OrderPaymentDetailDO paymentDetail = orderPaymentDetailMapper.selectOneByOrderId(orderId);
        if (paymentDetail != null) {
            afterSaleRefund.setOutTradeNo(paymentDetail.getOutTradeNo());
            afterSaleRefund.setPayType(paymentDetail.getPayType());
        }
        return afterSaleRefund;
    }
}
