package com.ruyuan.eshop.order.elasticsearch.handler.aftersale;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.common.collect.Lists;
import com.ruyuan.eshop.order.converter.EsEntityConverter;
import com.ruyuan.eshop.order.dao.AfterSaleInfoDAO;
import com.ruyuan.eshop.order.dao.AfterSaleItemDAO;
import com.ruyuan.eshop.order.dao.AfterSaleRefundDAO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleInfoDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleItemDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleRefundDO;
import com.ruyuan.eshop.order.elasticsearch.EsClientService;
import com.ruyuan.eshop.order.elasticsearch.handler.EsAbstractHandler;
import com.ruyuan.eshop.order.elasticsearch.query.AfterSaleListQueryIndex;
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
 * 构建售后单列表查询es index
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class EsAfterSaleListQueryIndexHandler extends EsAbstractHandler {


    @Autowired
    private EsEntityConverter esEntityConverter;

    @Autowired
    private EsClientService esClientService;

    @Autowired
    @Qualifier("orderThreadPool")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private AfterSaleInfoDAO afterSaleInfoDAO;

    @Autowired
    private AfterSaleItemDAO afterSaleItemDAO;

    @Autowired
    private AfterSaleRefundDAO afterSaleRefundDAO;

    /**
     * 构建afterSaleListQueryIndex
     */
    public List<AfterSaleListQueryIndex> buildAfterSaleListQueryIndex(List<AfterSaleInfoDO> afterSales, List<AfterSaleItemDO> afterSaleItems
            , List<AfterSaleRefundDO> afterSaleRefunds) {
        Map<String, AfterSaleRefundDO> afterSaleRefundMap = afterSaleRefunds.stream()
                .collect(Collectors.toMap(AfterSaleRefundDO::getAfterSaleId, r -> r));
        Map<String, List<AfterSaleItemDO>> afterSaleItemsMap = afterSaleItems.stream().collect(Collectors.groupingBy(AfterSaleItemDO::getAfterSaleId));

        List<AfterSaleListQueryIndex> result = new ArrayList<>();

        for (AfterSaleInfoDO afterSale : afterSales) {
            AfterSaleRefundDO afterSaleRefund = afterSaleRefundMap.get(afterSale.getAfterSaleId());
            List<AfterSaleItemDO> afterSaleItemList = afterSaleItemsMap.get(afterSale.getAfterSaleId());

            // 构建
            result.addAll(build(afterSale, afterSaleRefund, afterSaleItemList));
        }

        // 设置esId
        setEsIdOfAfterSaleListQueryIndex(result);

        return result;
    }

    /**
     * 同步到es
     */
    public void sycToEs(List<AfterSaleListQueryIndex> afterSaleListQueryIndices) throws Exception {
        sycToEs(afterSaleListQueryIndices, -1);
    }

    /**
     * 同步到es
     */
    public void sycToEs(List<AfterSaleListQueryIndex> afterSaleListQueryIndices, long timestamp) throws Exception {
        log.info("同步AfterSaleListQueryIndex到es , afterSaleListQueryIndices={}, timestamp={}", JSONObject.toJSONString(afterSaleListQueryIndices), timestamp);
        if (timestamp <= -1) {
            esClientService.bulkIndex(afterSaleListQueryIndices);
        } else {
            esClientService.bulkIndexWithVersionControl(afterSaleListQueryIndices, timestamp);
        }

    }


    /**
     * 异步构建AfterSaleListQueryListIndex并同步到es
     */
    public void asyncBuildAndSynToEs(List<AfterSaleInfoDO> afterSales, List<String> afterSaleIds, long timestamp) {
        taskExecutor.execute(() -> {
            try {
                buildAndSynToEs(afterSales, afterSaleIds, timestamp);
            } catch (Exception e) {
                log.error("同步AfterSaleListQueryIndex到es异常，err={}", e.getMessage(), e);
            }
        });
    }

    /**
     * 异步构建AfterSaleListQueryListIndex并同步到es
     */
    public void asyncBuildAndSynToEs(List<String> afterSaleIds, long timestamp) {
        taskExecutor.execute(() -> {
            try {
                // 查询售后单
                List<AfterSaleInfoDO> afterSales = afterSaleInfoDAO.listByAfterSaleIds(afterSaleIds);
                if (CollectionUtils.isEmpty(afterSales)) {
                    return;
                }

                buildAndSynToEs(afterSales, afterSaleIds, timestamp);
            } catch (Exception e) {
                log.error("同步AfterSaleListQueryIndex到es异常，err={}", e.getMessage(), e);
            }
        });
    }

    private void buildAndSynToEs(List<AfterSaleInfoDO> afterSales, List<String> afterSaleIds, long timestamp) throws Exception {
        // 1、查询售后单条目
        List<AfterSaleItemDO> afterSaleItems = afterSaleItemDAO.listByAfterSaleIds(afterSaleIds);

        // 2、查询售后单退款信息
        List<AfterSaleRefundDO> afterSaleRefunds = afterSaleRefundDAO.listByAfterSaleIds(afterSaleIds);

        List<AfterSaleListQueryIndex> afterSaleListQueryIndices =
                buildAfterSaleListQueryIndex(afterSales, afterSaleItems, afterSaleRefunds);
        sycToEs(afterSaleListQueryIndices, timestamp);
    }

    /**
     * 构造订单列表查询es index
     */
    private List<AfterSaleListQueryIndex> build(AfterSaleInfoDO afterSale, AfterSaleRefundDO afterSaleRefund
            , List<AfterSaleItemDO> afterSaleItems) {

        // 1、先将after_sale_info和after_sale_refund进行内连接
        AfterSaleListQueryIndex queryIndex = afterSaleInfoInnerJoinAfterSaleRefund(afterSale, afterSaleRefund);

        // 2、最后内连接after_sale_item

        return andInnerJoinWithAfterSaleItem(queryIndex, afterSaleItems);
    }


    /**
     * 将after_sale_info和after_sale_refund进行内连接，after_sale_info和after_sale_refund是1:1
     */
    private AfterSaleListQueryIndex afterSaleInfoInnerJoinAfterSaleRefund(AfterSaleInfoDO afterSale, AfterSaleRefundDO afterSaleRefund) {
        AfterSaleListQueryIndex.AfterSaleListQueryIndexBuilder builder = AfterSaleListQueryIndex.builder()
                .businessIdentifier(afterSale.getBusinessIdentifier())
                .orderId(afterSale.getOrderId())
                .afterSaleId(afterSale.getAfterSaleId())
                .orderType(afterSale.getOrderType())
                .afterSaleStatus(afterSale.getAfterSaleStatus())
                .applySource(afterSale.getApplySource())
                .afterSaleType(afterSale.getAfterSaleType())
                .userId(afterSale.getUserId())
                .createdTime(afterSale.getGmtCreate())
                .applyTime(afterSale.getApplyTime())
                .reviewTime(afterSale.getReviewTime());
        if (null != afterSaleRefund) {
            builder.refundPayTime(afterSaleRefund.getRefundPayTime())
                    .refundAmount(afterSaleRefund.getRefundAmount());
        }

        return builder.build();
    }

    /**
     * 再和after_sale_item进行内连接，after_sale_info*after_sale_refund和after_sale_item是1:N
     */
    private List<AfterSaleListQueryIndex> andInnerJoinWithAfterSaleItem(AfterSaleListQueryIndex originQueryIndex, List<AfterSaleItemDO> afterSaleItems) {
        if (CollectionUtils.isEmpty(afterSaleItems)) {
            return Lists.newArrayList(originQueryIndex);
        }

        List<AfterSaleListQueryIndex> result = new ArrayList<>();
        for (AfterSaleItemDO item : afterSaleItems) {
            AfterSaleListQueryIndex newQueryIndex = esEntityConverter.copyAfterSaleListQueryIndex(originQueryIndex);
            newQueryIndex.setSkuCode(item.getSkuCode());
            result.add(newQueryIndex);
        }
        return result;
    }

}
