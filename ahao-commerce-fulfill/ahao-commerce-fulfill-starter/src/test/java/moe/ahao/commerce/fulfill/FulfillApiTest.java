package moe.ahao.commerce.fulfill;

import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.PayTypeEnum;
import moe.ahao.commerce.common.enums.ProductTypeEnum;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.api.event.OrderDeliveredEvent;
import moe.ahao.commerce.fulfill.api.event.OrderOutStockEvent;
import moe.ahao.commerce.fulfill.api.event.OrderSignedEvent;
import moe.ahao.commerce.fulfill.api.event.TriggerOrderAfterFulfillEvent;
import moe.ahao.commerce.fulfill.application.CancelFulfillAppService;
import moe.ahao.commerce.fulfill.application.ReceiveFulfillAppService;
import moe.ahao.commerce.fulfill.application.TriggerOrderWmsShipAppService;
import moe.ahao.commerce.fulfill.infrastructure.gateway.feign.TmsFeignClient;
import moe.ahao.commerce.fulfill.infrastructure.gateway.feign.WmsFeignClient;
import moe.ahao.commerce.fulfill.infrastructure.publisher.DefaultProducer;
import moe.ahao.commerce.tms.api.dto.SendOutDTO;
import moe.ahao.commerce.wms.api.dto.PickDTO;
import moe.ahao.domain.entity.Result;
import moe.ahao.embedded.RedisExtension;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = FulfillApplication.class)
@ActiveProfiles("test")

@EnableAutoConfiguration(exclude = RocketMQAutoConfiguration.class)
public class FulfillApiTest {
    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();
    @MockBean
    private DefaultProducer defaultProducer;
    @MockBean
    private WmsFeignClient wmsFeignClient;
    @MockBean
    private TmsFeignClient tmsFeignClient;

    @Autowired
    private ReceiveFulfillAppService receiveFulfillAppService;
    @Autowired
    private CancelFulfillAppService cancelFulfillAppService;
    @Autowired
    private TriggerOrderWmsShipAppService triggerOrderWmsShipAppService;

    @BeforeEach
    public void beforeEach() {
        Mockito.doNothing().when(defaultProducer).sendMessage(any(), any(), any());

        PickDTO pickDTO = new PickDTO("订单号");
        Mockito.when(wmsFeignClient.pickGoods(any())).thenReturn(Result.success(pickDTO));

        SendOutDTO sendOutDTO = new SendOutDTO("订单号", "物流单号", "配送员Code", "配送员姓名", "配送员手机号");
        Mockito.when(tmsFeignClient.sendOut(any())).thenReturn(Result.success(sendOutDTO));
    }

    /**
     * 触发接收订单履约
     */
    @Test
    public void fulfill() throws Exception {
        ReceiveFulfillCommand command = new ReceiveFulfillCommand();
        command.setBusinessIdentifier(1);
        command.setOrderId("1021121945762025110");
        command.setSellerId("101");
        command.setUserId("110");
        command.setDeliveryType(1);
        command.setReceiverName("??");
        command.setReceiverPhone("13434545545");
        command.setReceiverProvince("110000");
        command.setReceiverCity("110100");
        command.setReceiverArea("110105");
        command.setReceiverStreet("110101007");
        command.setReceiverDetailAddress("北京北京市东城区朝阳门街道???10?");
        command.setReceiverLon(new BigDecimal("1010.2010100000"));
        command.setReceiverLat(new BigDecimal("100.1000000000"));
        command.setUserRemark(null);
        command.setPayType(PayTypeEnum.WEIXIN_PAY.getCode());
        command.setPayAmount(new BigDecimal("10000"));
        command.setTotalAmount(new BigDecimal("10000"));
        command.setDeliveryAmount(BigDecimal.ZERO);
        command.setFulfillException(null);
        command.setWmsException(null);
        command.setTmsException(null);
        command.setReceiveOrderItems(Arrays.asList(
            new ReceiveFulfillCommand.ReceiveOrderItem("skuCode411", ProductTypeEnum.NORMAL.getCode(), "压测数据411", new BigDecimal("1000"), new BigDecimal("10"), "个", new BigDecimal("10000"), new BigDecimal("10000"), "")
        ));

        receiveFulfillAppService.fulfill(command);
        cancelFulfillAppService.cancelFulfill(command.getOrderId());
    }

    @Test
    public void triggerOrderWmsShipEvent() throws Exception {
        String orderId = "1011250000000010000";
        OrderStatusChangeEnum orderStatusChange = OrderStatusChangeEnum.ORDER_OUT_STOCKED;
        OrderOutStockEvent wmsEvent1 = new OrderOutStockEvent();
        wmsEvent1.setOrderId(orderId);
        wmsEvent1.setOutStockTime(new Date());

        TriggerOrderAfterFulfillEvent request = new TriggerOrderAfterFulfillEvent(
            orderId, "11", orderStatusChange, wmsEvent1
        );
        triggerOrderWmsShipAppService.trigger(request);


        orderStatusChange = OrderStatusChangeEnum.ORDER_DELIVERED;
        OrderDeliveredEvent wmsEvent2 = new OrderDeliveredEvent();
        wmsEvent2.setOrderId(orderId);
        wmsEvent2.setDelivererNo("rc2019");
        wmsEvent2.setDelivererName("张三");
        wmsEvent2.setDelivererPhone("19100012112");

        request = new TriggerOrderAfterFulfillEvent(
            orderId, "11", orderStatusChange, wmsEvent2
        );

        triggerOrderWmsShipAppService.trigger(request);


        orderStatusChange = OrderStatusChangeEnum.ORDER_SIGNED;
        OrderSignedEvent wmsEvent3 = new OrderSignedEvent();
        wmsEvent3.setOrderId(orderId);
        wmsEvent3.setSignedTime(new Date());

        request = new TriggerOrderAfterFulfillEvent(
            orderId, "11", orderStatusChange, wmsEvent3
        );

        triggerOrderWmsShipAppService.trigger(request);
    }



}
