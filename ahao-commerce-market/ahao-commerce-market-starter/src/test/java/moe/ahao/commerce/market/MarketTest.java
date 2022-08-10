package moe.ahao.commerce.market;

import moe.ahao.commerce.common.enums.AmountTypeEnum;
import moe.ahao.commerce.market.api.dto.CalculateOrderAmountDTO;
import moe.ahao.commerce.market.api.query.CalculateOrderAmountQuery;
import moe.ahao.commerce.market.application.MarketCalculateAppService;
import moe.ahao.embedded.RedisExtension;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = MarketApplication.class)
@ActiveProfiles("test")

@EnableAutoConfiguration(exclude = RocketMQAutoConfiguration.class)
public class MarketTest {
    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();
    @Autowired
    private MarketCalculateAppService marketCalculateService;

    String skuCode1 = "10101010";
    String skuCode2 = "10101011";
    BigDecimal salePrice1 = new BigDecimal("1000");
    BigDecimal saleQuantity1 = new BigDecimal("2");
    BigDecimal salePrice2 = new BigDecimal("100");
    BigDecimal saleQuantity2 = new BigDecimal("3");
    BigDecimal originPayAmount = new BigDecimal("2300");
    BigDecimal couponDiscountAmount = new BigDecimal("500");
    BigDecimal realPayAmount = new BigDecimal("1800");
    @Test
    public void calculateOrderAmount() {
        // 1. 营销计算分摊金额
        CalculateOrderAmountQuery query = new CalculateOrderAmountQuery();
        query.setOrderId("1021121945762025110");
        query.setSellerId("101");
        query.setUserId("100");
        query.setCouponId("1001001");
        query.setRegionId("区域编号");
        query.setOrderItemList(Arrays.asList(
            new CalculateOrderAmountQuery.OrderItem("1001010", 1, skuCode1, salePrice1, saleQuantity1),
            new CalculateOrderAmountQuery.OrderItem("1001011", 1, skuCode2, salePrice2, saleQuantity2)
        ));
        query.setOrderAmountList(Arrays.asList(
            new CalculateOrderAmountQuery.OrderAmount(AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode(), originPayAmount),
            new CalculateOrderAmountQuery.OrderAmount(AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode(), couponDiscountAmount),
            new CalculateOrderAmountQuery.OrderAmount(AmountTypeEnum.REAL_PAY_AMOUNT.getCode(), realPayAmount)
        ));
        CalculateOrderAmountDTO calculateOrderAmountDTO = marketCalculateService.calculateOrderAmount(query);
        System.out.println(JSONHelper.toString(calculateOrderAmountDTO));

        // 2. 订单金额校验
        this.assertAmount(calculateOrderAmountDTO);

        // 3. 订单条目金额校验
        this.assertItemAmount(calculateOrderAmountDTO);
    }


    private void assertAmount(CalculateOrderAmountDTO dto) {
        // 1. 对计算出来的金额求和
        Map<Integer, BigDecimal> calculateAmountMap = new HashMap<>();
        for (CalculateOrderAmountDTO.OrderAmountDTO orderAmountDTO : dto.getOrderAmountList()) {
            Integer amountType = orderAmountDTO.getAmountType();

            BigDecimal amount = calculateAmountMap.getOrDefault(amountType, BigDecimal.ZERO);
            calculateAmountMap.put(amountType, amount.add(orderAmountDTO.getAmount()));
        }

        // 2. 对原金额比较
        Assertions.assertEquals(0, originPayAmount.compareTo(calculateAmountMap.get(AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode())));
        Assertions.assertEquals(0, couponDiscountAmount.compareTo(calculateAmountMap.get(AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode())));
        Assertions.assertEquals(0, realPayAmount.compareTo(calculateAmountMap.get(AmountTypeEnum.REAL_PAY_AMOUNT.getCode())));
    }

    private void assertItemAmount(CalculateOrderAmountDTO dto) {
        // 1. 对计算出来的金额求和
        Map<String, Map<Integer, BigDecimal>> calculateItemAmountMap1 = new HashMap</* <SkuCode, <AmountType, Amount>> */>();
        Map<Integer, BigDecimal> calculateItemAmountMap2 = new HashMap</* <AmountType, Amount> */>();
        for (CalculateOrderAmountDTO.OrderItemAmountDTO orderItemAmountDTO : dto.getOrderItemAmountList()) {
            String skuCode = orderItemAmountDTO.getSkuCode();
            Integer amountType = orderItemAmountDTO.getAmountType();

            Map<Integer, BigDecimal> amountMap = calculateItemAmountMap1.computeIfAbsent(skuCode, k -> new HashMap<>());

            BigDecimal amount = amountMap.getOrDefault(amountType, BigDecimal.ZERO);
            amountMap.put(amountType, amount.add(orderItemAmountDTO.getAmount()));
        }
        for (CalculateOrderAmountDTO.OrderItemAmountDTO orderItemAmountDTO : dto.getOrderItemAmountList()) {
            Integer amountType = orderItemAmountDTO.getAmountType();

            BigDecimal amount = calculateItemAmountMap2.getOrDefault(amountType, BigDecimal.ZERO);
            calculateItemAmountMap2.put(amountType, amount.add(orderItemAmountDTO.getAmount()));
        }
        // 2. 对订单金额进行校验
        Assertions.assertEquals(0, salePrice1.multiply(saleQuantity1).compareTo(calculateItemAmountMap1.get(skuCode1).get(AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode())));
        Assertions.assertEquals(0, salePrice2.multiply(saleQuantity2).compareTo(calculateItemAmountMap1.get(skuCode2).get(AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode())));

        // 3. 对订单条目金额进行校验
        Assertions.assertEquals(0, originPayAmount.compareTo(calculateItemAmountMap2.get(AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode())));
        Assertions.assertEquals(0, couponDiscountAmount.compareTo(calculateItemAmountMap2.get(AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode())));
        Assertions.assertEquals(0, realPayAmount.compareTo(calculateItemAmountMap2.get(AmountTypeEnum.REAL_PAY_AMOUNT.getCode())));
    }
}
