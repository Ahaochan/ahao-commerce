package moe.ahao.commerce.order.adapter.mq.handler;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.OrderListEsRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data.*;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.*;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单全量数据新增service：
 * <p>
 * 当order_info被创建时（下单），会同时创建
 * 1.order_info
 * 2.order_item
 * 3.order_delivery_detail
 * 4.order_payment_detail
 * 5.order_amount
 * 6.order_amount_detail
 * 7.order_operate_log(保存到mongoDB)
 * 8.order_snapshot(保存到hbase)
 * <p>
 * 于是在监听到order_info的新增binlog日志时，需要将1～6的数据同步到es里面去
 */
@Service
@Slf4j
public class EsOrderSyncHandler {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderDeliveryDetailMapper orderDeliveryDetailMapper;
    @Autowired
    private OrderPaymentDetailMapper orderPaymentDetailMapper;
    @Autowired
    private OrderAmountMapper orderAmountMapper;
    @Autowired
    private OrderAmountDetailMapper orderAmountDetailMapper;

    @Autowired
    private OrderListEsRepository orderListEsRepository;

    /**
     * 将订单全量数据新增至es
     */
    public void syncFullDataByOrderIds(List<String> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return;
        }
        List<OrderInfoDO> orders = orderInfoMapper.selectListByOrderIds(orderIds);
        this.syncFullData(orders);
    }

    /**
     * 将订单全量数据新增至es
     */
    public void syncFullData(List<OrderInfoDO> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }
        List<String> orderIds = orders.stream().map(OrderInfoDO::getOrderId).collect(Collectors.toList());
        // 1. 订单信息
        List<EsOrderInfoDO> esOrders = orders.stream().map(this::convert).collect(Collectors.toList());
        // orderListEsRepository.saveBatch(esOrders, EsOrderInfoDO.class);
        // 2、订单条目
        List<OrderItemDO> orderItems = orderItemMapper.selectListByOrderIds(orderIds);
        List<EsOrderItemDO> esOrderItems = orderItems.stream().map(this::convert).collect(Collectors.toList());
        // orderListEsRepository.saveBatch(esOrderItems, EsOrderItemDO.class);
        // 3. 订单配送信息
        List<OrderDeliveryDetailDO> orderDeliveryDetails = orderDeliveryDetailMapper.selectListByOrderIds(orderIds);
        List<EsOrderDeliveryDetailDO> esOrderDeliveryDetails = orderDeliveryDetails.stream().map(this::convert).collect(Collectors.toList());
        // orderListEsRepository.saveBatch(esOrderDeliveryDetails, EsOrderDeliveryDetailDO.class);
        // 4. 订单支付信息
        List<OrderPaymentDetailDO> orderPaymentDetails = orderPaymentDetailMapper.selectListByOrderIds(orderIds);
        List<EsOrderPaymentDetailDO> esOrderPaymentDetails = orderPaymentDetails.stream().map(this::convert).collect(Collectors.toList());
        // orderListEsRepository.saveBatch(esOrderPaymentDetails, EsOrderPaymentDetailDO.class);
        // 5. 订单条目价格明细
        List<OrderAmountDetailDO> orderAmountDetails = orderAmountDetailMapper.selectListByOrderIds(orderIds);
        List<EsOrderAmountDetailDO> esOrderAmountDetails = orderAmountDetails.stream().map(this::convert).collect(Collectors.toList());
        // orderListEsRepository.saveBatch(esOrderAmountDetails, EsOrderAmountDetailDO.class);
        // 6. 订单价格
        List<OrderAmountDO> orderAmounts = orderAmountMapper.selectListByOrderIds(orderIds);
        List<EsOrderAmountDO> esOrderAmounts = orderAmounts.stream().map(this::convert).collect(Collectors.toList());
        // orderListEsRepository.saveBatch(esOrderAmounts, EsOrderAmountDO.class);

        // 7. 构建orderListQueryIndex并同步到es
        //    只将要搜索的字段放到ES里, 不应该将全量字段放进ES, 再用查出来的ID去分库分表里查询
        Map<String, OrderDeliveryDetailDO> orderDeliveryDetailMap = orderDeliveryDetails.stream().collect(Collectors.toMap(OrderDeliveryDetailDO::getOrderId, d -> d));
        Map<String, List<OrderItemDO>> orderItemsMap = orderItems.stream().collect(Collectors.groupingBy(OrderItemDO::getOrderId));
        Map<String, List<OrderPaymentDetailDO>> orderPaymentDetailsMap = orderPaymentDetails.stream().collect(Collectors.groupingBy(OrderPaymentDetailDO::getOrderId));
        orderListEsRepository.saveBatchByOrderInfos(orders, orderDeliveryDetailMap, orderItemsMap, orderPaymentDetailsMap, -1);
    }

    /**
     * 将订单同步至es
     */
    public void syncOrderInfos(List<String> orderIds, long timestamp) {
        // 1. 查询订单
        if (CollectionUtils.isEmpty(orderIds)) {
            return;
        }
        List<OrderInfoDO> orders = orderInfoMapper.selectListByOrderIds(orderIds);
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }

        // 2. 订单信息
        List<EsOrderInfoDO> esOrders = orders.stream().map(this::convert).collect(Collectors.toList());
        orderListEsRepository.saveBatch(esOrders, EsOrderInfoDO.class);

        // 3. 构建orderListQueryIndex并同步到es
        orderListEsRepository.saveBatchByOrderInfos(orders, timestamp);
    }

    /**
     * 将订单配送信息同步至es
     */
    public void syncOrderDeliveryDetails(List<String> orderIds, long timestamp) {
        // 1. 查询订单配送信息
        if (CollectionUtils.isEmpty(orderIds)) {
            return;
        }
        List<OrderDeliveryDetailDO> orderDeliveryDetails = orderDeliveryDetailMapper.selectListByOrderIds(orderIds);
        if (CollectionUtils.isEmpty(orderDeliveryDetails)) {
            return;
        }

        // 2. 订单配送表
        List<EsOrderDeliveryDetailDO> esOrderDeliveryDetails = orderDeliveryDetails.stream().map(this::convert).collect(Collectors.toList());
        orderListEsRepository.saveBatch(esOrderDeliveryDetails, EsOrderDeliveryDetailDO.class);

        // 3. 构建orderListQueryIndex并同步到es
        orderListEsRepository.saveBatchByOrderIds(orderIds, timestamp);
    }

    /**
     * 将订单支付明细同步至es
     * <p>
     * 因为OrderPaymentDetailDO未指定docId，是es自动生成的，
     * 所以要实现更新，需要采用先删除，后新增的方式
     */
    public void syncOrderPaymentDetails(List<String> orderIds, long timestamp) {
        // 1、查询订单支付明细
        if (CollectionUtils.isEmpty(orderIds)) {
            return;
        }
        List<OrderPaymentDetailDO> orderPaymentDetails = orderPaymentDetailMapper.selectListByOrderIds(orderIds);
        if (CollectionUtils.isEmpty(orderPaymentDetails)) {
            return;
        }

        // 2、将订单支付明细同步到es里面去
        List<EsOrderPaymentDetailDO> esOrderPaymentDetails = orderPaymentDetails.stream().map(this::convert).collect(Collectors.toList());
        orderListEsRepository.saveBatch(esOrderPaymentDetails, EsOrderPaymentDetailDO.class);

        // 3. 构建orderListQueryIndex并同步到es
        orderListEsRepository.saveBatchByOrderIds(orderIds, timestamp);
    }

    private EsOrderInfoDO convert(OrderInfoDO that) {
        EsOrderInfoDO _this = new EsOrderInfoDO();
        // 以订单id作为es的主键id
        _this.setEsId(that.getOrderId());
        _this.setId(that.getId());
        _this.setCreateTime(that.getCreateTime());
        _this.setUpdateTime(that.getUpdateTime());
        _this.setBusinessIdentifier(that.getBusinessIdentifier());
        _this.setOrderId(that.getOrderId());
        _this.setParentOrderId(that.getParentOrderId());
        _this.setBusinessOrderId(that.getBusinessOrderId());
        _this.setOrderType(that.getOrderType());
        _this.setOrderStatus(that.getOrderStatus());
        _this.setCancelType(that.getCancelType());
        _this.setCancelTime(that.getCancelTime());
        _this.setSellerId(that.getSellerId());
        _this.setUserId(that.getUserId());
        _this.setTotalAmount(that.getTotalAmount());
        _this.setPayAmount(that.getPayAmount());
        _this.setPayType(that.getPayType());
        _this.setCouponId(that.getCouponId());
        _this.setPayTime(that.getPayTime());
        _this.setExpireTime(that.getExpireTime());
        _this.setUserRemark(that.getUserRemark());
        _this.setDeleteStatus(that.getDeleteStatus());
        _this.setCommentStatus(that.getCommentStatus());
        _this.setExtJson(that.getExtJson());
        return _this;
    }

    private EsOrderItemDO convert(OrderItemDO that) {
        EsOrderItemDO _this = new EsOrderItemDO();
        // 以订单条目id作为es的主键id
        _this.setEsId(that.getOrderItemId());
        _this.setId(that.getId());
        _this.setCreateTime(that.getCreateTime());
        _this.setUpdateTime(that.getUpdateTime());
        _this.setOrderId(that.getOrderId());
        _this.setOrderItemId(that.getOrderItemId());
        _this.setProductType(that.getProductType());
        _this.setProductId(that.getProductId());
        _this.setProductImg(that.getProductImg());
        _this.setProductName(that.getProductName());
        _this.setSkuCode(that.getSkuCode());
        _this.setSaleQuantity(that.getSaleQuantity());
        _this.setSalePrice(that.getSalePrice());
        _this.setOriginAmount(that.getOriginAmount());
        _this.setPayAmount(that.getPayAmount());
        _this.setProductUnit(that.getProductUnit());
        _this.setPurchasePrice(that.getPurchasePrice());
        _this.setSellerId(that.getSellerId());
        _this.setExtJson(that.getExtJson());
        return _this;
    }

    public EsOrderDeliveryDetailDO convert(OrderDeliveryDetailDO that) {
        EsOrderDeliveryDetailDO _this = new EsOrderDeliveryDetailDO();
        // 以订单id作为es的主键id
        _this.setEsId(that.getOrderId());
        _this.setId(that.getId());
        _this.setCreateTime(that.getCreateTime());
        _this.setUpdateTime(that.getUpdateTime());
        _this.setOrderId(that.getOrderId());
        _this.setDeliveryType(that.getDeliveryType());
        _this.setProvince(that.getProvince());
        _this.setCity(that.getCity());
        _this.setArea(that.getArea());
        _this.setStreet(that.getStreet());
        _this.setDetailAddress(that.getDetailAddress());
        _this.setLon(that.getLon());
        _this.setLat(that.getLat());
        _this.setReceiverName(that.getReceiverName());
        _this.setReceiverPhone(that.getReceiverPhone());
        _this.setModifyAddressCount(that.getModifyAddressCount());
        _this.setDelivererNo(that.getDelivererNo());
        _this.setDelivererName(that.getDelivererName());
        _this.setDelivererPhone(that.getDelivererPhone());
        _this.setOutStockTime(that.getOutStockTime());
        _this.setSignedTime(that.getSignedTime());
        return _this;
    }

    public EsOrderPaymentDetailDO convert(OrderPaymentDetailDO that) {
        EsOrderPaymentDetailDO _this = new EsOrderPaymentDetailDO();
        // 以订单id和支付方式作为es的主键id
        _this.setEsId(that.getOrderId() + "_" + that.getPayType());
        _this.setId(that.getId());
        _this.setCreateTime(that.getCreateTime());
        _this.setUpdateTime(that.getUpdateTime());
        _this.setOrderId(that.getOrderId());
        _this.setAccountType(that.getAccountType());
        _this.setPayType(that.getPayType());
        _this.setPayStatus(that.getPayStatus());
        _this.setPayAmount(that.getPayAmount());
        _this.setPayTime(that.getPayTime());
        _this.setOutTradeNo(that.getOutTradeNo());
        _this.setPayRemark(that.getPayRemark());
        return _this;
    }

    public EsOrderAmountDetailDO convert(OrderAmountDetailDO that) {
        EsOrderAmountDetailDO _this = new EsOrderAmountDetailDO();
        // 以订单id和skuCode和金额类型作为es的主键id
        _this.setEsId(that.getOrderId() + "_" + that.getSkuCode() + "_" + that.getAmountType());
        _this.setId(that.getId());
        _this.setCreateTime(that.getCreateTime());
        _this.setUpdateTime(that.getUpdateTime());
        _this.setOrderId(that.getOrderId());
        _this.setProductType(that.getProductType());
        _this.setOrderItemId(that.getOrderItemId());
        _this.setProductId(that.getProductId());
        _this.setSkuCode(that.getSkuCode());
        _this.setSaleQuantity(that.getSaleQuantity());
        _this.setSalePrice(that.getSalePrice());
        _this.setAmountType(that.getAmountType());
        _this.setAmount(that.getAmount());
        return _this;
    }

    public EsOrderAmountDO convert(OrderAmountDO that) {
        EsOrderAmountDO _this = new EsOrderAmountDO();
        // 以订单id和金额类型作为es的主键id
        _this.setEsId(that.getOrderId() + "_" + that.getAmountType());
        _this.setId(that.getId());
        _this.setCreateTime(that.getCreateTime());
        _this.setUpdateTime(that.getUpdateTime());
        _this.setOrderId(that.getOrderId());
        _this.setAmountType(that.getAmountType());
        _this.setAmount(that.getAmount());
        return _this;
    }
}
