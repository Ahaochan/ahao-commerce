package moe.ahao.commerce.order.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.dto.OrderLackItemDTO;
import moe.ahao.commerce.aftersale.application.AfterSaleQueryService;
import moe.ahao.commerce.aftersale.application.OrderLackAppService;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.api.dto.OrderDetailDTO;
import moe.ahao.commerce.order.api.dto.OrderListDTO;
import moe.ahao.commerce.order.api.query.OrderPageQuery;
import moe.ahao.commerce.common.enums.BusinessIdentifierEnum;
import moe.ahao.commerce.common.enums.OrderTypeEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.OrderOperateLogRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.hbase.OrderSnapshotRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.OrderListEsRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data.EsOrderListQueryDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.hbase.data.OrderSnapshotDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.data.OrderOperateLogDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.*;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.*;
import moe.ahao.domain.entity.PagingInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderQueryService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderAmountDetailMapper orderAmountDetailMapper;
    @Autowired
    private OrderDeliveryDetailMapper orderDeliveryDetailMapper;
    @Autowired
    private OrderPaymentDetailMapper orderPaymentDetailMapper;
    @Autowired
    private OrderSnapshotRepository orderSnapshotRepository;
    @Autowired
    private OrderAmountMapper orderAmountMapper;
    @Autowired
    private OrderOperateLogRepository orderOperateLogRepository;
    @Autowired
    private AfterSaleQueryService afterSaleQueryService;
    @Autowired
    private OrderLackAppService orderLackService;

    @Autowired
    private OrderListEsRepository orderListEsRepository;

    public PagingInfo<OrderListDTO> queryV1(OrderPageQuery query) {
        // 1. 参数校验
        this.checkQueryParam(query);

        // 第一阶段采用很low的连表查询，连接5张表，即使加索引，只要数据量稍微大一点查询性能就很低了
        // 第二阶段会接入es，优化这块的查询性能

        // 1. 组装业务查询规则
        if (CollectionUtils.isEmpty(query.getOrderStatus())) {
            // 默认不展示无效订单
            query.setOrderStatus(OrderStatusEnum.validStatus());
        }
        // 2. 查询
        Page<OrderListDTO> page = orderInfoMapper.selectPage(new Page<>(query.getPageNo(), query.getPageSize()), query);

        // 3. 转化
        PagingInfo<OrderListDTO> pagingInfo = new PagingInfo<>();
        pagingInfo.setPageNo((int) page.getCurrent());
        pagingInfo.setPageSize((int) page.getSize());
        pagingInfo.setTotal(page.getTotal());
        pagingInfo.setList(page.getRecords());
        return pagingInfo;
    }

    public PagingInfo<OrderDetailDTO> queryV2(OrderPageQuery query) {
        // 1. 参数校验
        this.checkQueryParam(query);

        // 2. 在ES查询
        SearchHits<EsOrderListQueryDO> hits = orderListEsRepository.query(query);

        // 3. 转化
        List<OrderDetailDTO> orderDetailList = new ArrayList<>();
        for (SearchHit<EsOrderListQueryDO> hit : hits) {
            EsOrderListQueryDO content = hit.getContent();
            String orderId = content.getOrderId();

            OrderDetailDTO orderDetail = this.orderDetail(orderId);
            orderDetailList.add(orderDetail);
        }
        PagingInfo<OrderDetailDTO> pagingInfo = new PagingInfo<>();
        pagingInfo.setPageNo(query.getPageNo());
        pagingInfo.setPageSize(query.getPageSize());
        pagingInfo.setTotal(hits.getTotalHits());
        pagingInfo.setList(orderDetailList);
        return pagingInfo;
    }

    private void checkQueryParam(OrderPageQuery query) {
        if (query == null) {
            throw OrderExceptionEnum.BUSINESS_IDENTIFIER_IS_NULL.msg();
        }
        Integer businessIdentifier = query.getBusinessIdentifier();
        if (businessIdentifier != null && !BusinessIdentifierEnum.allowableValues().contains(businessIdentifier)) {
            throw OrderExceptionEnum.BUSINESS_IDENTIFIER_ERROR.msg();
        }
        Set<Integer> orderTypes = query.getOrderTypes();
        if (CollectionUtils.isNotEmpty(orderTypes) && !OrderTypeEnum.allowableValues().containsAll(orderTypes)) {
            throw OrderExceptionEnum.ORDER_TYPE_ERROR.msg();
        }
        Set<Integer> orderStatus = query.getOrderStatus();
        if (CollectionUtils.isNotEmpty(orderStatus) && !OrderStatusEnum.allowableValues().containsAll(orderStatus)) {
            throw OrderExceptionEnum.ORDER_TYPE_ERROR.msg();
        }

        int maxSize = OrderPageQuery.MAX_PAGE_SIZE;
        Set<String> orderIds = query.getOrderIds();
        if (CollectionUtils.isNotEmpty(orderIds) && orderIds.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("orderIds", maxSize);
        }
        Set<String> sellerIds = query.getSellerIds();
        if (CollectionUtils.isNotEmpty(sellerIds) && sellerIds.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("sellerIds", maxSize);
        }
        Set<String> parentOrderIds = query.getParentOrderIds();
        if (CollectionUtils.isNotEmpty(parentOrderIds) && parentOrderIds.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("parentOrderIds", maxSize);
        }
        Set<String> receiverNames = query.getReceiverNames();
        if (CollectionUtils.isNotEmpty(receiverNames) && receiverNames.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("receiverNames", maxSize);
        }
        Set<String> receiverPhones = query.getReceiverPhones();
        if (CollectionUtils.isNotEmpty(receiverPhones) && receiverPhones.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("receiverPhones", maxSize);
        }
        Set<String> tradeNos = query.getTradeNos();
        if (CollectionUtils.isNotEmpty(tradeNos) && tradeNos.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("tradeNos", maxSize);
        }

        Set<String> userIds = query.getUserIds();
        if (CollectionUtils.isNotEmpty(userIds) && userIds.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("userIds", maxSize);
        }

        Set<String> skuCodes = query.getSkuCodes();
        if (CollectionUtils.isNotEmpty(skuCodes) && skuCodes.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("skuCodes", maxSize);
        }

        Set<String> productNames = query.getProductNames();
        if (CollectionUtils.isNotEmpty(productNames) && productNames.size() > maxSize) {
            throw OrderExceptionEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE.msg("productNames", maxSize);
        }
    }

    public OrderDetailDTO orderDetail(String orderId) {
        // 1. 查询订单
        if (StringUtils.isEmpty(orderId)) {
            throw null;
        }
        OrderInfoDO orderInfo = orderInfoMapper.selectOneByOrderId(orderId);
        if (orderInfo == null) {
            return null;
        }
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setBusinessIdentifier(orderInfo.getBusinessIdentifier());
        dto.setOrderId(orderInfo.getOrderId());
        dto.setParentOrderId(orderInfo.getParentOrderId());
        dto.setBusinessOrderId(orderInfo.getBusinessOrderId());
        dto.setOrderType(orderInfo.getOrderType());
        dto.setOrderStatus(orderInfo.getOrderStatus());
        dto.setCancelType(orderInfo.getCancelType());
        dto.setCancelTime(orderInfo.getCancelTime());
        dto.setSellerId(orderInfo.getSellerId());
        dto.setUserId(orderInfo.getUserId());
        dto.setTotalAmount(orderInfo.getTotalAmount());
        dto.setPayAmount(orderInfo.getPayAmount());
        dto.setPayType(orderInfo.getPayType());
        dto.setCouponId(orderInfo.getCouponId());
        dto.setPayTime(orderInfo.getPayTime());
        dto.setExpireTime(orderInfo.getExpireTime());
        dto.setUserRemark(orderInfo.getUserRemark());
        dto.setDeleteStatus(orderInfo.getDeleteStatus());
        dto.setCommentStatus(orderInfo.getCommentStatus());
        dto.setExtJson(orderInfo.getExtJson());

        // 2. 查询订单条目
        List<OrderDetailDTO.OrderItemDTO> orderItemDTOList = this.orderItemDetail(orderId);
        dto.setOrderItems(orderItemDTOList);

        // 3. 查询订单费用明细
        List<OrderAmountDetailDO> orderAmountDetails = orderAmountDetailMapper.selectListByOrderId(orderId);
        List<OrderDetailDTO.OrderAmountDetailDTO> orderAmountDetailDTOList = new ArrayList<>();
        for (OrderAmountDetailDO orderAmountDetail : orderAmountDetails) {
            OrderDetailDTO.OrderAmountDetailDTO orderAmountDetailDTO = new OrderDetailDTO.OrderAmountDetailDTO();
            orderAmountDetailDTO.setOrderId(orderAmountDetail.getOrderId());
            orderAmountDetailDTO.setProductType(orderAmountDetail.getProductType());
            orderAmountDetailDTO.setOrderItemId(orderAmountDetail.getOrderItemId());
            orderAmountDetailDTO.setProductId(orderAmountDetail.getProductId());
            orderAmountDetailDTO.setSkuCode(orderAmountDetail.getSkuCode());
            orderAmountDetailDTO.setSaleQuantity(orderAmountDetail.getSaleQuantity());
            orderAmountDetailDTO.setSalePrice(orderAmountDetail.getSalePrice());
            orderAmountDetailDTO.setAmountType(orderAmountDetail.getAmountType());
            orderAmountDetailDTO.setAmount(orderAmountDetail.getAmount());

            orderAmountDetailDTOList.add(orderAmountDetailDTO);
        }
        dto.setOrderAmountDetails(orderAmountDetailDTOList);

        // 4. 查询订单配送信息
        OrderDeliveryDetailDO orderDeliveryDetail = orderDeliveryDetailMapper.selectOneByOrderId(orderId);
        OrderDetailDTO.OrderDeliveryDetailDTO deliveryDetailDTO = new OrderDetailDTO.OrderDeliveryDetailDTO();
        deliveryDetailDTO.setOrderId(orderDeliveryDetail.getOrderId());
        deliveryDetailDTO.setDeliveryType(orderDeliveryDetail.getDeliveryType());
        deliveryDetailDTO.setProvince(orderDeliveryDetail.getProvince());
        deliveryDetailDTO.setCity(orderDeliveryDetail.getCity());
        deliveryDetailDTO.setArea(orderDeliveryDetail.getArea());
        deliveryDetailDTO.setStreet(orderDeliveryDetail.getStreet());
        deliveryDetailDTO.setDetailAddress(orderDeliveryDetail.getDetailAddress());
        deliveryDetailDTO.setLon(orderDeliveryDetail.getLon());
        deliveryDetailDTO.setLat(orderDeliveryDetail.getLat());
        deliveryDetailDTO.setReceiverName(orderDeliveryDetail.getReceiverName());
        deliveryDetailDTO.setReceiverPhone(orderDeliveryDetail.getReceiverPhone());
        deliveryDetailDTO.setModifyAddressCount(orderDeliveryDetail.getModifyAddressCount());
        deliveryDetailDTO.setDelivererNo(orderDeliveryDetail.getDelivererNo());
        deliveryDetailDTO.setDelivererName(orderDeliveryDetail.getDelivererName());
        deliveryDetailDTO.setDelivererPhone(orderDeliveryDetail.getDelivererPhone());
        deliveryDetailDTO.setOutStockTime(orderDeliveryDetail.getOutStockTime());
        deliveryDetailDTO.setSignedTime(orderDeliveryDetail.getSignedTime());

        // 5. 查询订单支付明细
        List<OrderPaymentDetailDO> orderPaymentDetails = orderPaymentDetailMapper.selectListByOrderId(orderId);
        List<OrderDetailDTO.OrderPaymentDetailDTO> orderPaymentDetailDTOList = new ArrayList<>();
        for (OrderPaymentDetailDO orderPaymentDetail : orderPaymentDetails) {
            OrderDetailDTO.OrderPaymentDetailDTO orderPaymentDetailDTO = new OrderDetailDTO.OrderPaymentDetailDTO();
            orderPaymentDetailDTO.setOrderId(orderPaymentDetail.getOrderId());
            orderPaymentDetailDTO.setAccountType(orderPaymentDetail.getAccountType());
            orderPaymentDetailDTO.setPayType(orderPaymentDetail.getPayType());
            orderPaymentDetailDTO.setPayStatus(orderPaymentDetail.getPayStatus());
            orderPaymentDetailDTO.setPayAmount(orderPaymentDetail.getPayAmount());
            orderPaymentDetailDTO.setPayTime(orderPaymentDetail.getPayTime());
            orderPaymentDetailDTO.setOutTradeNo(orderPaymentDetail.getOutTradeNo());
            orderPaymentDetailDTO.setPayRemark(orderPaymentDetail.getPayRemark());

            orderPaymentDetailDTOList.add(orderPaymentDetailDTO);
        }
        dto.setOrderPaymentDetails(orderPaymentDetailDTOList);

        // 6. 查询订单费用类型
        List<OrderAmountDO> orderAmounts = orderAmountMapper.selectListByOrderId(orderId);
        Map<Integer, BigDecimal> orderAmountMap = orderAmounts.stream()
            .collect(Collectors.toMap(OrderAmountDO::getAmountType, OrderAmountDO::getAmount));
        dto.setOrderAmounts(orderAmountMap);

        // 7. 查询订单操作日志
        List<OrderOperateLogDO> orderOperateLogs = orderOperateLogRepository.getListByOrderId(orderId);
        List<OrderDetailDTO.OrderOperateLogDTO> orderOperateLogDTOList = new ArrayList<>();
        for (OrderOperateLogDO orderOperateLog : orderOperateLogs) {
            OrderDetailDTO.OrderOperateLogDTO orderOperateLogDTO = new OrderDetailDTO.OrderOperateLogDTO();
            orderOperateLogDTO.setOrderId(orderOperateLog.getOrderId());
            orderOperateLogDTO.setOperateType(orderOperateLog.getOperateType());
            orderOperateLogDTO.setPreStatus(orderOperateLog.getPreStatus());
            orderOperateLogDTO.setCurrentStatus(orderOperateLog.getCurrentStatus());
            orderOperateLogDTO.setRemark(orderOperateLog.getRemark());

            orderOperateLogDTOList.add(orderOperateLogDTO);
        }
        dto.setOrderOperateLogs(orderOperateLogDTOList);

        // 8. 查询订单快照
        List<OrderSnapshotDO> orderSnapshots = orderSnapshotRepository.getListByOrderId(orderId);
        List<OrderDetailDTO.OrderSnapshotDTO> orderSnapshotDTOList = new ArrayList<>();
        for (OrderSnapshotDO orderSnapshot : orderSnapshots) {
            OrderDetailDTO.OrderSnapshotDTO orderSnapshotDTO = new OrderDetailDTO.OrderSnapshotDTO();
            orderSnapshotDTO.setOrderId(orderSnapshot.getOrderId());
            orderSnapshotDTO.setSnapshotJson(orderSnapshot.getSnapshotJson());

            orderSnapshotDTOList.add(orderSnapshotDTO);
        }
        dto.setOrderSnapshots(orderSnapshotDTOList);

        // 9. 查询缺品退款信息
        if (orderLackService.isOrderLacked(orderInfo)) {
            List<OrderLackItemDTO> lackItems = afterSaleQueryService.getOrderLackItemInfo(orderId);
            dto.setLackItems(lackItems);
        }
        return dto;
    }

    public List<OrderDetailDTO.OrderItemDTO> orderItemDetail(String orderId) {
        List<OrderItemDO> orderItems = orderItemMapper.selectListByOrderId(orderId);
        List<OrderDetailDTO.OrderItemDTO> orderItemDTOList = new ArrayList<>();
        for (OrderItemDO orderItem : orderItems) {
            OrderDetailDTO.OrderItemDTO orderItemDTO = new OrderDetailDTO.OrderItemDTO();
            orderItemDTO.setOrderId(orderItem.getOrderId());
            orderItemDTO.setOrderItemId(orderItem.getOrderItemId());
            orderItemDTO.setProductType(orderItem.getProductType());
            orderItemDTO.setProductId(orderItem.getProductId());
            orderItemDTO.setProductImg(orderItem.getProductImg());
            orderItemDTO.setProductName(orderItem.getProductName());
            orderItemDTO.setSkuCode(orderItem.getSkuCode());
            orderItemDTO.setSaleQuantity(orderItem.getSaleQuantity());
            orderItemDTO.setSalePrice(orderItem.getSalePrice());
            orderItemDTO.setOriginAmount(orderItem.getOriginAmount());
            orderItemDTO.setPayAmount(orderItem.getPayAmount());
            orderItemDTO.setProductUnit(orderItem.getProductUnit());
            orderItemDTO.setPurchasePrice(orderItem.getPurchasePrice());
            orderItemDTO.setSellerId(orderItem.getSellerId());

            orderItemDTOList.add(orderItemDTO);
        }
        return orderItemDTOList;
    }
}
