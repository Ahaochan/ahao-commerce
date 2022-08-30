package com.ruyuan.eshop.order.statemachine.action.order.create.node;

import cn.hutool.json.JSONUtil;
import com.ruyuan.eshop.address.domain.dto.AddressDTO;
import com.ruyuan.eshop.address.domain.query.AddressQuery;
import com.ruyuan.eshop.common.utils.JsonUtil;
import com.ruyuan.eshop.market.domain.dto.CalculateOrderAmountDTO;
import com.ruyuan.eshop.market.domain.dto.UserCouponDTO;
import com.ruyuan.eshop.market.domain.query.UserCouponQuery;
import com.ruyuan.eshop.order.builder.FullOrderData;
import com.ruyuan.eshop.order.builder.NewOrderBuilder;
import com.ruyuan.eshop.order.config.OrderProperties;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.OrderSnapshotDAO;
import com.ruyuan.eshop.order.dao.OrderSnapshotDAOUtils;
import com.ruyuan.eshop.order.domain.entity.*;
import com.ruyuan.eshop.order.domain.request.CreateOrderRequest;
import com.ruyuan.eshop.order.enums.SnapshotTypeEnum;
import com.ruyuan.eshop.order.hbase.entity.OrderInfoExtJsonDTO;
import com.ruyuan.eshop.order.remote.AddressRemote;
import com.ruyuan.eshop.order.remote.MarketRemote;
import com.ruyuan.eshop.order.service.impl.NewOrderDataHolder;
import com.ruyuan.eshop.product.domain.dto.ProductSkuDTO;
import com.ruyuan.process.engine.process.ProcessContext;
import com.ruyuan.process.engine.process.StandardProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 创建订单构建主订单节点
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
public class CreateOrderMasterBuilderNode extends StandardProcessor {

    @Autowired
    private OrderProperties orderProperties;

    @Autowired
    private OrderConverter orderConverter;

    @Autowired
    private AddressRemote addressRemote;

    @Autowired
    private MarketRemote marketRemote;

    @Override
    protected void processInternal(ProcessContext processContext) {
        CreateOrderRequest createOrderRequest = processContext.get("createOrderRequest");
        List<ProductSkuDTO> productSkuList = processContext.get("productSkuList");
        CalculateOrderAmountDTO calculateOrderAmountDTO = processContext.get("calculateOrderAmountDTO");

        // 1、对sku按照商品类型进行分组
        Map<Integer, List<ProductSkuDTO>> productTypeMap = productSkuList.stream()
                .collect(Collectors.groupingBy(ProductSkuDTO::getProductType));

        // 2、生成主订单
        FullOrderData fullMasterOrderData = addNewMasterOrder(createOrderRequest, productSkuList, productTypeMap,
                calculateOrderAmountDTO);

        // 3、封装主订单数据到NewOrderData对象中
        NewOrderDataHolder newOrderDataHolder = new NewOrderDataHolder();

        newOrderDataHolder.appendOrderData(fullMasterOrderData);

        processContext.set("fullMasterOrderData", fullMasterOrderData);
        processContext.set("newOrderDataHolder", newOrderDataHolder);
        processContext.set("productTypeSet", productTypeMap.keySet());
    }

