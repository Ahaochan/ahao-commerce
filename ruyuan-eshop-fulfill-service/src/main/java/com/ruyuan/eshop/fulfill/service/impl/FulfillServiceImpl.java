package com.ruyuan.eshop.fulfill.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.common.constants.RedisLockKeyConstants;
import com.ruyuan.eshop.common.enums.OrderTypeEnum;
import com.ruyuan.eshop.common.redis.RedisLock;
import com.ruyuan.eshop.common.utils.RandomUtil;
import com.ruyuan.eshop.fulfill.builder.FulfillData;
import com.ruyuan.eshop.fulfill.converter.FulFillConverter;
import com.ruyuan.eshop.fulfill.dao.OrderFulfillDAO;
import com.ruyuan.eshop.fulfill.dao.OrderFulfillItemDAO;
import com.ruyuan.eshop.fulfill.dao.OrderFulfillLogDAO;
import com.ruyuan.eshop.fulfill.domain.entity.OrderFulfillDO;
import com.ruyuan.eshop.fulfill.domain.entity.OrderFulfillItemDO;
import com.ruyuan.eshop.fulfill.domain.request.ReceiveFulfillRequest;
import com.ruyuan.eshop.fulfill.domain.request.ReceiveOrderItemRequest;
import com.ruyuan.eshop.fulfill.enums.OrderFulfillOperateTypeEnum;
import com.ruyuan.eshop.fulfill.enums.OrderFulfillStatusEnum;
import com.ruyuan.eshop.fulfill.enums.OrderFulfillTypeEnum;
import com.ruyuan.eshop.fulfill.exception.FulfillBizException;
import com.ruyuan.eshop.fulfill.exception.FulfillErrorCodeEnum;
import com.ruyuan.eshop.fulfill.service.FulfillService;
import com.ruyuan.eshop.fulfill.utils.OrderFulfillTypeUtils;
import com.ruyuan.eshop.product.enums.ProductTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class FulfillServiceImpl implements FulfillService {

    @Autowired
    private OrderFulfillDAO orderFulfillDAO;

    @Autowired
    private OrderFulfillItemDAO orderFulfillItemDAO;

    @Autowired
    private OrderFulfillLogDAO orderFulfillLogDAO;

    @Autowired
    private RedisLock redisLock;

    @Autowired
    private FulFillConverter fulFillConverter;

    @Autowired
    private OrderFulfillOperateLogFactory orderFulfillOperateLogFactory;

    @Autowired
    private OrderFulfillScheduleService orderFulfillScheduleService;

    @Override
    public FulfillData createFulfillOrders(ReceiveFulfillRequest request) {
        Integer orderType = request.getOrderType();
        List<ReceiveOrderItemRequest> receiveOrderItems = request.getReceiveOrderItems();

        // 1、创建履约单
        FulfillData fulfillData = new FulfillData();
        if (shouldSplit(orderType, receiveOrderItems)) {
            // 进行履约单拆分
            doCreateSplitFulfillOrder(request, receiveOrderItems, fulfillData);
        } else {
            // 直接创建履约单
            doCreateFulfillOrder(request, receiveOrderItems, fulfillData);
        }

        // 2、设置履约单状态为"已创建"
        fulfillData.getOrderFulFills().forEach(orderFulfill ->
                orderFulfill.setStatus(OrderFulfillStatusEnum.FULFILL.getCode()));

        // 3、保存履约单、履约条目、履约单状态变更
        orderFulfillDAO.saveBatch(fulfillData.getOrderFulFills());
        orderFulfillItemDAO.saveBatch(fulfillData.getOrderFulFillItems());
        orderFulfillLogDAO.saveBatch(fulfillData.getOrderFulfillLogs());

        // 4、返回创建好的履约单
        return fulfillData;
    }

    /**
     * 进行履约单拆分
     */
    private void doCreateSplitFulfillOrder(ReceiveFulfillRequest request, List<ReceiveOrderItemRequest> receiveOrderItems, FulfillData fulfillData) {
        Integer remainDeliveryAmount = request.getDeliveryAmount();
        Integer totalDeliveryAmount = request.getDeliveryAmount();
        Integer totalPayAmount = request.getPayAmount();
        int size = receiveOrderItems.size();
        boolean isLast;
        for (int i = 0; i < size; i++) {
            isLast = (i == size - 1);
            ReceiveOrderItemRequest receiveOrderItem = receiveOrderItems.get(i);
            double deliveryAmount = 0.0;
            if (!isLast) {
                // 计算拆分履约单后的运费
                deliveryAmount = Math.ceil(receiveOrderItem.getPayAmount() / totalPayAmount.doubleValue() * totalDeliveryAmount);
                remainDeliveryAmount -= (int) deliveryAmount;
            }
            // 通过ReceiveOrderItemRequest构建履约单
            OrderFulfillDO orderFulfillDO = buildOrderFulfill(receiveOrderItem, request, !isLast ? (int) deliveryAmount : remainDeliveryAmount);
            //设置扩展字段：目前仅限预售单
            orderFulfillDO.setExtJson(receiveOrderItem.getExtJson());
            OrderFulfillItemDO orderFulfillItemDO = fulFillConverter.convertFulFillRequest(receiveOrderItem);
            orderFulfillItemDO.setFulfillId(orderFulfillDO.getFulfillId());
            fulfillData.addOrderFulfill(orderFulfillDO);
            fulfillData.addOrderFulFillItem(orderFulfillItemDO);
            fulfillData.addOrderFulFillLog(orderFulfillOperateLogFactory.get(orderFulfillDO, OrderFulfillOperateTypeEnum.NEW_ORDER));
        }
    }

    /**
     * 创建履约单
     */
    private void doCreateFulfillOrder(ReceiveFulfillRequest request, List<ReceiveOrderItemRequest> receiveOrderItems, FulfillData fulfillData) {
        // 生成履约单ID
        String fulfillId = genFulfillId();

        List<OrderFulfillItemDO> orderFulfillItemDOS = fulFillConverter.convertFulFillRequest(receiveOrderItems);
        // 设置履约单ID
        for (OrderFulfillItemDO item : orderFulfillItemDOS) {
            item.setFulfillId(fulfillId);
        }

        OrderFulfillDO orderFulFill = fulFillConverter.convertFulFillRequest(request);
        orderFulFill.setFulfillId(fulfillId);
        OrderFulfillTypeUtils.setOrderFulfillType(request.getOrderType(), orderFulFill);

        fulfillData.addOrderFulfill(orderFulFill);
        fulfillData.addOrderFulFillItems(orderFulfillItemDOS);
        fulfillData.addOrderFulFillLog(orderFulfillOperateLogFactory.get(orderFulFill, OrderFulfillOperateTypeEnum.NEW_ORDER));

        // 设置预售履约单预售商品信息
        ProductTypeEnum productType = ProductTypeEnum.PRE_SALE;
        ReceiveOrderItemRequest preSaleOrderItem = receiveOrderItems.stream()
                .filter(item -> productType.getCode().equals(item.getProductType())).findFirst()
                .orElse(null);
        if (null != preSaleOrderItem) {
            orderFulFill.setExtJson(preSaleOrderItem.getExtJson());
        }
    }


    /**
     * 通过ReceiveOrderItemRequest构建履约单
     */
    private OrderFulfillDO buildOrderFulfill(ReceiveOrderItemRequest receiveOrderItem, ReceiveFulfillRequest receiveFulfill, Integer deliveryAmount) {
        Integer orderType = receiveFulfill.getOrderType();
        // 1、生成履约单ID
        String fulfillId = genFulfillId();

        // 2、构造履约单
        OrderFulfillDO orderFulfill = fulFillConverter.convertFulFillRequest(receiveFulfill);
        orderFulfill.setFulfillId(fulfillId);
        OrderFulfillTypeUtils.setOrderFulfillType(orderType, orderFulfill);
        orderFulfill.setPayAmount(receiveOrderItem.getPayAmount());
        orderFulfill.setTotalAmount(receiveOrderItem.getOriginAmount());
        orderFulfill.setDeliveryAmount(deliveryAmount);

        return orderFulfill;
    }

    /**
     * 判断是否需要拆单
     */
    private boolean shouldSplit(Integer orderType, List<ReceiveOrderItemRequest> receiveOrderItems) {
        return OrderTypeEnum.PRE_SALE.getCode().equals(orderType) && receiveOrderItems.size() > 1;
    }

    @Override
    public void cancelFulfillOrder(String orderId) {
        //1、查询履约单
        OrderFulfillDO orderFulfill = orderFulfillDAO.getOne(orderId);

        //2、移除履约单
        if (orderFulfill != null) {
            orderFulfillDAO.removeById(orderFulfill.getId());
            //3、查询履约单条目
            List<OrderFulfillItemDO> fulfillItems = orderFulfillItemDAO
                    .listByFulfillId(orderFulfill.getFulfillId());

            //4、移除履约单条目
            if (CollectionUtils.isNotEmpty(fulfillItems)) {
                List<Long> ids = fulfillItems.stream().map(OrderFulfillItemDO::getId).collect(Collectors.toList());
                orderFulfillDAO.removeByIds(ids);
            }
        }
    }

    /**
     * 生成履约单id
     */
    private String genFulfillId() {
        return RandomUtil.genRandomNumber(10);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean receiveOrderFulFill(ReceiveFulfillRequest request) {
        log.info("接受订单履约成功，request={}", JSONObject.toJSONString(request));

        String orderId = request.getOrderId();

        // 加分布式锁（防止重复触发履约）
        String key = RedisLockKeyConstants.FULFILL_KEY + orderId;
        boolean lock = redisLock.tryLock(key);
        if (!lock) {
            log.error("履约锁加锁失败，lock={},orderId={}", lock,orderId);
            return true;
            //throw new FulfillBizException(FulfillErrorCodeEnum.ORDER_FULFILL_ERROR);
        }

        try {
            //  1、幂等：校验orderId是否已经履约过
            if (orderFulfilled(request.getOrderId())) {
                log.info("该订单已履约！！！,orderId={}", request.getOrderId());
                return true;
            }

            // 2、创建履约单
            FulfillData fulfillData = createFulfillOrders(request);

            // 3、进行履约调度
            List<OrderFulfillDO> orderFulfills = fulfillData.getOrderFulFills();
            Map<String, List<OrderFulfillItemDO>> orderFulfillItemMap = fulfillData.getOrderFulFillItems()
                    .stream()
                    .collect(Collectors.groupingBy(OrderFulfillItemDO::getFulfillId));
            orderFulfills.stream()
                    .filter(e -> OrderFulfillTypeEnum.NORMAL.equals(OrderFulfillTypeEnum.getByCode(e.getOrderFulfillType())))
                    .forEach(orderFulfill -> {
                        List<OrderFulfillItemDO> orderFulfillItems = orderFulfillItemMap.get(orderFulfill.getFulfillId());
                        orderFulfillScheduleService.doSchedule(orderFulfill, orderFulfillItems);
                    });
            return true;
        } finally {
            if(lock) {
                redisLock.unlock(key);
            }
        }
    }

    /**
     * 校验订单是否履约过
     */
    private boolean orderFulfilled(String orderId) {
        List<OrderFulfillDO> orderFulfills = orderFulfillDAO.listByOrderId(orderId);
        return CollectionUtils.isNotEmpty(orderFulfills);
    }
}
