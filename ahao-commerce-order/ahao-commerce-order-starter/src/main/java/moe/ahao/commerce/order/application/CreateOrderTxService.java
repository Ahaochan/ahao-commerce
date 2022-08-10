package moe.ahao.commerce.order.application;


import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.inventory.api.command.DeductProductStockCommand;
import moe.ahao.commerce.market.api.command.LockUserCouponCommand;
import moe.ahao.commerce.market.api.dto.CalculateOrderAmountDTO;
import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.infrastructure.component.OrderDataBuilder;
import moe.ahao.commerce.order.infrastructure.config.OrderProperties;
import moe.ahao.commerce.order.infrastructure.gateway.CouponGateway;
import moe.ahao.commerce.order.infrastructure.gateway.InventoryGateway;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.OrderOperateLogRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.hbase.OrderSnapshotRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.hbase.data.OrderSnapshotDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.data.OrderOperateLogDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.*;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.service.*;
import moe.ahao.commerce.product.api.dto.ProductSkuDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class CreateOrderTxService {
    @Autowired
    private OrderProperties orderProperties;

    @Autowired
    private GenOrderIdAppService genOrderIdAppService;

    @Autowired
    private CouponGateway couponGateway;
    @Autowired
    private InventoryGateway inventoryGateway;

    @Autowired
    private OrderAmountDetailMybatisService orderAmountDetailMybatisService;
    @Autowired
    private OrderAmountMybatisService orderAmountMybatisService;
    @Autowired
    private OrderDeliveryDetailMybatisService orderDeliveryDetailMybatisService;
    @Autowired
    private OrderInfoMybatisService orderInfoMybatisService;
    @Autowired
    private OrderItemMybatisService orderItemMybatisService;
    @Autowired
    private OrderOperateLogRepository orderOperateLogRepository;
    @Autowired
    private OrderPaymentDetailMybatisService orderPaymentDetailMybatisService;
    @Autowired
    private OrderSnapshotRepository orderSnapshotRepository;

    // 去除了seata AT事务
    // @GlobalTransactional(rollbackFor = Exception.class)
    public String addNewOrder(CreateOrderCommand command, List<ProductSkuDTO> productList, CalculateOrderAmountDTO calculateOrderAmountDTO) {
        // 1. 锁定优惠券
        this.lockUserCoupon(command.getOrderId(), command.getUserId(), command.getCouponId());
        // 2. 扣减库存
        this.deductProductStock(command.getOrderId(), command.getOrderItems());

        // 3. 生成数据库实体数据
        OrderDataBuilder.OrderData orderData = new OrderDataBuilder(command, productList, calculateOrderAmountDTO, orderProperties).build();
        // 如果存在多种商品类型，需要按商品类型进行拆单
        List<OrderDataBuilder.OrderData> subOrderDataList = orderData.split(list ->
            new ArrayList<>(list.stream()
                .collect(Collectors.groupingBy(OrderItemDO::getProductType, Collectors.toList()))
                .values()), genOrderIdAppService);
        List<OrderDataBuilder.OrderData> allOrderDataList = Stream.of(subOrderDataList, Collections.singletonList(orderData))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        // 4. 保存订单到数据库
        // 订单信息
        List<OrderInfoDO> orderInfoList = allOrderDataList.stream()
            .map(OrderDataBuilder.OrderData::getOrderInfo)
            .collect(Collectors.toList());
        if (!orderInfoList.isEmpty()) {
            orderInfoMybatisService.saveBatch(orderInfoList);
        }

        // 订单条目
        List<OrderItemDO> orderItemDOList = allOrderDataList.stream()
            .map(OrderDataBuilder.OrderData::getOrderItemList)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        if (!orderItemDOList.isEmpty()) {
            orderItemMybatisService.saveBatch(orderItemDOList);
        }

        // 订单配送信息
        List<OrderDeliveryDetailDO> orderDeliveryDetailDOList = allOrderDataList.stream()
            .map(OrderDataBuilder.OrderData::getOrderDeliveryDetail)
            .collect(Collectors.toList());
        if (!orderDeliveryDetailDOList.isEmpty()) {
            orderDeliveryDetailMybatisService.saveBatch(orderDeliveryDetailDOList);
        }

        // 订单支付信息
        List<OrderPaymentDetailDO> orderPaymentDetailDOList = allOrderDataList.stream()
            .map(OrderDataBuilder.OrderData::getOrderPaymentDetailList)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        if (!orderPaymentDetailDOList.isEmpty()) {
            orderPaymentDetailMybatisService.saveBatch(orderPaymentDetailDOList);
        }

        // 订单费用信息
        List<OrderAmountDO> orderAmountDOList = allOrderDataList.stream()
            .map(OrderDataBuilder.OrderData::getOrderAmountList)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        if (!orderAmountDOList.isEmpty()) {
            orderAmountMybatisService.saveBatch(orderAmountDOList);
        }

        // 订单费用明细
        List<OrderAmountDetailDO> orderAmountDetailDOList = allOrderDataList.stream()
            .map(OrderDataBuilder.OrderData::getOrderAmountDetailList)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        if (!orderAmountDetailDOList.isEmpty()) {
            orderAmountDetailMybatisService.saveBatch(orderAmountDetailDOList);
        }

        // 订单状态变更日志信息
        List<OrderOperateLogDO> orderOperateLogDOList = allOrderDataList.stream()
            .map(OrderDataBuilder.OrderData::getOrderOperateLog)
            .collect(Collectors.toList());
        if (!orderOperateLogDOList.isEmpty()) {
            orderOperateLogRepository.saveBatch(orderOperateLogDOList);
        }

        // 订单快照数据
        List<OrderSnapshotDO> orderSnapshotDOList = allOrderDataList.stream()
            .map(OrderDataBuilder.OrderData::getOrderSnapshotList)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        if (!orderSnapshotDOList.isEmpty()) {
            orderSnapshotRepository.saveBatch(orderSnapshotDOList);
        }

        return command.getOrderId();
    }

    private void lockUserCoupon(String orderNo, String userNo, String couponNo) {
        if (StringUtils.isEmpty(couponNo)) {
            log.info("下单流程, 用户{}的订单编号{}没有使用优惠券{}, 不锁定优惠券", userNo, orderNo, couponNo);
            return;
        }

        LockUserCouponCommand command = new LockUserCouponCommand();
        command.setUserId(command.getUserId());
        command.setCouponId(command.getCouponId());
        couponGateway.lock(command);
    }

    private void deductProductStock(String orderId, List<CreateOrderCommand.OrderItem> orderItems) {
        DeductProductStockCommand command = new DeductProductStockCommand();
        command.setOrderId(orderId);

        List<DeductProductStockCommand.OrderItem> orderItemCommandList = new ArrayList<>();
        for (CreateOrderCommand.OrderItem orderItem : orderItems) {
            DeductProductStockCommand.OrderItem orderItemCommand = new DeductProductStockCommand.OrderItem();
            orderItemCommand.setSkuCode(orderItem.getSkuCode());
            orderItemCommand.setSaleQuantity(orderItem.getSaleQuantity());

            orderItemCommandList.add(orderItemCommand);
        }
        command.setOrderItems(orderItemCommandList);

        inventoryGateway.deductProductStock(command);
    }
}
