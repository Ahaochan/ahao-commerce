package moe.ahao.commerce.order;

import moe.ahao.commerce.common.enums.AmountTypeEnum;
import moe.ahao.commerce.common.enums.PayTypeEnum;
import moe.ahao.commerce.common.enums.ProductTypeEnum;
import moe.ahao.commerce.order.api.command.CreateOrderCommand;
import moe.ahao.commerce.order.api.dto.CreateOrderDTO;
import moe.ahao.commerce.order.application.CreateOrderAppService;
import moe.ahao.commerce.order.infrastructure.enums.AccountTypeEnum;
import moe.ahao.commerce.common.enums.BusinessIdentifierEnum;
import moe.ahao.commerce.order.infrastructure.enums.DeliveryTypeEnum;
import moe.ahao.commerce.common.enums.OrderTypeEnum;
import moe.ahao.commerce.order.infrastructure.gateway.feign.ProductFeignClient;
import moe.ahao.commerce.order.infrastructure.gateway.feign.RiskFeignClient;
import moe.ahao.commerce.order.infrastructure.publisher.DefaultProducer;
import moe.ahao.commerce.product.api.query.ListProductSkuQuery;
import moe.ahao.commerce.risk.api.dto.CheckOrderRiskDTO;
import moe.ahao.domain.entity.Result;
import moe.ahao.embedded.RedisExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = OrderApplication.class)
@ActiveProfiles("test")
public class CreateOrderTest {
    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();
    @MockBean
    private DefaultProducer defaultProducer;
    @MockBean
    private RiskFeignClient riskFeignClient;
    @MockBean
    private ProductFeignClient productFeignClient;

    @Autowired
    private CreateOrderAppService createOrderAppService;

    @BeforeEach
    public void beforeEach() {
        Mockito.doNothing().when(defaultProducer).sendMessage(any(), any(), any(), any());
        Mockito.doNothing().when(defaultProducer).sendMessage(any(), any(), any(), any(), any());
        Mockito.doNothing().when(defaultProducer).sendMessage(any(), any(), any(), any(), any(), any(), any());

        CheckOrderRiskDTO checkOrderRiskDTO = new CheckOrderRiskDTO();
        checkOrderRiskDTO.setResult(true);
        checkOrderRiskDTO.setNoticeList(Collections.emptyList());
        Mockito.when(riskFeignClient.checkOrderRisk(any())).thenReturn(Result.success(checkOrderRiskDTO));

        ListProductSkuQuery listProductSkuQuery = new ListProductSkuQuery();
        // listProductSkuQuery.setSellerId();
        // listProductSkuQuery.setSkuCodeList();
        // Mockito.when(productFeignClient.listBySkuCodes(listProductSkuQuery)).thenReturn()
    }

    @Test
    public void normal() {
        CreateOrderCommand command = new CreateOrderCommand();
        command.setOrderId("1021121732891264100");
        command.setBusinessIdentifier(BusinessIdentifierEnum.SELF_MALL.getCode());
        command.setOpenid(null);
        command.setUserId("100");
        command.setOrderType(OrderTypeEnum.NORMAL.getCode());
        command.setSellerId("101");
        command.setUserRemark("test reamark");
        command.setCouponId(null);
        command.setDeliveryType(DeliveryTypeEnum.SELF.getCode());
        command.setProvince("110000");
        command.setCity("110100");
        command.setArea("110105");
        command.setStreet("110101007");
        command.setDetailAddress("北京路10号");
        command.setLon(new BigDecimal("100.1"));
        command.setLat(new BigDecimal("1010.20101"));
        command.setReceiverName("张三");
        command.setReceiverPhone("13434545545");
        command.setUserAddressId("1010");
        command.setAddressCode("1010100");
        command.setRegionId("10002020");
        command.setShippingAreaId("101010212");
        command.setClientIp("34.53.12.34");
        command.setDeviceId("45sf2354adfw245");

        CreateOrderCommand.OrderItem orderItem = new CreateOrderCommand.OrderItem();
        orderItem.setProductType(ProductTypeEnum.NORMAL.getCode());
        orderItem.setSaleQuantity(new BigDecimal("10"));
        orderItem.setSkuCode("skuCode001");
        command.setOrderItems(Arrays.asList(orderItem));

        CreateOrderCommand.OrderAmount orderAmount1 = new CreateOrderCommand.OrderAmount(AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode(), new BigDecimal("100"));
        CreateOrderCommand.OrderAmount orderAmount2 = new CreateOrderCommand.OrderAmount(AmountTypeEnum.SHIPPING_AMOUNT.getCode(), new BigDecimal("0"));
        CreateOrderCommand.OrderAmount orderAmount3 = new CreateOrderCommand.OrderAmount(AmountTypeEnum.REAL_PAY_AMOUNT.getCode(), new BigDecimal("100"));
        command.setOrderAmounts(Arrays.asList(orderAmount1, orderAmount2, orderAmount3));

        CreateOrderCommand.OrderPayment orderPayment = new CreateOrderCommand.OrderPayment();
        orderPayment.setPayType(PayTypeEnum.WEIXIN_PAY.getCode());
        orderPayment.setAccountType(AccountTypeEnum.THIRD.getCode());
        command.setOrderPayments(Arrays.asList(orderPayment));

        CreateOrderDTO dto = createOrderAppService.createOrder(command);
        Assertions.assertNotNull(dto);
    }
}