    /**
     * 新增主订单信息订单
     */
    private FullOrderData addNewMasterOrder(CreateOrderRequest createOrderRequest, List<ProductSkuDTO> productSkuList,
                                            Map<Integer, List<ProductSkuDTO>> productTypeMap,
                                            CalculateOrderAmountDTO calculateOrderAmountDTO) {
        NewOrderBuilder newOrderBuilder = new NewOrderBuilder(createOrderRequest, productSkuList, productTypeMap,
                calculateOrderAmountDTO, orderProperties, orderConverter);
        FullOrderData fullOrderData = newOrderBuilder.buildOrder()
                .setOrderType()
                .buildOrderItems()
                .addPreSaleInfoToOrderItems()
                .buildOrderDeliveryDetail()
                .buildOrderPaymentDetail()
                .buildOrderAmount()
                .buildOrderAmountDetail()
                .buildOperateLog()
                .buildOrderSnapshot()
                .build();

        // 订单信息
        OrderInfoDO orderInfoDO = fullOrderData.getOrderInfoDO();

        // 订单条目信息
        List<OrderItemDO> orderItemDOList = fullOrderData.getOrderItemDOList();

        // 订单费用信息
        List<OrderAmountDO> orderAmountDOList = fullOrderData.getOrderAmountDOList();

        // 补全地址信息
        OrderDeliveryDetailDO orderDeliveryDetailDO = fullOrderData.getOrderDeliveryDetailDO();
        String detailAddress = getDetailAddress(orderDeliveryDetailDO);
        orderDeliveryDetailDO.setDetailAddress(detailAddress);

        // 补全订单状态变更日志
        OrderOperateLogDO orderOperateLogDO = fullOrderData.getOrderOperateLogDO();
        String remark = "创建订单操作0-10";
        orderOperateLogDO.setRemark(remark);

        // 补全订单商品快照信息
        List<OrderSnapshotDO> orderSnapshotDOList = fullOrderData.getOrderSnapshotDOList();
        for (OrderSnapshotDO orderSnapshotDO : orderSnapshotDOList) {
            // 优惠券信息
            if (orderSnapshotDO.getSnapshotType().equals(SnapshotTypeEnum.ORDER_COUPON.getCode())) {
                String couponId = orderInfoDO.getCouponId();
                String userId = orderInfoDO.getUserId();
                UserCouponQuery userCouponQuery = new UserCouponQuery();
                userCouponQuery.setCouponId(couponId);
                userCouponQuery.setUserId(userId);
                UserCouponDTO userCouponDTO = marketRemote.getUserCoupon(userCouponQuery);
                if (userCouponDTO != null) {
                    orderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(userCouponDTO));
                } else {
                    orderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(couponId));
                }
            }
            // 订单费用信息
            else if (orderSnapshotDO.getSnapshotType().equals(SnapshotTypeEnum.ORDER_AMOUNT.getCode())) {
                orderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(orderAmountDOList));
            }
            // 订单条目信息
            else if (orderSnapshotDO.getSnapshotType().equals(SnapshotTypeEnum.ORDER_ITEM.getCode())) {
                orderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(orderItemDOList));
            }
        }

        // 预分配主单快照在HBASE中的rowKey前缀
        OrderInfoExtJsonDTO orderInfoExtJsonDTO = new OrderInfoExtJsonDTO();
        orderInfoExtJsonDTO.setOrderSnapshotRowKeyPrefixList(OrderSnapshotDAOUtils.batchGenerateRowKey(orderSnapshotDOList.size()));
        orderInfoDO.setExtJson(JSONUtil.toJsonStr(orderInfoExtJsonDTO));

        fullOrderData.setOrderInfoDO(orderInfoDO);

        return fullOrderData;
    }

    /**
     * 获取用户收货详细地址
     */
    private String getDetailAddress(OrderDeliveryDetailDO orderDeliveryDetailDO) {
        String provinceCode = orderDeliveryDetailDO.getProvince();
        String cityCode = orderDeliveryDetailDO.getCity();
        String areaCode = orderDeliveryDetailDO.getArea();
        String streetCode = orderDeliveryDetailDO.getStreet();
        AddressQuery query = new AddressQuery();
        query.setProvinceCode(provinceCode);
        query.setCityCode(cityCode);
        query.setAreaCode(areaCode);
        query.setStreetCode(streetCode);
        AddressDTO addressDTO = addressRemote.queryAddress(query);
        if (addressDTO == null) {
            return orderDeliveryDetailDO.getDetailAddress();
        }

        StringBuilder detailAddress = new StringBuilder();
        if (StringUtils.isNotEmpty(addressDTO.getProvince())) {
            detailAddress.append(addressDTO.getProvince());
        }
        if (StringUtils.isNotEmpty(addressDTO.getCity())) {
            detailAddress.append(addressDTO.getCity());
        }
        if (StringUtils.isNotEmpty(addressDTO.getArea())) {
            detailAddress.append(addressDTO.getArea());
        }
        if (StringUtils.isNotEmpty(addressDTO.getStreet())) {
            detailAddress.append(addressDTO.getStreet());
        }
        if (StringUtils.isNotEmpty(orderDeliveryDetailDO.getDetailAddress())) {
            detailAddress.append(orderDeliveryDetailDO.getDetailAddress());
        }
        return detailAddress.toString();
    }


}
