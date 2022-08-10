package moe.ahao.commerce.aftersale.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import moe.ahao.commerce.aftersale.api.dto.*;
import moe.ahao.commerce.aftersale.api.query.AfterSalePageQuery;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleLogDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleRefundDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleItemMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleLogMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleRefundMapper;
import moe.ahao.commerce.common.enums.*;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch.AfterSaleListEsRepository;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch.data.EsAfterSaleListQueryDO;
import moe.ahao.domain.entity.PagingInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 售后查询service
 */
@Service
public class AfterSaleQueryService {
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;
    @Autowired
    private AfterSaleItemMapper afterSaleItemMapper;
    @Autowired
    private AfterSaleRefundMapper afterSaleRefundMapper;
    @Autowired
    private AfterSaleLogMapper afterSaleLogMapper;

    @Autowired
    private AfterSaleListEsRepository afterSaleListEsRepository;

    /**
     * 执行列表查询
     */
    public PagingInfo<AfterSaleOrderListDTO> queryV1(AfterSalePageQuery query) {
        // 1. 参数校验
        this.check(query);
        // 2. 组装业务查询规则
        if (CollectionUtils.isEmpty(query.getApplySources())) {
            // 默认只展示用户主动发起的售后单
            query.setApplySources(AfterSaleApplySourceEnum.userApply());
        }

        // 第一阶段采用连表查询
        // 第二阶段会接入es

        // 2. 查询
        Page<AfterSaleOrderListDTO> page = afterSaleInfoMapper.selectPage(new Page<>(query.getPageNo(), query.getPageSize()), query);

        // 3. 转化
        PagingInfo<AfterSaleOrderListDTO> pagingInfo = new PagingInfo<>();
        pagingInfo.setPageNo((int) page.getCurrent());
        pagingInfo.setPageSize((int) page.getSize());
        pagingInfo.setTotal(page.getTotal());
        pagingInfo.setList(page.getRecords());
        return pagingInfo;
    }

    public PagingInfo<AfterSaleOrderDetailDTO> queryV2(AfterSalePageQuery query) {
        // 1. 参数校验
        this.check(query);

        // 2. 在ES查询
        if (CollectionUtils.isEmpty(query.getApplySources())) {
            //默认只展示用户主动发起的售后单
            query.setApplySources(AfterSaleApplySourceEnum.userApply());
        }
        SearchHits<EsAfterSaleListQueryDO> hits = afterSaleListEsRepository.query(query);

        // 3. 转化
        List<AfterSaleOrderDetailDTO> afterSaleOrderDetailList = new ArrayList<>();
        for (SearchHit<EsAfterSaleListQueryDO> hit : hits) {
            EsAfterSaleListQueryDO content = hit.getContent();
            String afterSaleId = content.getAfterSaleId();

            AfterSaleOrderDetailDTO afterSaleOrderDetail = this.afterSaleDetail(afterSaleId);
            afterSaleOrderDetailList.add(afterSaleOrderDetail);
        }
        PagingInfo<AfterSaleOrderDetailDTO> pagingInfo = new PagingInfo<>();
        pagingInfo.setPageNo(query.getPageNo());
        pagingInfo.setPageSize(query.getPageSize());
        pagingInfo.setTotal(hits.getTotalHits());
        pagingInfo.setList(afterSaleOrderDetailList);
        return pagingInfo;
    }

