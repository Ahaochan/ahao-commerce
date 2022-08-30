package com.ruyuan.eshop.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyuan.eshop.common.bean.SpringApplicationContext;
import com.ruyuan.eshop.common.enums.BusinessIdentifierEnum;
import com.ruyuan.eshop.common.enums.OrderStatusEnum;
import com.ruyuan.eshop.common.enums.OrderTypeEnum;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.common.tuple.Pair;
import com.ruyuan.eshop.common.utils.DateFormatUtil;
import com.ruyuan.eshop.common.utils.LoggerFormat;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.order.constants.OrderQueryFiledNameConstant;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.*;
import com.ruyuan.eshop.order.domain.dto.OrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.OrderLackItemDTO;
import com.ruyuan.eshop.order.domain.dto.OrderListDTO;
import com.ruyuan.eshop.order.domain.dto.OrderListQueryDTO;
import com.ruyuan.eshop.order.domain.entity.*;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import com.ruyuan.eshop.order.domain.request.OrderDetailRequest;
import com.ruyuan.eshop.order.elasticsearch.EsClientService;
import com.ruyuan.eshop.order.elasticsearch.dto.EsResponseDTO;
import com.ruyuan.eshop.order.elasticsearch.enums.EsIndexNameEnum;
import com.ruyuan.eshop.order.elasticsearch.query.OrderListQueryIndex;
import com.ruyuan.eshop.order.enums.OrderQueryDataTypeEnums;
import com.ruyuan.eshop.order.enums.OrderQuerySortField;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.service.AfterSaleQueryService;
import com.ruyuan.eshop.order.service.OrderLackService;
import com.ruyuan.eshop.order.service.OrderQueryService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderQueryServiceImpl implements OrderQueryService {

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private OrderAmountDetailDAO orderAmountDetailDAO;

    @Autowired
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private OrderAmountDAO orderAmountDAO;

    @Autowired
    private AfterSaleQueryService afterSaleQueryService;

    @Autowired
    private OrderLackService orderLackService;

    @Autowired
    private OrderConverter orderConverter;
    /**
     * 订单快照数据存储的DAO组件
     */
    @Autowired
    private OrderSnapshotDAO orderSnapshotDAO;
    /**
     * 订单操作日志存储的DAO组件
     */
    @Autowired
    private OrderOperateLogDAO orderOperateLogDAO;

    @Autowired
    private EsClientService esClientService;

    @Autowired
    private SpringApplicationContext springApplicationContext;


    @Override
    public void checkQueryParam(OrderQuery query) {

        ParamCheckUtil.checkObjectNonNull(query.getBusinessIdentifier(), OrderErrorCodeEnum.BUSINESS_IDENTIFIER_IS_NULL);
        checkIntAllowableValues(query.getBusinessIdentifier(), BusinessIdentifierEnum.allowableValues(), "businessIdentifier");
        checkIntSetAllowableValues(query.getOrderTypes(), OrderTypeEnum.allowableValues(), "orderTypes");
        checkIntSetAllowableValues(query.getOrderStatus(), OrderStatusEnum.allowableValues(), "orderStatus");


        Integer maxSize = OrderQuery.MAX_PAGE_SIZE;
        checkSetMaxSize(query.getOrderIds(), maxSize, "orderIds");
        checkSetMaxSize(query.getSellerIds(), maxSize, "sellerIds");
        checkSetMaxSize(query.getParentOrderIds(), maxSize, "parentOrderIds");
        checkSetMaxSize(query.getReceiverNames(), maxSize, "receiverNames");
        checkSetMaxSize(query.getReceiverPhones(), maxSize, "receiverPhones");
        checkSetMaxSize(query.getTradeNos(), maxSize, "tradeNos");
        checkSetMaxSize(query.getUserIds(), maxSize, "userIds");
        checkSetMaxSize(query.getSkuCodes(), maxSize, "skuCodes");
        checkSetMaxSize(query.getProductNames(), maxSize, "productNames");
    }

    @Override
    public PagingInfo<OrderListDTO> executeListQueryV1(OrderQuery query) {

        //第一阶段采用很low的连表查询，连接5张表，即使加索引，只要数据量稍微大一点查询性能就很低了
        //第二阶段会接入es，优化这块的查询性能

        //1、组装业务查询规则
        if (CollectionUtils.isEmpty(query.getOrderStatus())) {
            //不展示无效订单
            query.setOrderStatus(OrderStatusEnum.validStatus());
        }
        OrderListQueryDTO queryDTO = orderConverter.orderListQuery2DTO(query);
        log.info(LoggerFormat.build()
                .remark("executeListQuery->request")
                .data("request", query)
                .finish());
        //2、查询
        Page<OrderListDTO> page = orderInfoDAO.listByPage(queryDTO);

        //3、转化
        return PagingInfo.toResponse(page.getRecords()
                , page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public PagingInfo<OrderDetailDTO> executeListQueryV2(OrderQuery query,
                                                         OrderQueryDataTypeEnums... queryDataTypes) throws Exception {

        log.info(LoggerFormat.build()
                .remark("order:executeListQuery_v2->request")
                .finish());

        //1、组装业务查询规则
        if (CollectionUtils.isEmpty(query.getOrderStatus())) {
            //不展示无效订单
            query.setOrderStatus(OrderStatusEnum.validStatus());
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 构建全匹配查询
        BoolQueryBuilder boolQueryBuilder = buildMatchAllBoolQueryBuilder();
        searchSourceBuilder.query(boolQueryBuilder);

        // 设置排序
        setSort(searchSourceBuilder, query);

        // 设置分页
        setPage(searchSourceBuilder, query);

        // 查询es
        EsResponseDTO esResponseDTO = esClientService.search(EsIndexNameEnum.ORDER_LIST_QUERY_INDEX, searchSourceBuilder);

        log.info(LoggerFormat.build()
                .remark("order:executeListQuery_v2->es->response")
                .data("esResponseDTO", esResponseDTO)
                .finish());
        Long total = esResponseDTO.getTotal();

        List<OrderDetailDTO> orderDetailDTOs = new ArrayList<>(total.intValue());
        if (total > 0) {
            List<OrderListQueryIndex> queryIndices = esResponseDTO.getData().toJavaList(OrderListQueryIndex.class);
            // 从数据库/es查询订单详情DTO
            queryIndices.forEach(index -> {
                orderDetailDTOs.add(buildOrderDetail(index.getOrderId()));
            });
        }

        // 转化
        return PagingInfo.toResponse(orderDetailDTOs
                , total, query.getPageNo(), query.getPageSize());
    }

    @Override
    public OrderDetailDTO orderDetailV1(String orderId) {
        log.info(LoggerFormat.build()
                .remark("orderDetail_v1->request")
                .data("orderId", orderId)
                .finish());

        //1、查询订单
        OrderInfoDO orderInfo = orderInfoDAO.getByOrderId(orderId);
        if (null == orderInfo) {
            return null;
        }

        //2、查询订单条目
        List<OrderItemDO> orderItems = orderItemDAO.listByOrderId(orderId);

        //3、查询订单费用明细
        List<OrderAmountDetailDO> orderAmountDetails = orderAmountDetailDAO.listByOrderId(orderId);

        //4、查询订单配送信息
        OrderDeliveryDetailDO orderAmountDetail = orderDeliveryDetailDAO.getByOrderId(orderId);

        //5、查询订单支付明细
        List<OrderPaymentDetailDO> orderPaymentDetails = orderPaymentDetailDAO.listByOrderId(orderId);

        //6、查询订单费用类型
        List<OrderAmountDO> orderAmounts = orderAmountDAO.listByOrderId(orderId);

        //7、查询订单操作日志
        List<OrderOperateLogDO> orderOperateLogs = orderOperateLogDAO.listByOrderId(orderId);

        //8、查询订单快照
        List<OrderSnapshotDO> orderSnapshots = orderSnapshotDAO.queryOrderSnapshotByOrderId(orderId, OrderSnapshotDAOUtils.getRowKeyPrefixList(orderInfo));

        //9、查询缺品退款信息
        List<OrderLackItemDTO> lackItems = null;
        if (orderLackService.isOrderLacked(orderInfo)) {
            lackItems = afterSaleQueryService.getOrderLackItemInfo(orderId);
        }

        //10、构造返参
        return OrderDetailDTO.builder()
                .orderInfo(orderConverter.orderInfoDO2DTO(orderInfo))
                .orderItems(orderConverter.orderItemDO2DTO(orderItems))
                .orderAmountDetails(orderConverter.orderAmountDetailDO2DTO(orderAmountDetails))
                .orderDeliveryDetail(orderConverter.orderDeliveryDetailDO2DTO(orderAmountDetail))
                .orderPaymentDetails(orderConverter.orderPaymentDetailDO2DTO(orderPaymentDetails))
                .orderAmounts(orderAmounts.stream().collect(
                        Collectors.toMap(OrderAmountDO::getAmountType, OrderAmountDO::getAmount, (v1, v2) -> v1)))
                .orderOperateLogs(orderConverter.orderOperateLogsDO2DTO(orderOperateLogs))
                .orderSnapshots(orderConverter.orderSnapshotsDO2DTO(orderSnapshots))
                .lackItems(lackItems)
                .build();
    }

    @Override
    public OrderDetailDTO orderDetailV2(OrderDetailRequest request) {
        log.info(LoggerFormat.build()
                .remark("orderDetail_v2->request")
                .data("orderId", request.getOrderId())
                .data("queryDataTypes", request.getQueryDataTypes())
                .finish());
        try {
            // 构建订单详情DTO
            return buildOrderDetail(request.getOrderId(), request.getQueryDataTypes());
        } catch (Exception e) {
            log.error("查询订单详情异常，err={}", e);
            log.info("进行降级，查询es");
            return buildOrderDetail(request.getOrderId(), request.getQueryDataTypes());
        }
    }

    /**
     * 构建订单详情DTO
     *  @param orderId        订单id
     * @param queryDataTypes 查询项
     */
    private OrderDetailDTO buildOrderDetail(String orderId, OrderQueryDataTypeEnums... queryDataTypes) {
        if (Objects.isNull(queryDataTypes) || queryDataTypes.length == 0) {
            queryDataTypes = new OrderQueryDataTypeEnums[]{OrderQueryDataTypeEnums.ORDER};
        }
        OrderDetailBuilder orderDetailBuilder =
                springApplicationContext.getBean(OrderDetailBuilder.class);

        for (OrderQueryDataTypeEnums dataType : queryDataTypes) {
            orderDetailBuilder.buildOrderInfo(dataType, orderId)
                    .buildOrderItems(dataType, orderId)
                    .buildOrderAmountDetails(dataType, orderId)
                    .buildOrderDeliveryDetail(dataType, orderId)
                    .buildOrderPaymentDetails(dataType, orderId)
                    .buildOrderAmounts(dataType, orderId)
                    .buildOrderOperateLogs(dataType, orderId)
                    .buildOrderSnapshots(dataType, orderId)
                    .buildOrderLackItems(dataType, orderId);
        }

        if (orderDetailBuilder.allNull()) {
            return null;
        }

        return orderDetailBuilder.build();
    }

    /**
     * 设置es排序
     */
    private void setSort(SearchSourceBuilder searchSourceBuilder, OrderQuery query) {
        if (query.getSort().equals(OrderQuerySortField.PAY_TIME_DESC)) {
            searchSourceBuilder.sort(OrderQueryFiledNameConstant.PAY_TIME, SortOrder.DESC);
        } else {
            searchSourceBuilder.sort(OrderQueryFiledNameConstant.CREATED_TIME, SortOrder.DESC);
        }
    }

    /**
     * 设置es分页
     */
    private void setPage(SearchSourceBuilder searchSourceBuilder, OrderQuery query) {
        searchSourceBuilder.from((query.getPageNo() - 1) * query.getPageSize())
                .size(query.getPageSize());
    }

    /**
     * 构造订单列表查询es bool查询条件
     */
    private BoolQueryBuilder buildBoolQueryBuilder(OrderQuery query) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (Objects.nonNull(query.getBusinessIdentifier())) {
            boolQueryBuilder.must(QueryBuilders.termQuery(OrderQueryFiledNameConstant.BUSINESS_IDENTIFIER, query.getBusinessIdentifier()));
        }

        if (CollectionUtils.isNotEmpty(query.getOrderIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.ORDER_ID, query.getOrderIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getOrderTypes())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.ORDER_TYPE, query.getOrderTypes()));
        }
        if (CollectionUtils.isNotEmpty(query.getSellerIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.SELLER_ID, query.getSellerIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getParentOrderIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.PARENT_ORDER_ID, query.getParentOrderIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getUserIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.USER_ID, query.getUserIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getOrderStatus())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.ORDER_STATUS, query.getOrderStatus()));
        }
        if (CollectionUtils.isNotEmpty(query.getReceiverPhones())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.RECEIVER_PHONE, query.getReceiverPhones()));
        }
        if (CollectionUtils.isNotEmpty(query.getReceiverNames())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.RECEIVER_NAME, query.getReceiverNames()));
        }
        if (CollectionUtils.isNotEmpty(query.getTradeNos())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.TRADE_NO, query.getTradeNos()));
        }
        if (CollectionUtils.isNotEmpty(query.getSkuCodes())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.SKU_CODE, query.getSkuCodes()));
        }
        if (CollectionUtils.isNotEmpty(query.getProductNames())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.PRODUCT_NAME, query.getProductNames()));
        }
        if (CollectionUtils.isNotEmpty(query.getProductNames())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(OrderQueryFiledNameConstant.PRODUCT_NAME, query.getProductNames()));
        }
        if (Objects.nonNull(query.getCreatedTimeInterval())) {
            Pair<Date, Date> createdTimeInterval = query.getCreatedTimeInterval();
            boolQueryBuilder.must(QueryBuilders.rangeQuery(OrderQueryFiledNameConstant.CREATED_TIME)
                    .gte(DateFormatUtil.formatDateTime(createdTimeInterval.getLeft()))
                    .lte(DateFormatUtil.formatDateTime(createdTimeInterval.getRight()))
            );
        }
        if (Objects.nonNull(query.getPayTimeInterval())) {
            Pair<Date, Date> payTimeInterval = query.getPayTimeInterval();
            boolQueryBuilder.must(QueryBuilders.rangeQuery(OrderQueryFiledNameConstant.PAY_TIME)
                    .gte(DateFormatUtil.formatDateTime(payTimeInterval.getLeft()))
                    .lte(DateFormatUtil.formatDateTime(payTimeInterval.getRight()))
            );
        }
        if (Objects.nonNull(query.getPayAmountInterval())) {
            Pair<Integer, Integer> payAmountInterval = query.getPayAmountInterval();
            boolQueryBuilder.must(QueryBuilders.rangeQuery(OrderQueryFiledNameConstant.PAY_AMOUNT)
                    .gte(payAmountInterval.getLeft()).lte(payAmountInterval.getRight()));
        }

        return boolQueryBuilder;
    }

    /**
     * 构建订单列表查询es 全匹配查询
     * 可与 buildBoolQueryBuilder 交替调用，通过对比查看 全匹配查询 和 精准匹配查询 之间的区别
     * <p>
     * 调用精准匹配： BoolQueryBuilder boolQueryBuilder = buildBoolQueryBuilder(query);
     * 调用全匹配： BoolQueryBuilder boolQueryBuilder = buildMatchAllBoolQueryBuilder();
     */
    private BoolQueryBuilder buildMatchAllBoolQueryBuilder() {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        return boolQueryBuilder.filter(QueryBuilders.matchAllQuery());
    }

    private void checkIntAllowableValues(Integer i, Set<Integer> allowableValues, String paramName) {
        OrderErrorCodeEnum orderErrorCodeEnum = OrderErrorCodeEnum.ENUM_PARAM_MUST_BE_IN_ALLOWABLE_VALUE;
        ParamCheckUtil.checkIntAllowableValues(i
                , allowableValues,
                orderErrorCodeEnum, paramName, allowableValues);
    }

    private void checkIntSetAllowableValues(Set<Integer> set, Set<Integer> allowableValues, String paramName) {
        OrderErrorCodeEnum orderErrorCodeEnum = OrderErrorCodeEnum.ENUM_PARAM_MUST_BE_IN_ALLOWABLE_VALUE;
        ParamCheckUtil.checkIntSetAllowableValues(set
                , allowableValues,
                orderErrorCodeEnum, paramName, allowableValues);
    }

    private void checkSetMaxSize(Set setParam, Integer maxSize, String paramName) {
        OrderErrorCodeEnum orderErrorCodeEnum = OrderErrorCodeEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE;
        ParamCheckUtil.checkSetMaxSize(setParam, maxSize,
                orderErrorCodeEnum, paramName
                , maxSize);

    }
}
