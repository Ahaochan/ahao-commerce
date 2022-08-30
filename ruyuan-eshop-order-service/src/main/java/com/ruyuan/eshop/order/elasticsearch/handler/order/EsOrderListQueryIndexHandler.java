package com.ruyuan.eshop.order.elasticsearch.handler.order;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.common.collect.Lists;
import com.ruyuan.eshop.order.converter.EsEntityConverter;
import com.ruyuan.eshop.order.dao.OrderDeliveryDetailDAO;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.dao.OrderItemDAO;
import com.ruyuan.eshop.order.dao.OrderPaymentDetailDAO;
import com.ruyuan.eshop.order.domain.entity.OrderDeliveryDetailDO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.entity.OrderItemDO;
import com.ruyuan.eshop.order.domain.entity.OrderPaymentDetailDO;
import com.ruyuan.eshop.order.elasticsearch.EsClientService;
import com.ruyuan.eshop.order.elasticsearch.handler.EsAbstractHandler;
import com.ruyuan.eshop.order.elasticsearch.query.OrderListQueryIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 构建订单列表查询es index
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class EsOrderListQueryIndexHandler extends EsAbstractHandler {

    @Autowired
    private EsEntityConverter esEntityConverter;

    @Autowired
    private EsClientService esClientService;

    @Autowired
    @Qualifier("orderThreadPool")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    /**
     * 构建orderListQueryIndex
     */
    public List<OrderListQueryIndex> buildOrderListQueryIndex(List<OrderInfoDO> orders, List<OrderDeliveryDetailDO> orderDeliveryDetails
            , List<OrderItemDO> orderItems, List<OrderPaymentDetailDO> orderPaymentDetails) {
        Map<String, OrderDeliveryDetailDO> orderDeliveryDetailMap = orderDeliveryDetails.stream()
                .collect(Collectors.toMap(OrderDeliveryDetailDO::getOrderId, d -> d));
        Map<String, List<OrderItemDO>> orderItemsMap = orderItems.stream().collect(Collectors.groupingBy(OrderItemDO::getOrderId));
        Map<String, List<OrderPaymentDetailDO>> orderPaymentDetailsMap = orderPaymentDetails.stream().collect(Collectors.groupingBy(OrderPaymentDetailDO::getOrderId));

        List<OrderListQueryIndex> result = new ArrayList<>();

        for (OrderInfoDO order : orders) {
            OrderDeliveryDetailDO deliveryDetail = orderDeliveryDetailMap.get(order.getOrderId());
            List<OrderItemDO> orderItemList = orderItemsMap.get(order.getOrderId());
            List<OrderPaymentDetailDO> orderPaymentDetailList = orderPaymentDetailsMap.get(order.getOrderId());
            // 构建
            result.addAll(build(order, deliveryDetail, orderItemList, orderPaymentDetailList));
        }

        // 设置esId
        setEsIdOfOrderListQueryIndex(result);

        return result;
    }

    /**
     * 同步到es
     */
    public void sycToEs(List<OrderListQueryIndex> orderListQueryIndices) throws Exception {
        sycToEs(orderListQueryIndices, -1);
    }

    /**
     * 同步到es
     */
    public void sycToEs(List<OrderListQueryIndex> orderListQueryIndices, long timestamp) throws Exception {
        log.info("同步OrderListQueryIndex到es , orderListQueryIndices={},timestamp={}", JSONObject.toJSONString(orderListQueryIndices), timestamp);
        if (timestamp <= -1) {
            esClientService.bulkIndex(orderListQueryIndices);
        } else {
            esClientService.bulkIndexWithVersionControl(orderListQueryIndices, timestamp);
        }
    }


    /**
     * 异步构建OrderListQueryListIndex并同步到es
     */
    public void asyncBuildAndSynToEs(List<OrderInfoDO> orders, List<String> orderIds, long timestamp) {
        taskExecutor.execute(() -> {
            try {
                buildAndSynToEs(orders, orderIds, timestamp);
            } catch (Exception e) {
                log.error("同步OrderListQueryIndex到es异常，err={}", e.getMessage(), e);
            }
        });
    }

    /**
     * 异步构建OrderListQueryListIndex并同步到es
     */
    public void asyncBuildAndSynToEs(List<String> orderIds, long timestamp) {
        taskExecutor.execute(() -> {
            try {
                // 查询订单条目
                List<OrderInfoDO> orders = orderInfoDAO.listByOrderIds(orderIds);
                if (CollectionUtils.isEmpty(orders)) {
                    return;
                }

                buildAndSynToEs(orders, orderIds, timestamp);
            } catch (Exception e) {
                log.error("同步OrderListQueryIndex到es异常，err={}", e.getMessage(), e);
            }
        });
    }

    /**
     * 构建宽表信息并保存到ES
     */
    private void buildAndSynToEs(List<OrderInfoDO> orders, List<String> orderIds, long timestamp) throws Exception {
        // 1、查询订单条目
        List<OrderItemDO> orderItems = orderItemDAO.listByOrderIds(orderIds);

        // 2、查询订单配送信息
        List<OrderDeliveryDetailDO> orderDeliveryDetails = orderDeliveryDetailDAO.listByOrderIds(orderIds);

        // 3、查询订单支付信息
        List<OrderPaymentDetailDO> orderPaymentDetails = orderPaymentDetailDAO.listByOrderIds(orderIds);

        List<OrderListQueryIndex> orderListQueryIndices = buildOrderListQueryIndex(orders, orderDeliveryDetails
                , orderItems, orderPaymentDetails);

        sycToEs(orderListQueryIndices, timestamp);
    }

    /**
     * 构造订单列表查询es index
     */
    private List<OrderListQueryIndex> build(OrderInfoDO order, OrderDeliveryDetailDO deliveryDetail
            , List<OrderItemDO> orderItems, List<OrderPaymentDetailDO> orderPaymentDetails) {

        // 1、先将order_info和order_delivery_detail进行内连接
        OrderListQueryIndex queryIndex = orderInfoInnerJoinOrderDeliveryDetail(order, deliveryDetail);

        // 2、再内连接order_item
        List<OrderListQueryIndex> result = andInnerJoinWithOrderItem(queryIndex, orderItems);

        // 3、最后内连接order_payment_detail
        result = andInnerJoinWithOrderPaymentDetail(result, orderPaymentDetails);

        return result;
    }


    /**
     * 将order_info和order_delivery_detail进行内连接，order_info和order_delivery_detail是1:1
     */
    private OrderListQueryIndex orderInfoInnerJoinOrderDeliveryDetail(OrderInfoDO order, OrderDeliveryDetailDO deliveryDetail) {
        OrderListQueryIndex.OrderListQueryIndexBuilder builder = OrderListQueryIndex.builder()
                .orderId(order.getOrderId())
                .parentOrderId(order.getParentOrderId())
                .businessIdentifier(order.getBusinessIdentifier())
                .createdTime(order.getGmtCreate())
                .orderStatus(order.getOrderStatus())
                .orderType(order.getOrderType())
                .sellerId(order.getSellerId())
                .payAmount(order.getPayAmount())
                .userId(order.getUserId());
        if (null != deliveryDetail) {
            builder.receiverName(deliveryDetail.getReceiverName())
                    .receiverPhone(deliveryDetail.getReceiverPhone());
        }

        return builder.build();
    }

    /**
     * 再和order_item进行内连接，order_info*order_delivery_detail和order_item是1:N
     */
    private List<OrderListQueryIndex> andInnerJoinWithOrderItem(OrderListQueryIndex originQueryIndex, List<OrderItemDO> orderItems) {
        if (CollectionUtils.isEmpty(orderItems)) {
            return Lists.newArrayList(originQueryIndex);
        }

        List<OrderListQueryIndex> result = new ArrayList<>();
        for (OrderItemDO item : orderItems) {
            OrderListQueryIndex newQueryIndex = esEntityConverter.copyOrderListQueryIndex(originQueryIndex);

            newQueryIndex.setOrderItemId(item.getOrderItemId());
            newQueryIndex.setSkuCode(item.getSkuCode());
            newQueryIndex.setProductName(item.getProductName());
            newQueryIndex.setProductType(item.getProductType());

            result.add(newQueryIndex);
        }
        return result;
    }

    /**
     * 再和order_payment_detail进行内连接
     * <p>
     * order_info和order_payment_detail是1:N
     * 于是order_info*order_delivery_detail*order_item和order_payment_detail是N:N
     */
    private List<OrderListQueryIndex> andInnerJoinWithOrderPaymentDetail(List<OrderListQueryIndex> queryIndices, List<OrderPaymentDetailDO> orderPaymentDetails) {

        if (CollectionUtils.isEmpty(orderPaymentDetails)) {
            return queryIndices;
        }

        // 假设order_info*order_delivery_detail*order_item生成了3条数据
        // orderPaymentDetails有2条数据
        // 那么两者进行内连接，会生成2*3=6条数据，相当于做笛卡尔积
        List<OrderListQueryIndex> result = new ArrayList<>();
        for (OrderPaymentDetailDO paymentDetail : orderPaymentDetails) {
            for (OrderListQueryIndex originQueryIndex : queryIndices) {
                OrderListQueryIndex newQueryIndex = esEntityConverter.copyOrderListQueryIndex(originQueryIndex);
                newQueryIndex.setPayTime(paymentDetail.getPayTime());
                newQueryIndex.setTradeNo(paymentDetail.getOutTradeNo());
                newQueryIndex.setPayType(paymentDetail.getPayType());
                result.add(newQueryIndex);
            }
        }
        return result;
    }

}
