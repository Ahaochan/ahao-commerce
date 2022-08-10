package moe.ahao.commerce.order.adapter.mq.handler;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleRefundDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleItemMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleRefundMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch.AfterSaleListEsRepository;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch.data.EsAfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch.data.EsAfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch.data.EsAfterSaleRefundDO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 售后单全量数据新增service：
 * <p>
 * 当after_sale_info被创建时（下单），会同时创建
 * 1.after_sale_info
 * 2.after_sale_item
 * 3.after_sale_refund
 * <p>
 * 于是在监听到after_sale_info的新增binlog日志时，需要将1～3的数据同步到es里面去
 */
@Service
@Slf4j
public class EsAfterSaleSyncHandler {
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;
    @Autowired
    private AfterSaleItemMapper afterSaleItemMapper;
    @Autowired
    private AfterSaleRefundMapper afterSaleRefundMapper;

    @Autowired
    private AfterSaleListEsRepository afterSaleListEsRepository;

    /**
     * 将售后单全量数据新增至es
     */
    public void syncFullDataByAfterSaleIds(List<String> afterSaleIds) {
        if (CollectionUtils.isEmpty(afterSaleIds)) {
            return;
        }
        List<AfterSaleInfoDO> afterSales = afterSaleInfoMapper.selectListByAfterSaleIds(afterSaleIds);
        this.syncFullData(afterSales);
    }

    /**
     * 将售后单全量数据新增至es
     */
    public void syncFullData(List<AfterSaleInfoDO> afterSales) {
        if (CollectionUtils.isEmpty(afterSales)) {
            return;
        }
        List<String> afterSaleIds = afterSales.stream().map(AfterSaleInfoDO::getAfterSaleId).collect(Collectors.toList());
        // 1. 售后单
        List<EsAfterSaleInfoDO> esAfterSales = afterSales.stream().map(this::convert).collect(Collectors.toList());
        afterSaleListEsRepository.saveBatch(esAfterSales, EsAfterSaleInfoDO.class);
        // 2. 售后单条目
        List<AfterSaleItemDO> afterSaleItems = afterSaleItemMapper.selectListByAfterSaleIds(afterSaleIds);
        List<EsAfterSaleItemDO> esAfterSaleItems = afterSaleItems.stream().map(this::convert).collect(Collectors.toList());
        afterSaleListEsRepository.saveBatch(esAfterSaleItems, EsAfterSaleItemDO.class);
        // 3. 售后退款信息
        List<AfterSaleRefundDO> afterSaleRefunds = afterSaleRefundMapper.selectListByAfterSaleIds(afterSaleIds);
        List<EsAfterSaleRefundDO> esAfterSaleRefunds = afterSaleRefunds.stream().map(this::convert).collect(Collectors.toList());
        afterSaleListEsRepository.saveBatch(esAfterSaleRefunds, EsAfterSaleRefundDO.class);

        // 5. 构建afterSaleListQueryIndex并同步到es
        Map<String, AfterSaleRefundDO> afterSaleRefundMap = afterSaleRefunds.stream()
            .collect(Collectors.toMap(AfterSaleRefundDO::getAfterSaleId, r -> r));
        Map<String, List<AfterSaleItemDO>> afterSaleItemsMap = afterSaleItems.stream().collect(Collectors.groupingBy(AfterSaleItemDO::getAfterSaleId));
        afterSaleListEsRepository.saveBatchByAfterSales(afterSales, afterSaleItemsMap, afterSaleRefundMap, -1);
    }

    /**
     * 将售后单同步至es
     */
    public void syncAfterSales(List<String> afterSaleIds, long timestamp) {
        // 1. 查询售后单
        if (CollectionUtils.isEmpty(afterSaleIds)) {
            return;
        }
        List<AfterSaleInfoDO> afterSales = afterSaleInfoMapper.selectListByAfterSaleIds(afterSaleIds);
        if (CollectionUtils.isEmpty(afterSales)) {
            return;
        }

        // 2. 售后信息
        List<EsAfterSaleInfoDO> esOrders = afterSales.stream().map(this::convert).collect(Collectors.toList());
        afterSaleListEsRepository.saveBatch(esOrders, EsAfterSaleInfoDO.class);

        // 3. 构建afterSaleListQueryIndex并同步到es
        afterSaleListEsRepository.saveBatchByAfterSales(afterSales, timestamp);
    }

    /**
     * 将售后单条目同步至es
     */
    public void syncAfterSaleItems(List<String> afterSaleIds, long timestamp) {
        // 1. 查询售后单条目
        if (CollectionUtils.isEmpty(afterSaleIds)) {
            return;
        }
        List<AfterSaleItemDO> afterSaleItems = afterSaleItemMapper.selectListByAfterSaleIds(afterSaleIds);
        if (CollectionUtils.isEmpty(afterSaleItems)) {
            return;
        }

        // 2. 售后单条目
        List<EsAfterSaleItemDO> esAfterSaleItems = afterSaleItems.stream().map(this::convert).collect(Collectors.toList());
        afterSaleListEsRepository.saveBatch(esAfterSaleItems, EsAfterSaleItemDO.class);

        // 3. 构建afterSaleListQueryIndex并同步到es
        afterSaleListEsRepository.saveBatchByAfterSaleIds(afterSaleIds, timestamp);
    }

