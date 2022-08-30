package com.ruyuan.eshop.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.ruyuan.eshop.common.bean.SpringApplicationContext;
import com.ruyuan.eshop.common.enums.*;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.common.tuple.Pair;
import com.ruyuan.eshop.common.utils.LoggerFormat;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.order.constants.AfterSaleQueryFiledNameConstant;
import com.ruyuan.eshop.order.converter.AfterSaleConverter;
import com.ruyuan.eshop.order.dao.AfterSaleInfoDAO;
import com.ruyuan.eshop.order.dao.AfterSaleItemDAO;
import com.ruyuan.eshop.order.dao.AfterSaleLogDAO;
import com.ruyuan.eshop.order.dao.AfterSaleRefundDAO;
import com.ruyuan.eshop.order.domain.dto.AfterSaleListQueryDTO;
import com.ruyuan.eshop.order.domain.dto.AfterSaleOrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.AfterSaleOrderListDTO;
import com.ruyuan.eshop.order.domain.dto.OrderLackItemDTO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleInfoDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleItemDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleLogDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleRefundDO;
import com.ruyuan.eshop.order.domain.query.AfterSaleQuery;
import com.ruyuan.eshop.order.elasticsearch.EsClientService;
import com.ruyuan.eshop.order.elasticsearch.dto.EsResponseDTO;
import com.ruyuan.eshop.order.elasticsearch.enums.EsIndexNameEnum;
import com.ruyuan.eshop.order.elasticsearch.query.AfterSaleListQueryIndex;
import com.ruyuan.eshop.order.enums.AfterSaleQueryDataTypeEnums;
import com.ruyuan.eshop.order.enums.AfterSaleQuerySortField;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.service.AfterSaleQueryService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class AfterSaleQueryServiceImpl implements AfterSaleQueryService {

    @Autowired
    private AfterSaleInfoDAO afterSaleInfoDAO;

    @Autowired
    private AfterSaleItemDAO afterSaleItemDAO;

    @Autowired
    private AfterSaleRefundDAO afterSaleRefundDAO;

    @Autowired
    private AfterSaleLogDAO afterSaleLogDAO;

    @Autowired
    private AfterSaleConverter afterSaleConverter;

    @Autowired
    private EsClientService esClientService;

    @Autowired
    private SpringApplicationContext springApplicationContext;


    @Override
    public void checkQueryParam(AfterSaleQuery query) {

        ParamCheckUtil.checkObjectNonNull(query.getBusinessIdentifier(), OrderErrorCodeEnum.BUSINESS_IDENTIFIER_IS_NULL);
        checkIntAllowableValues(query.getBusinessIdentifier(), BusinessIdentifierEnum.allowableValues(), "businessIdentifier");
        checkIntSetAllowableValues(query.getOrderTypes(), OrderTypeEnum.allowableValues(), "orderTypes");
        checkIntSetAllowableValues(query.getAfterSaleTypes(), AfterSaleStatusEnum.allowableValues(), "afterSaleStatus");
        checkIntSetAllowableValues(query.getApplySources(), AfterSaleApplySourceEnum.allowableValues(), "applySources");
        checkIntSetAllowableValues(query.getAfterSaleTypes(), AfterSaleTypeEnum.allowableValues(), "afterSaleTypes");


        Integer maxSize = AfterSaleQuery.MAX_PAGE_SIZE;
        checkSetMaxSize(query.getAfterSaleIds(), maxSize, "afterSaleIds");
        checkSetMaxSize(query.getOrderIds(), maxSize, "orderIds");
        checkSetMaxSize(query.getUserIds(), maxSize, "userIds");
        checkSetMaxSize(query.getSkuCodes(), maxSize, "skuCodes");

    }

    @Override
    public PagingInfo<AfterSaleOrderListDTO> executeListQueryV1(AfterSaleQuery query) {

        //第一阶段采用连表查询
        //第二阶段会接入es

        //1、组装业务查询规则
        if (CollectionUtils.isEmpty(query.getApplySources())) {
            //默认只展示用户主动发起的售后单
            query.setApplySources(AfterSaleApplySourceEnum.userApply());
        }
        AfterSaleListQueryDTO queryDTO = afterSaleConverter.afterSaleListQueryDTO(query);

        //2、查询
        Page<AfterSaleOrderListDTO> page = afterSaleInfoDAO.listByPage(queryDTO);

        //3、转化
        return PagingInfo.toResponse(page.getRecords()
                , page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public PagingInfo<AfterSaleOrderDetailDTO> executeListQueryV2(AfterSaleQuery query, AfterSaleQueryDataTypeEnums... queryDataTypes) throws Exception {
        log.info(LoggerFormat.build()
                .remark("afterSale:executeListQuery_v2->request")
                .finish());

        //1、组装业务查询规则
        if (CollectionUtils.isEmpty(query.getApplySources())) {
            //默认只展示用户主动发起的售后单
            query.setApplySources(AfterSaleApplySourceEnum.userApply());
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 构建条件查询
        BoolQueryBuilder boolQueryBuilder = buildBoolQueryBuilder(query);
        searchSourceBuilder.query(boolQueryBuilder);

        // 设置排序
        setSort(searchSourceBuilder, query);

        // 设置分页
        setPage(searchSourceBuilder, query);

        // 查询es
        EsResponseDTO esResponseDTO = esClientService.search(EsIndexNameEnum.AFTER_SALE_LIST_QUERY_INDEX, searchSourceBuilder);

        log.info(LoggerFormat.build()
                .remark("afterSale:executeListQuery_v2->es->response")
                .data("esResponseDTO", esResponseDTO)
                .finish());
        Integer total = (int) esResponseDTO.getTotal();

        List<AfterSaleOrderDetailDTO> afterSaleOrderDetailDTOs = new ArrayList<>(total);
        if (total > 0) {
            List<AfterSaleListQueryIndex> queryIndices = esResponseDTO.getData().toJavaList(AfterSaleListQueryIndex.class);
            // 从数据库/es查询售后单详情DTO
            queryIndices.forEach(index -> {
                afterSaleOrderDetailDTOs.add(buildAfterSaleDetail(index.getAfterSaleId(), queryDataTypes));
            });
        }

        // 转化
        return PagingInfo.toResponse(afterSaleOrderDetailDTOs
                , total.longValue(), query.getPageNo(), query.getPageSize());
    }

    @Override
    public AfterSaleOrderDetailDTO afterSaleDetailV1(String afterSaleId) {
        //1、查询售后单
        AfterSaleInfoDO afterSaleInfo = afterSaleInfoDAO.getOneByAfterSaleId(afterSaleId);

        if (null == afterSaleInfo) {
            return null;
        }

        //2、查询售后单条目
        List<AfterSaleItemDO> afterSaleItems = afterSaleItemDAO.listByAfterSaleId(afterSaleId);

        //3、查询售后支付信息
        List<AfterSaleRefundDO> afterSalePays = afterSaleRefundDAO.listByAfterSaleId(afterSaleId);

        //4、查询售后日志
        List<AfterSaleLogDO> afterSaleLogs = afterSaleLogDAO.listByAfterSaleId(afterSaleId);

        //5、构造返参
        return AfterSaleOrderDetailDTO.builder()
                .afterSaleInfo(afterSaleConverter.afterSaleInfoDO2DTO(afterSaleInfo))
                .afterSaleItems(afterSaleConverter.afterSaleItemDO2DTO(afterSaleItems))
                .afterSaleRefunds(afterSaleConverter.afterSaleRefundDO2DTO(afterSalePays))
                .afterSaleLogs(afterSaleConverter.afterSaleLogDO2DTO(afterSaleLogs))
                .build();
    }

    @Override
    public AfterSaleOrderDetailDTO afterSaleDetailV2(String afterSaleId, AfterSaleQueryDataTypeEnums... queryDataTypes) {
        log.info(LoggerFormat.build()
                .remark("afterSaleDetail_v2->request")
                .data("afterSaleId", afterSaleId)
                .data("queryDataTypes", queryDataTypes)
                .finish());
        // 构建订单详情DTO
        return buildAfterSaleDetail(afterSaleId, queryDataTypes);
    }

    @Override
    public List<OrderLackItemDTO> getOrderLackItemInfo(String orderId) {
        List<AfterSaleInfoDO> lackItemDO = afterSaleInfoDAO.listBy(orderId
                , Lists.newArrayList(AfterSaleTypeDetailEnum.LACK_REFUND.getCode()));
        if (CollectionUtils.isEmpty(lackItemDO)) {
            return null;
        }

        List<OrderLackItemDTO> lackItems = new ArrayList<>();

        lackItemDO.forEach(lackItem -> {
            AfterSaleOrderDetailDTO detailDTO = afterSaleDetailV1(lackItem.getAfterSaleId());
            OrderLackItemDTO itemDTO = new OrderLackItemDTO();
            BeanUtils.copyProperties(detailDTO, itemDTO);
            lackItems.add(itemDTO);
        });

        return lackItems;
    }

    /**
     * 构建售后单详情DTO
     *
     * @param afterSaleId    售后单id
     * @param queryDataTypes 查询项
     * @return
     */
    private AfterSaleOrderDetailDTO buildAfterSaleDetail(String afterSaleId, AfterSaleQueryDataTypeEnums... queryDataTypes) {
        if (Objects.isNull(queryDataTypes) || queryDataTypes.length == 0) {
            queryDataTypes = new AfterSaleQueryDataTypeEnums[]{AfterSaleQueryDataTypeEnums.AFTER_SALE};
        }
        AfterSaleDetailBuilder afterSaleDetailBuilder =
                springApplicationContext.getBean(AfterSaleDetailBuilder.class);

        for (AfterSaleQueryDataTypeEnums dataType : queryDataTypes) {
            afterSaleDetailBuilder.buildAfterSale(dataType, afterSaleId)
                    .buildAfterSaleItems(dataType, afterSaleId)
                    .buildAfterSaleRefunds(dataType, afterSaleId)
                    .buildAfterSaleLogs(dataType, afterSaleId);
        }

        if (afterSaleDetailBuilder.allNull()) {
            return null;
        }

        return afterSaleDetailBuilder.build();
    }


    /**
     * 设置es排序
     *
     * @param searchSourceBuilder
     * @param query
     */
    private void setSort(SearchSourceBuilder searchSourceBuilder, AfterSaleQuery query) {
        if (query.getSort().equals(AfterSaleQuerySortField.REFUND_TIME_DESC)) {
            searchSourceBuilder.sort(AfterSaleQueryFiledNameConstant.REFUND_PAY_TIME, SortOrder.DESC);
        } else {
            searchSourceBuilder.sort(AfterSaleQueryFiledNameConstant.CREATED_TIME, SortOrder.DESC);
        }
    }

    /**
     * 设置es分页
     *
     * @param searchSourceBuilder
     * @param query
     */
    private void setPage(SearchSourceBuilder searchSourceBuilder, AfterSaleQuery query) {
        searchSourceBuilder.from((query.getPageNo() - 1) * query.getPageSize())
                .size(query.getPageSize());
    }


    /**
     * 构造订单列表查询es bool查询条件
     *
     * @param query
     * @return
     */
    private BoolQueryBuilder buildBoolQueryBuilder(AfterSaleQuery query) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (Objects.nonNull(query.getBusinessIdentifier())) {
            boolQueryBuilder.must(QueryBuilders.termQuery(AfterSaleQueryFiledNameConstant.BUSINESS_IDENTIFIER, query.getBusinessIdentifier()));
        }
        if (CollectionUtils.isNotEmpty(query.getAfterSaleIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(AfterSaleQueryFiledNameConstant.AFTER_SALE_ID, query.getAfterSaleIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getOrderTypes())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(AfterSaleQueryFiledNameConstant.ORDER_TYPE, query.getOrderTypes()));
        }
        if (CollectionUtils.isNotEmpty(query.getOrderIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(AfterSaleQueryFiledNameConstant.ORDER_ID, query.getOrderIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getAfterSaleStatus())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(AfterSaleQueryFiledNameConstant.AFTER_SALE_STATUS, query.getAfterSaleStatus()));
        }
        if (CollectionUtils.isNotEmpty(query.getApplySources())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(AfterSaleQueryFiledNameConstant.APPLY_SOURCE, query.getApplySources()));
        }
        if (CollectionUtils.isNotEmpty(query.getAfterSaleTypes())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(AfterSaleQueryFiledNameConstant.AFTER_SALE_TYPE, query.getAfterSaleTypes()));
        }
        if (CollectionUtils.isNotEmpty(query.getUserIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(AfterSaleQueryFiledNameConstant.USER_ID, query.getUserIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getSkuCodes())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(AfterSaleQueryFiledNameConstant.SKU_CODE, query.getSkuCodes()));
        }
        if (Objects.nonNull(query.getCreatedTimeInterval())) {
            Pair<Date, Date> createdTimeInterval = query.getCreatedTimeInterval();
            boolQueryBuilder.must(QueryBuilders.rangeQuery(AfterSaleQueryFiledNameConstant.CREATED_TIME)
                    .gte(createdTimeInterval.getLeft()).lte(createdTimeInterval.getRight()));
        }
        if (Objects.nonNull(query.getApplyTimeInterval())) {
            Pair<Date, Date> applyTimeInterval = query.getApplyTimeInterval();
            boolQueryBuilder.must(QueryBuilders.rangeQuery(AfterSaleQueryFiledNameConstant.APPLY_TIME)
                    .gte(applyTimeInterval.getLeft()).lte(applyTimeInterval.getRight()));
        }
        if (Objects.nonNull(query.getReviewTimeInterval())) {
            Pair<Date, Date> reviewTimeInterval = query.getReviewTimeInterval();
            boolQueryBuilder.must(QueryBuilders.rangeQuery(AfterSaleQueryFiledNameConstant.REVIEW_TIME)
                    .gte(reviewTimeInterval.getLeft()).lte(reviewTimeInterval.getRight()));
        }
        if (Objects.nonNull(query.getRefundPayTimeInterval())) {
            Pair<Date, Date> refundPayTimeInterval = query.getRefundPayTimeInterval();
            boolQueryBuilder.must(QueryBuilders.rangeQuery(AfterSaleQueryFiledNameConstant.REFUND_PAY_TIME)
                    .gte(refundPayTimeInterval.getLeft()).lte(refundPayTimeInterval.getRight()));
        }

        if (Objects.nonNull(query.getRefundAmountInterval())) {
            Pair<Integer, Integer> refundAmountInterval = query.getRefundAmountInterval();
            boolQueryBuilder.must(QueryBuilders.rangeQuery(AfterSaleQueryFiledNameConstant.REFUND_AMOUNT)
                    .gte(refundAmountInterval.getLeft()).lte(refundAmountInterval.getRight()));
        }

        return boolQueryBuilder;
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
