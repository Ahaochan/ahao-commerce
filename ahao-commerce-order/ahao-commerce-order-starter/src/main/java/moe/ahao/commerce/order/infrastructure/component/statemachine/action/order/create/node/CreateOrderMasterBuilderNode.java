package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.create.node;

import moe.ahao.commerce.address.api.dto.AddressFullDTO;
import moe.ahao.commerce.address.api.query.AddressQuery;
import moe.ahao.commerce.market.api.dto.CalculateOrderAmountDTO;
import moe.ahao.commerce.market.api.dto.UserCouponDTO;
import moe.ahao.commerce.market.api.query.GetUserCouponQuery;
import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.application.GenOrderIdAppService;
import moe.ahao.commerce.order.infrastructure.component.OrderDataBuilder;
import moe.ahao.commerce.order.infrastructure.config.OrderProperties;
import moe.ahao.commerce.order.infrastructure.enums.SnapshotTypeEnum;
import moe.ahao.commerce.order.infrastructure.gateway.AddressGateway;
import moe.ahao.commerce.order.infrastructure.gateway.CouponGateway;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderDeliveryDetailDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderItemDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.hbase.data.OrderSnapshotDO;
import moe.ahao.commerce.product.api.dto.ProductSkuDTO;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.StandardProcessor;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 创建订单构建主订单节点
 */
@Component
public class CreateOrderMasterBuilderNode extends StandardProcessor {
    @Autowired
    private OrderProperties orderProperties;
    @Autowired
    private GenOrderIdAppService genOrderIdAppService;

    @Autowired
    private AddressGateway addressGateway;
    @Autowired
    private CouponGateway couponGateway;

    @Override
    protected void processInternal(ProcessContext processContext) {
        CreateOrderCommand command = processContext.get("createOrderCommand");
        List<ProductSkuDTO> productSkuList = processContext.get("productSkuList");
        CalculateOrderAmountDTO calculateOrderAmountDTO = processContext.get("calculateOrderAmountDTO");

        // 3. 生成数据库实体数据
        OrderDataBuilder.OrderData orderData = new OrderDataBuilder(command, productSkuList, calculateOrderAmountDTO, orderProperties).build();

        // 补全地址信息
        OrderDeliveryDetailDO orderDeliveryDetailDO = orderData.getOrderDeliveryDetail();
        String detailAddress = this.getDetailAddress(orderDeliveryDetailDO);
        orderDeliveryDetailDO.setDetailAddress(detailAddress);

        // 补全订单商品快照信息
        OrderInfoDO orderInfo = orderData.getOrderInfo();
        List<OrderSnapshotDO> orderSnapshotList = orderData.getOrderSnapshotList();
        for (OrderSnapshotDO orderSnapshot : orderSnapshotList) {
            // 优惠券信息
            if (orderSnapshot.getSnapshotType().equals(SnapshotTypeEnum.ORDER_COUPON.getCode())) {
                GetUserCouponQuery getUserCouponQuery = new GetUserCouponQuery();
                getUserCouponQuery.setCouponId(orderInfo.getCouponId());
                getUserCouponQuery.setUserId(orderInfo.getUserId());
                UserCouponDTO userCouponDTO = couponGateway.get(getUserCouponQuery);

                if (userCouponDTO != null) {
                    orderSnapshot.setSnapshotJson(JSONHelper.toString(userCouponDTO));
                } else {
                    orderSnapshot.setSnapshotJson("{\"couponId\": \"" + orderInfo.getCouponId() + "\"}");
                }
            }
        }

        // 4. 拆单
        List<OrderDataBuilder.OrderData> subOrderDataList = orderData.split(list ->
            new ArrayList<>(list.stream()
                .collect(Collectors.groupingBy(OrderItemDO::getProductType, Collectors.toList()))
                .values()), genOrderIdAppService);
        List<OrderDataBuilder.OrderData> allOrderDataList = Stream.of(subOrderDataList, Collections.singletonList(orderData))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        processContext.set("orderData", orderData);
        processContext.set("subOrderDataList", subOrderDataList);
        processContext.set("allOrderDataList", allOrderDataList);
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
        AddressFullDTO addressDTO = addressGateway.queryAddress(query);
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