    /**
     * 将售后单条目同步至es
     */
    public void syncAfterSaleRefund(List<String> afterSaleIds, long timestamp) {
        // 1. 查询售后退款信息
        if (CollectionUtils.isEmpty(afterSaleIds)) {
            return;
        }
        List<AfterSaleRefundDO> afterSaleRefunds = afterSaleRefundMapper.selectListByAfterSaleIds(afterSaleIds);
        if (CollectionUtils.isEmpty(afterSaleRefunds)) {
            return;
        }

        // 2. 售后信息
        List<EsAfterSaleRefundDO> esAfterSaleRefunds = afterSaleRefunds.stream().map(this::convert).collect(Collectors.toList());
        afterSaleListEsRepository.saveBatch(esAfterSaleRefunds, EsAfterSaleRefundDO.class);

        // 3. 构建afterSaleListQueryIndex并同步到es
        afterSaleListEsRepository.saveBatchByAfterSaleIds(afterSaleIds, timestamp);
    }

    private EsAfterSaleRefundDO convert(AfterSaleRefundDO that) {
        EsAfterSaleRefundDO _this = new EsAfterSaleRefundDO();
        // 以售后单id作为es的主键id
        _this.setEsId(that.getAfterSaleId());
        _this.setId(that.getId());
        _this.setCreateTime(that.getCreateTime());
        _this.setUpdateTime(that.getUpdateTime());
        _this.setAfterSaleId(that.getAfterSaleId());
        _this.setOrderId(that.getOrderId());
        _this.setAfterSaleBatchNo(that.getAfterSaleBatchNo());
        _this.setAccountType(that.getAccountType());
        _this.setPayType(that.getPayType());
        _this.setRefundStatus(that.getRefundStatus());
        _this.setRefundAmount(that.getRefundAmount());
        _this.setRefundPayTime(that.getRefundPayTime());
        _this.setOutTradeNo(that.getOutTradeNo());
        _this.setRemark(that.getRemark());
        return null;
    }

    public EsAfterSaleInfoDO convert(AfterSaleInfoDO that) {
        EsAfterSaleInfoDO _this = new EsAfterSaleInfoDO();
        // 以售后单id作为es的主键id
        _this.setEsId(that.getAfterSaleId());
        _this.setId(that.getId());
        _this.setCreateTime(that.getCreateTime());
        _this.setUpdateTime(that.getUpdateTime());
        _this.setAfterSaleId(that.getAfterSaleId());
        _this.setBusinessIdentifier(that.getBusinessIdentifier());
        _this.setOrderId(that.getOrderId());
        _this.setUserId(that.getUserId());
        _this.setOrderType(that.getOrderType());
        _this.setApplySource(that.getApplySource());
        _this.setApplyTime(that.getApplyTime());
        _this.setApplyReasonCode(that.getApplyReasonCode());
        _this.setApplyReason(that.getApplyReason());
        _this.setReviewTime(that.getReviewTime());
        _this.setReviewSource(that.getReviewSource());
        _this.setReviewReasonCode(that.getReviewReasonCode());
        _this.setReviewReason(that.getReviewReason());
        _this.setAfterSaleType(that.getAfterSaleType());
        _this.setAfterSaleTypeDetail(that.getAfterSaleTypeDetail());
        _this.setAfterSaleStatus(that.getAfterSaleStatus());
        _this.setApplyRefundAmount(that.getApplyRefundAmount());
        _this.setRealRefundAmount(that.getRealRefundAmount());
        _this.setRemark(that.getRemark());
        return _this;
    }

    public EsAfterSaleItemDO convert(AfterSaleItemDO that) {
        EsAfterSaleItemDO _this = new EsAfterSaleItemDO();
        // 以售后单id和skuCode作为es的主键id
        _this.setEsId(that.getAfterSaleId() + "_" + that.getSkuCode());
        _this.setId(that.getId());
        _this.setCreateTime(that.getCreateTime());
        _this.setUpdateTime(that.getUpdateTime());
        _this.setAfterSaleId(that.getAfterSaleId());
        _this.setOrderId(that.getOrderId());
        _this.setSkuCode(that.getSkuCode());
        _this.setProductName(that.getProductName());
        _this.setProductImg(that.getProductImg());
        _this.setReturnQuantity(that.getReturnQuantity());
        _this.setOriginAmount(that.getOriginAmount());
        _this.setApplyRefundAmount(that.getApplyRefundAmount());
        _this.setRealRefundAmount(that.getRealRefundAmount());
        _this.setReturnCompletionMark(that.getReturnCompletionMark());
        _this.setAfterSaleItemType(that.getAfterSaleItemType());
        return _this;
    }
}
