package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.create.node;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.AmountTypeEnum;
import moe.ahao.commerce.market.api.dto.CalculateOrderAmountDTO;
import moe.ahao.commerce.market.api.query.CalculateOrderAmountQuery;
import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.gateway.MarketCalculateGateway;
import moe.ahao.commerce.order.infrastructure.gateway.ProductGateway;
import moe.ahao.commerce.product.api.dto.ProductSkuDTO;
import moe.ahao.commerce.product.api.query.ListProductSkuQuery;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.StandardProcessor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 创建订单计算金额节点
 */
@Slf4j
@Component
public class CreateOrderCalculateAmountNode extends StandardProcessor {

    @Autowired
    private ProductGateway productGateway;

    @Autowired
    private MarketCalculateGateway marketCalculateGateway;

    @Override
    protected void processInternal(ProcessContext processContext) {
        CreateOrderCommand command = processContext.get("createOrderCommand");

        // 3. 获取商品信息
        List<ProductSkuDTO> productSkuList = this.listProductSku(command);

        // 4. 计算订单价格
        CalculateOrderAmountDTO calculateOrderAmountDTO = this.calculateOrderAmount(command, productSkuList);

        // 5. 验证订单实付金额
        this.checkRealPayAmount(command, calculateOrderAmountDTO);

        // 设置参数，传递给后面节点
        processContext.set("productSkuList", productSkuList);
        processContext.set("calculateOrderAmountDTO", calculateOrderAmountDTO);
    }

    /**
     * 获取订单条目商品信息
     */
    private List<ProductSkuDTO> listProductSku(CreateOrderCommand command) {
        String sellerId = command.getSellerId();
        List<String> skuCodeList = command.getOrderItems().stream()
            .map(CreateOrderCommand.OrderItem::getSkuCode)
            .collect(Collectors.toList());

        ListProductSkuQuery query = new ListProductSkuQuery();
        query.setSellerId(sellerId);
        query.setSkuCodeList(skuCodeList);
        List<ProductSkuDTO> productSkuList = productGateway.listBySkuCodes(query);
        return productSkuList;
    }

    /**
     * 计算订单价格
     * 如果使用了优惠券、红包、积分等，会一并进行扣减
     *
     * @param command        订单信息
     * @param productSkuList 商品信息
     */
    private CalculateOrderAmountDTO calculateOrderAmount(CreateOrderCommand command, List<ProductSkuDTO> productSkuList) {
        // 1. 订单条目补充商品信息
        Map<String, ProductSkuDTO> productSkuDTOMap = productSkuList.stream()
            .collect(Collectors.toMap(ProductSkuDTO::getSkuCode, Function.identity()));

        // 2. 组装营销服务计算价格请求体
        CalculateOrderAmountQuery query = new CalculateOrderAmountQuery();
        query.setOrderId(command.getOrderId());
        query.setUserId(command.getUserId());
        query.setSellerId(command.getSellerId());
        query.setCouponId(command.getCouponId());
        query.setRegionId(command.getRegionId());
        List<CalculateOrderAmountQuery.OrderItem> orderItemQueryList = new ArrayList<>();
        for (CreateOrderCommand.OrderItem orderItemCommand : command.getOrderItems()) {
            ProductSkuDTO productSkuDTO = productSkuDTOMap.get(orderItemCommand.getSkuCode());

            CalculateOrderAmountQuery.OrderItem orderItemAmountQuery = new CalculateOrderAmountQuery.OrderItem();
            orderItemAmountQuery.setProductId(productSkuDTO.getProductId());
            orderItemAmountQuery.setSkuCode(orderItemCommand.getSkuCode());
            orderItemAmountQuery.setSalePrice(productSkuDTO.getSalePrice());
            orderItemAmountQuery.setSaleQuantity(orderItemCommand.getSaleQuantity());

            orderItemQueryList.add(orderItemAmountQuery);
        }
        query.setOrderItemList(orderItemQueryList);

        List<CalculateOrderAmountQuery.OrderAmount> orderAmountQueryList = new ArrayList<>();
        for (CreateOrderCommand.OrderAmount orderAmountCommand : command.getOrderAmounts()) {

            CalculateOrderAmountQuery.OrderAmount orderAmountQuery = new CalculateOrderAmountQuery.OrderAmount();
            orderAmountQuery.setAmountType(orderAmountCommand.getAmountType());
            orderAmountQuery.setAmount(orderAmountCommand.getAmount());

            orderAmountQueryList.add(orderAmountQuery);
        }
        query.setOrderAmountList(orderAmountQueryList);

        // 3. 调用营销服务计算订单价格
        CalculateOrderAmountDTO calculateOrderAmountDTO = marketCalculateGateway.calculateOrderAmount(query);
        if (calculateOrderAmountDTO == null) {
            throw OrderExceptionEnum.CALCULATE_ORDER_AMOUNT_ERROR.msg();
        }
        // 订单费用信息
        if (CollectionUtils.isEmpty(calculateOrderAmountDTO.getOrderAmountList())) {
            throw OrderExceptionEnum.CALCULATE_ORDER_AMOUNT_ERROR.msg();
        }
        // 订单条目费用明细
        if (CollectionUtils.isEmpty(calculateOrderAmountDTO.getOrderItemAmountList())) {
            throw OrderExceptionEnum.CALCULATE_ORDER_AMOUNT_ERROR.msg();
        }
        return calculateOrderAmountDTO;
    }

    /**
     * 验证订单实付金额
     */
    private void checkRealPayAmount(CreateOrderCommand command, CalculateOrderAmountDTO calculateDTO) {
        // 前端给的实付金额
        Map<Integer, CreateOrderCommand.OrderAmount> originOrderAmountMap = command.getOrderAmounts().stream()
            .collect(Collectors.toMap(
                CreateOrderCommand.OrderAmount::getAmountType, Function.identity()));

        BigDecimal originRealPayAmount = originOrderAmountMap.get(AmountTypeEnum.REAL_PAY_AMOUNT.getCode()).getAmount();

        // 营销计算出来的实付金额
        Map<Integer, CalculateOrderAmountDTO.OrderAmountDTO> orderAmountMap = calculateDTO.getOrderAmountList().stream()
            .collect(Collectors.toMap(CalculateOrderAmountDTO.OrderAmountDTO::getAmountType, Function.identity()));
        BigDecimal realPayAmount = orderAmountMap.get(AmountTypeEnum.REAL_PAY_AMOUNT.getCode()).getAmount();

        // 订单验价失败
        if (originRealPayAmount.compareTo(realPayAmount) != 0) {
            throw OrderExceptionEnum.ORDER_CHECK_REAL_PAY_AMOUNT_FAIL.msg();
        }
    }
}
