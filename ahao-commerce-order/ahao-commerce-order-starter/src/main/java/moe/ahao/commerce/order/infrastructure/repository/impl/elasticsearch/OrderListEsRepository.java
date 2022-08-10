package moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch;

import moe.ahao.commerce.order.api.enums.OrderQuerySortField;
import moe.ahao.commerce.order.api.query.OrderPageQuery;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data.EsOrderListQueryDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderDeliveryDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderItemDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderPaymentDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderDeliveryDetailMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderItemMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderPaymentDetailMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class OrderListEsRepository extends ElasticsearchRepository {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderDeliveryDetailMapper orderDeliveryDetailMapper;
    @Autowired
    private OrderPaymentDetailMapper orderPaymentDetailMapper;

    public OrderListEsRepository(ElasticsearchRestTemplate elasticsearchRestTemplate) {
        super(elasticsearchRestTemplate);
    }

    public SearchHits<EsOrderListQueryDO> query(OrderPageQuery query) {
        // 1. 构造and等值查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (query.getBusinessIdentifier() != null) {
            boolQueryBuilder.must(QueryBuilders.termQuery(EsOrderListQueryDO.BUSINESS_IDENTIFIER, query.getBusinessIdentifier()));
        }
        if (CollectionUtils.isNotEmpty(query.getOrderIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.ORDER_ID, query.getOrderIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getOrderTypes())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.ORDER_TYPE, query.getOrderTypes()));
        }
        if (CollectionUtils.isNotEmpty(query.getSellerIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.SELLER_ID, query.getSellerIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getParentOrderIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.PARENT_ORDER_ID, query.getParentOrderIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getUserIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.USER_ID, query.getUserIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getOrderStatus())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.ORDER_STATUS, query.getOrderStatus()));
        }
        if (CollectionUtils.isNotEmpty(query.getReceiverPhones())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.RECEIVER_PHONE, query.getReceiverPhones()));
        }
        if (CollectionUtils.isNotEmpty(query.getReceiverNames())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.RECEIVER_NAME, query.getReceiverNames()));
        }
        if (CollectionUtils.isNotEmpty(query.getTradeNos())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.TRADE_NO, query.getTradeNos()));
        }
        if (CollectionUtils.isNotEmpty(query.getSkuCodes())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.SKU_CODE, query.getSkuCodes()));
        }
        if (CollectionUtils.isNotEmpty(query.getProductNames())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.PRODUCT_NAME, query.getProductNames()));
        }
        if (CollectionUtils.isNotEmpty(query.getProductNames())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.PRODUCT_NAME, query.getProductNames()));
        }
        super.range(boolQueryBuilder, EsOrderListQueryDO.CREATED_TIME, query.getQueryStartCreatedTime(), query.getQueryEndCreatedTime());
        super.range(boolQueryBuilder, EsOrderListQueryDO.PAY_TIME, query.getQueryStartPayTime(), query.getQueryEndPayTime());
        super.range(boolQueryBuilder, EsOrderListQueryDO.PAY_AMOUNT, query.getQueryStartPayAmount(), query.getQueryEndPayAmount());
        // 2. 设置排序
        String sortField = query.getSort().equals(OrderQuerySortField.PAY_TIME_DESC) ? EsOrderListQueryDO.PAY_TIME : EsOrderListQueryDO.CREATED_TIME;

        // 3. 查询es, 获取返回hits
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(boolQueryBuilder)
            .withPageable(PageRequest.of(query.getPageNo(), query.getPageSize()))
            .withSort(new FieldSortBuilder(sortField).order(SortOrder.DESC))
            .build();
        SearchHits<EsOrderListQueryDO> hits = elasticsearchRestTemplate.search(nativeSearchQuery, EsOrderListQueryDO.class);
        return hits;
    }

    public EsOrderListQueryDO getOneByOrderId(String orderId) {
        if(StringUtils.isEmpty(orderId)) {
            return null;
        }
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termQuery(EsOrderListQueryDO.ORDER_ID, orderId));
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(boolQueryBuilder)
            .build();
        SearchHits<EsOrderListQueryDO> hits = elasticsearchRestTemplate.search(nativeSearchQuery, EsOrderListQueryDO.class);
        EsOrderListQueryDO index = hits.stream().map(SearchHit::getContent).findFirst().orElse(null);
        return index;
    }

    public List<EsOrderListQueryDO> getListByOrderIds(List<String> orderIds) {
        if(CollectionUtils.isEmpty(orderIds)) {
            return Collections.emptyList();
        }
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termsQuery(EsOrderListQueryDO.ORDER_ID, orderIds));
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(boolQueryBuilder)
            .build();
        SearchHits<EsOrderListQueryDO> hits = elasticsearchRestTemplate.search(nativeSearchQuery, EsOrderListQueryDO.class);
        List<EsOrderListQueryDO> indexList = hits.stream().map(SearchHit::getContent).collect(Collectors.toList());
        return indexList;
    }

    /**
     * 构建orderListQueryIndex
     */
    public void saveBatchByOrderIds(List<String> orderId, long timestamp) {
        List<OrderInfoDO> orderInfos = orderInfoMapper.selectListByOrderIds(orderId);
        this.saveBatchByOrderInfos(orderInfos, timestamp);
    }

    /**
     * 构建orderListQueryIndex
     */
    public void saveBatchByOrderInfos(List<OrderInfoDO> orderInfos, long timestamp) {
        List<String> orderIds = orderInfos.stream().map(OrderInfoDO::getOrderId).collect(Collectors.toList());
        // 1、订单条目
        List<OrderItemDO> orderItems = orderItemMapper.selectListByOrderIds(orderIds);
        Map<String, List<OrderItemDO>> orderItemsMap = orderItems.stream().collect(Collectors.groupingBy(OrderItemDO::getOrderId));
        // 2. 订单配送信息
        List<OrderDeliveryDetailDO> orderDeliveryDetails = orderDeliveryDetailMapper.selectListByOrderIds(orderIds);
        Map<String, OrderDeliveryDetailDO> orderDeliveryDetailMap = orderDeliveryDetails.stream().collect(Collectors.toMap(OrderDeliveryDetailDO::getOrderId, d -> d));
        // 3. 订单支付信息
        List<OrderPaymentDetailDO> orderPaymentDetails = orderPaymentDetailMapper.selectListByOrderIds(orderIds);
        Map<String, List<OrderPaymentDetailDO>> orderPaymentDetailsMap = orderPaymentDetails.stream().collect(Collectors.groupingBy(OrderPaymentDetailDO::getOrderId));

        this.saveBatchByOrderInfos(orderInfos, orderDeliveryDetailMap, orderItemsMap, orderPaymentDetailsMap, timestamp);
    }

    /**
     * 构建orderListQueryIndex
     */
    public void saveBatchByOrderInfos(List<OrderInfoDO> orderInfos, Map<String, OrderDeliveryDetailDO> orderDeliveryDetailMap, Map<String, List<OrderItemDO>> orderItemsMap, Map<String, List<OrderPaymentDetailDO>> orderPaymentDetailsMap, long timestamp) {
        List<EsOrderListQueryDO> list = new ArrayList<>();
        for (OrderInfoDO order : orderInfos) {
            OrderDeliveryDetailDO deliveryDetail = orderDeliveryDetailMap.get(order.getOrderId());
            List<OrderItemDO> orderItemList = orderItemsMap.get(order.getOrderId());
            List<OrderPaymentDetailDO> orderPaymentDetailList = orderPaymentDetailsMap.get(order.getOrderId());

            List<EsOrderListQueryDO> index = this.buildOrderListQueryIndex(order, deliveryDetail, orderItemList, orderPaymentDetailList);

            list.addAll(index);
        }

        this.saveBatch(list, EsOrderListQueryDO.class, timestamp);
    }

    /**
     * 构造订单列表查询es index
     */
    private List<EsOrderListQueryDO> buildOrderListQueryIndex(OrderInfoDO orderInfo, OrderDeliveryDetailDO deliveryDetail, List<OrderItemDO> orderItems, List<OrderPaymentDetailDO> orderPaymentDetails) {
        // 1. 根据order_info和order_delivery_detail构造index
        EsOrderListQueryDO baseIndex = new EsOrderListQueryDO();
        // baseIndex.setEsId();
        baseIndex.setBusinessIdentifier(orderInfo.getBusinessIdentifier());
        baseIndex.setOrderType(orderInfo.getOrderType());
        baseIndex.setOrderId(orderInfo.getOrderId());
        // baseIndex.setOrderItemId();
        // baseIndex.setProductType();
        baseIndex.setSellerId(orderInfo.getSellerId());
        baseIndex.setParentOrderId(orderInfo.getParentOrderId());
        baseIndex.setUserId(orderInfo.getUserId());
        baseIndex.setOrderStatus(orderInfo.getOrderStatus());
        if (deliveryDetail != null) {
            baseIndex.setReceiverPhone(deliveryDetail.getReceiverPhone());
            baseIndex.setReceiverName(deliveryDetail.getReceiverName());
        }
        // baseIndex.setTradeNo();
        // baseIndex.setSkuCode();
        // baseIndex.setProductName();
        baseIndex.setCreatedTime(orderInfo.getCreateTime());
        // baseIndex.setPayTime();
        // baseIndex.setPayType();
        baseIndex.setPayAmount(orderInfo.getPayAmount());

        // 2. 内连接order_item
        List<EsOrderListQueryDO> joinList1 = new ArrayList<>();
        if (CollectionUtils.isEmpty(orderItems)) {
            joinList1.add(baseIndex);
        } else {
            for (OrderItemDO item : orderItems) {
                // TODO 改造为mapstruct
                EsOrderListQueryDO newIndex = new EsOrderListQueryDO();
                BeanUtils.copyProperties(baseIndex, newIndex);

                newIndex.setOrderItemId(item.getOrderItemId());
                newIndex.setProductType(item.getProductType());
                newIndex.setSkuCode(item.getSkuCode());
                newIndex.setProductName(item.getProductName());

                joinList1.add(newIndex);
            }
        }

        // 3. 内连接order_payment_detail, 会出现笛卡尔积
        List<EsOrderListQueryDO> joinList2 = new ArrayList<>();
        if (CollectionUtils.isEmpty(orderPaymentDetails)) {
            joinList2 = joinList1;
        } else {
            for (OrderPaymentDetailDO detail : orderPaymentDetails) {
                for (EsOrderListQueryDO joinItem : joinList1) {
                    // TODO 改造为mapstruct
                    EsOrderListQueryDO newIndex = new EsOrderListQueryDO();
                    BeanUtils.copyProperties(joinItem, newIndex);

                    newIndex.setTradeNo(detail.getOutTradeNo());
                    newIndex.setPayTime(detail.getPayTime());
                    newIndex.setPayType(detail.getPayType());

                    joinList2.add(newIndex);
                }
            }
        }

        // 4. 初始化es_id
        for (EsOrderListQueryDO queryIndex : joinList2) {
            queryIndex.setEsId(queryIndex.getOrderItemId() + "_" + queryIndex.getPayType());
        }
        return joinList2;
    }

}