    /**
     * 校验列表查询参数
     */
    public void check(AfterSalePageQuery query) {
        Integer businessIdentifier = query.getBusinessIdentifier();
        if (businessIdentifier == null) {
            throw OrderExceptionEnum.BUSINESS_IDENTIFIER_IS_NULL.msg();
        }
        BusinessIdentifierEnum businessIdentifierEnum = BusinessIdentifierEnum.getByCode(businessIdentifier);
        if (businessIdentifierEnum == null) {
            throw OrderExceptionEnum.BUSINESS_IDENTIFIER_ERROR.msg();
        }

        Set<Integer> orderTypes = query.getOrderTypes();
        if (CollectionUtils.isNotEmpty(orderTypes) && !OrderTypeEnum.allowableValues().containsAll(orderTypes)) {
            throw OrderExceptionEnum.ORDER_TYPE_ERROR.msg();
        }
        Set<Integer> afterSaleStatus = query.getAfterSaleStatus();
        if (CollectionUtils.isNotEmpty(afterSaleStatus) && !AfterSaleStatusEnum.allowableValues().containsAll(afterSaleStatus)) {
            throw OrderExceptionEnum.ENUM_PARAM_MUST_BE_IN_ALLOWABLE_VALUE.msg("afterSaleStatus", AfterSaleStatusEnum.allowableValues());
        }
        Set<Integer> applySources = query.getApplySources();
        if (CollectionUtils.isNotEmpty(applySources) && !AfterSaleApplySourceEnum.allowableValues().containsAll(applySources)) {
            throw OrderExceptionEnum.ENUM_PARAM_MUST_BE_IN_ALLOWABLE_VALUE.msg("applySources", AfterSaleApplySourceEnum.allowableValues());
        }
        Set<Integer> afterSaleTypes = query.getAfterSaleTypes();
        if (CollectionUtils.isNotEmpty(afterSaleTypes) && !AfterSaleTypeEnum.allowableValues().containsAll(afterSaleTypes)) {
            throw OrderExceptionEnum.ENUM_PARAM_MUST_BE_IN_ALLOWABLE_VALUE.msg("afterSaleTypes", AfterSaleTypeEnum.allowableValues());
        }

        int maxSize = AfterSalePageQuery.MAX_PAGE_SIZE;
        Set<String> afterSaleIds = query.getAfterSaleIds();
        if (CollectionUtils.isNotEmpty(afterSaleIds) && afterSaleIds.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("afterSaleIds", maxSize);
        }
        Set<String> orderIds = query.getOrderIds();
        if (CollectionUtils.isNotEmpty(orderIds) && orderIds.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("orderIds", maxSize);
        }
        Set<String> userIds = query.getUserIds();
        if (CollectionUtils.isNotEmpty(userIds) && userIds.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("userIds", maxSize);
        }
        Set<String> skuCodes = query.getSkuCodes();
        if (CollectionUtils.isNotEmpty(skuCodes) && skuCodes.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("skuCodes", maxSize);
        }
    }

