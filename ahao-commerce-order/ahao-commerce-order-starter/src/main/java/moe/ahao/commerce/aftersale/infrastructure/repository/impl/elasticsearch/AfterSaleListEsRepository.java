package moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch;


import moe.ahao.commerce.aftersale.api.enums.AfterSaleQuerySortField;
import moe.ahao.commerce.aftersale.api.query.AfterSalePageQuery;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleRefundDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleItemMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleRefundMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch.data.EsAfterSaleListQueryDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.ElasticsearchRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class AfterSaleListEsRepository extends ElasticsearchRepository {
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;
    @Autowired
    private AfterSaleItemMapper afterSaleItemMapper;
    @Autowired
    private AfterSaleRefundMapper afterSaleRefundMapper;

    public AfterSaleListEsRepository(ElasticsearchRestTemplate elasticsearchRestTemplate) {
        super(elasticsearchRestTemplate);
    }

    public SearchHits<EsAfterSaleListQueryDO> query(AfterSalePageQuery query) {
        // 1. 构造and等值查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (query.getBusinessIdentifier() != null) {
            boolQueryBuilder.must(QueryBuilders.termQuery(EsAfterSaleListQueryDO.BUSINESS_IDENTIFIER, query.getBusinessIdentifier()));
        }
        if (CollectionUtils.isNotEmpty(query.getAfterSaleIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsAfterSaleListQueryDO.AFTER_SALE_ID, query.getAfterSaleIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getOrderTypes())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsAfterSaleListQueryDO.ORDER_TYPE, query.getOrderTypes()));
        }
        if (CollectionUtils.isNotEmpty(query.getOrderIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsAfterSaleListQueryDO.ORDER_ID, query.getOrderIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getAfterSaleStatus())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsAfterSaleListQueryDO.AFTER_SALE_STATUS, query.getAfterSaleStatus()));
        }
        if (CollectionUtils.isNotEmpty(query.getApplySources())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsAfterSaleListQueryDO.APPLY_SOURCE, query.getApplySources()));
        }
        if (CollectionUtils.isNotEmpty(query.getAfterSaleTypes())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsAfterSaleListQueryDO.AFTER_SALE_TYPE, query.getAfterSaleTypes()));
        }
        if (CollectionUtils.isNotEmpty(query.getUserIds())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsAfterSaleListQueryDO.USER_ID, query.getUserIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getSkuCodes())) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(EsAfterSaleListQueryDO.SKU_CODE, query.getSkuCodes()));
        }
        super.range(boolQueryBuilder, EsAfterSaleListQueryDO.CREATED_TIME, query.getQueryStartCreatedTime(), query.getQueryEndCreatedTime());
        super.range(boolQueryBuilder, EsAfterSaleListQueryDO.APPLY_TIME, query.getQueryStartApplyTime(), query.getQueryEndApplyTime());
        super.range(boolQueryBuilder, EsAfterSaleListQueryDO.REVIEW_TIME, query.getQueryStartReviewTime(), query.getQueryEndReviewTime());
        super.range(boolQueryBuilder, EsAfterSaleListQueryDO.REFUND_PAY_TIME, query.getQueryStartRefundPayTime(), query.getQueryEndRefundPayTime());
        super.range(boolQueryBuilder, EsAfterSaleListQueryDO.REFUND_AMOUNT, query.getQueryStartRefundAmount(), query.getQueryEndRefundAmount());
        // 2. 设置排序
        String sortField = query.getSort().equals(AfterSaleQuerySortField.REFUND_TIME_DESC) ? EsAfterSaleListQueryDO.REFUND_PAY_TIME : EsAfterSaleListQueryDO.CREATED_TIME;

        // 3. 查询es, 获取返回hits
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(boolQueryBuilder)
            .withPageable(PageRequest.of(query.getPageNo(), query.getPageSize()))
            .withSort(new FieldSortBuilder(sortField).order(SortOrder.DESC))
            .build();
        SearchHits<EsAfterSaleListQueryDO> hits = elasticsearchRestTemplate.search(nativeSearchQuery, EsAfterSaleListQueryDO.class);
        return hits;
    }

    /**
     * 构建afterSaleListQueryIndex
     */
    public void saveBatchByAfterSaleIds(List<String> afterSaleIds, long timestamp) {
        List<AfterSaleInfoDO> afterSales = afterSaleInfoMapper.selectListByAfterSaleIds(afterSaleIds);
        this.saveBatchByAfterSales(afterSales, timestamp);
    }

    /**
     * 构建afterSaleListQueryIndex
     */
    public void saveBatchByAfterSales(List<AfterSaleInfoDO> afterSales, long timestamp) {
        List<String> afterSaleIds = afterSales.stream().map(AfterSaleInfoDO::getAfterSaleId).collect(Collectors.toList());
        // 1、售后单条目
        List<AfterSaleItemDO> afterSaleItems = afterSaleItemMapper.selectListByAfterSaleIds(afterSaleIds);
        Map<String, List<AfterSaleItemDO>> afterSaleItemMap = afterSaleItems.stream().collect(Collectors.groupingBy(AfterSaleItemDO::getAfterSaleId));
        // 2. 售后退款单
        List<AfterSaleRefundDO> afterSaleRefunds = afterSaleRefundMapper.selectListByAfterSaleIds(afterSaleIds);
        Map<String, AfterSaleRefundDO> afterSaleRefundMap = afterSaleRefunds.stream().collect(Collectors.toMap(AfterSaleRefundDO::getAfterSaleId, d -> d));

        this.saveBatchByAfterSales(afterSales, afterSaleItemMap, afterSaleRefundMap, timestamp);
    }

    /**
     * 构建afterSaleListQueryIndex
     */
    public void saveBatchByAfterSales(List<AfterSaleInfoDO> afterSales, Map<String, List<AfterSaleItemDO>> afterSaleItemsMap, Map<String, AfterSaleRefundDO> afterSaleRefundMap, long timestamp) {
        List<EsAfterSaleListQueryDO> list = new ArrayList<>();
        for (AfterSaleInfoDO afterSale : afterSales) {
            AfterSaleRefundDO afterSaleRefund = afterSaleRefundMap.get(afterSale.getAfterSaleId());
            List<AfterSaleItemDO> afterSaleItemList = afterSaleItemsMap.get(afterSale.getAfterSaleId());

            List<EsAfterSaleListQueryDO> index = this.buildAfterSaleListQueryIndex(afterSale, afterSaleRefund, afterSaleItemList);

            list.addAll(index);
        }

        this.saveBatch(list, EsAfterSaleListQueryDO.class, timestamp);
    }

    /**
     * 构造订单列表查询es index
     */
    private List<EsAfterSaleListQueryDO> buildAfterSaleListQueryIndex(AfterSaleInfoDO afterSale, AfterSaleRefundDO afterSaleRefund, List<AfterSaleItemDO> afterSaleItems) {
        // 1. 先将after_sale_info和after_sale_refund进行内连接
        EsAfterSaleListQueryDO baseIndex = new EsAfterSaleListQueryDO();
        // baseIndex.setEsId();
        baseIndex.setBusinessIdentifier(afterSale.getBusinessIdentifier());
        baseIndex.setAfterSaleId(afterSale.getAfterSaleId());
        baseIndex.setOrderId(afterSale.getOrderId());
        baseIndex.setOrderType(afterSale.getOrderType());
        baseIndex.setAfterSaleStatus(afterSale.getAfterSaleStatus());
        baseIndex.setApplySource(afterSale.getApplySource());
        baseIndex.setAfterSaleType(afterSale.getAfterSaleType());
        baseIndex.setUserId(afterSale.getUserId());
        // baseIndex.setSkuCode();
        baseIndex.setCreatedTime(afterSale.getCreateTime());
        baseIndex.setApplyTime(afterSale.getApplyTime());
        baseIndex.setReviewTime(afterSale.getReviewTime());
        if (afterSaleRefund != null) {
            baseIndex.setRefundPayTime(afterSaleRefund.getRefundPayTime());
            baseIndex.setRefundAmount(afterSaleRefund.getRefundAmount());
        }

        // 2. 内连接after_sale_item
        List<EsAfterSaleListQueryDO> joinList1 = new ArrayList<>();
        if (CollectionUtils.isEmpty(afterSaleItems)) {
            joinList1.add(baseIndex);
        } else {
            for (AfterSaleItemDO item : afterSaleItems) {
                // TODO 改造为mapstruct
                EsAfterSaleListQueryDO newIndex = new EsAfterSaleListQueryDO();
                BeanUtils.copyProperties(baseIndex, newIndex);

                newIndex.setSkuCode(item.getSkuCode());

                joinList1.add(newIndex);
            }
        }

        // 3. 初始化es_id
        for (EsAfterSaleListQueryDO queryIndex : joinList1) {
            queryIndex.setEsId(queryIndex.getAfterSaleId() + "_" + queryIndex.getSkuCode());
        }
        return joinList1;
    }
}