    /**
     * 查询售后单详情
     */
    public AfterSaleOrderDetailDTO afterSaleDetail(String afterSaleId) {
        // 1. 参数校验
        if (StringUtils.isEmpty(afterSaleId)) {
            throw OrderExceptionEnum.AFTER_SALE_ID_IS_NULL.msg();
        }
        // 2. 查询售后单
        AfterSaleInfoDO afterSaleInfo = afterSaleInfoMapper.selectOneByAfterSaleId(afterSaleId);
        if (afterSaleInfo == null) {
            return null;
        }
        AfterSaleOrderDetailDTO dto = new AfterSaleOrderDetailDTO();
        dto.setAfterSaleId(afterSaleInfo.getAfterSaleId());
        dto.setBusinessIdentifier(afterSaleInfo.getBusinessIdentifier());
        dto.setOrderId(afterSaleInfo.getOrderId());
        dto.setUserId(afterSaleInfo.getUserId());
        dto.setOrderType(afterSaleInfo.getOrderType());
        dto.setApplySource(afterSaleInfo.getApplySource());
        dto.setApplyTime(afterSaleInfo.getApplyTime());
        dto.setApplyReasonCode(afterSaleInfo.getApplyReasonCode());
        dto.setApplyReason(afterSaleInfo.getApplyReason());
        dto.setReviewTime(afterSaleInfo.getReviewTime());
        dto.setReviewSource(afterSaleInfo.getReviewSource());
        dto.setReviewReasonCode(afterSaleInfo.getReviewReasonCode());
        dto.setReviewReason(afterSaleInfo.getReviewReason());
        dto.setAfterSaleType(afterSaleInfo.getAfterSaleType());
        dto.setAfterSaleTypeDetail(afterSaleInfo.getAfterSaleTypeDetail());
        dto.setAfterSaleStatus(afterSaleInfo.getAfterSaleStatus());
        dto.setApplyRefundAmount(afterSaleInfo.getApplyRefundAmount());
        dto.setRealRefundAmount(afterSaleInfo.getRealRefundAmount());
        dto.setRemark(afterSaleInfo.getRemark());

        // 3. 查询售后单条目
        List<AfterSaleItemDO> afterSaleItems = afterSaleItemMapper.selectListByAfterSaleId(afterSaleId);
        List<AfterSaleItemDTO> afterSaleItemDTOList = new ArrayList<>();
        for (AfterSaleItemDO afterSaleItem : afterSaleItems) {
            AfterSaleItemDTO afterSaleItemDTO = new AfterSaleItemDTO();
            afterSaleItemDTO.setAfterSaleId(afterSaleItem.getAfterSaleId());
            afterSaleItemDTO.setOrderId(afterSaleItem.getOrderId());
            afterSaleItemDTO.setSkuCode(afterSaleItem.getSkuCode());
            afterSaleItemDTO.setProductName(afterSaleItem.getProductName());
            afterSaleItemDTO.setProductImg(afterSaleItem.getProductImg());
            afterSaleItemDTO.setReturnQuantity(afterSaleItem.getReturnQuantity());
            afterSaleItemDTO.setOriginAmount(afterSaleItem.getOriginAmount());
            afterSaleItemDTO.setApplyRefundAmount(afterSaleItem.getApplyRefundAmount());
            afterSaleItemDTO.setRealRefundAmount(afterSaleItem.getRealRefundAmount());

        }
        dto.setAfterSaleItems(afterSaleItemDTOList);

        // 4. 查询售后支付信息
        AfterSaleRefundDO afterSalePays = afterSaleRefundMapper.selectOneByAfterSaleId(afterSaleId);
        AfterSaleRefundDTO afterSalePayDTO = new AfterSaleRefundDTO();
        afterSalePayDTO.setAfterSaleRefundId(afterSalePays.getAfterSaleRefundId());
        afterSalePayDTO.setAfterSaleId(afterSalePays.getAfterSaleId());
        afterSalePayDTO.setOrderId(afterSalePays.getOrderId());
        afterSalePayDTO.setAfterSaleBatchNo(afterSalePays.getAfterSaleBatchNo());
        afterSalePayDTO.setAccountType(afterSalePays.getAccountType());
        afterSalePayDTO.setPayType(afterSalePays.getPayType());
        afterSalePayDTO.setRefundStatus(afterSalePays.getRefundStatus());
        afterSalePayDTO.setRefundAmount(afterSalePays.getRefundAmount());
        afterSalePayDTO.setRefundPayTime(afterSalePays.getRefundPayTime());
        afterSalePayDTO.setOutTradeNo(afterSalePays.getOutTradeNo());
        afterSalePayDTO.setRemark(afterSalePays.getRemark());

        // 5. 查询售后日志
        List<AfterSaleLogDO> afterSaleLogs = afterSaleLogMapper.selectListByAfterSaleId(afterSaleId);
        List<AfterSaleOrderDetailDTO.AfterSaleLogDTO> afterSaleLogDTOList = new ArrayList<>();
        for (AfterSaleLogDO afterSaleLog : afterSaleLogs) {
            AfterSaleOrderDetailDTO.AfterSaleLogDTO afterSaleLogDTO = new AfterSaleOrderDetailDTO.AfterSaleLogDTO();
            afterSaleLogDTO.setAfterSaleId(afterSaleLog.getAfterSaleId());
            afterSaleLogDTO.setPreStatus(afterSaleLog.getPreStatus());
            afterSaleLogDTO.setCurrentStatus(afterSaleLog.getCurrentStatus());
            afterSaleLogDTO.setRemark(afterSaleLog.getRemark());

        }
        dto.setAfterSaleLogs(afterSaleLogDTOList);
        // 6. 构造返参

        return dto;
    }

    /**
     * 查询缺品信息
     */
    public List<OrderLackItemDTO> getOrderLackItemInfo(String orderId) {
        List<AfterSaleInfoDO> afterSaleInfo = afterSaleInfoMapper.selectListByOrderIdAndAfterSaleTypeDetails(orderId, Arrays.asList(AfterSaleTypeDetailEnum.LACK_REFUND.getCode()));
        if (CollectionUtils.isEmpty(afterSaleInfo)) {
            return null;
        }

        List<OrderLackItemDTO> lackItems = new ArrayList<>();

        for (AfterSaleInfoDO lackItem : afterSaleInfo) {
            AfterSaleOrderDetailDTO detailDTO = afterSaleDetail(lackItem.getAfterSaleId());
            OrderLackItemDTO itemDTO = new OrderLackItemDTO();
            BeanUtils.copyProperties(detailDTO, itemDTO);
            lackItems.add(itemDTO);
        }

        return lackItems;
    }
}
