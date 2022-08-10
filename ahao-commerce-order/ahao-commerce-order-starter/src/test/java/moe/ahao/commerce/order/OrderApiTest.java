package moe.ahao.commerce.order;

import moe.ahao.commerce.order.OrderApplication;
import moe.ahao.commerce.order.api.command.AdjustDeliveryAddressCommand;
import moe.ahao.commerce.order.api.command.RemoveOrderCommand;
import moe.ahao.commerce.order.application.AdjustDeliveryAddressAppService;
import moe.ahao.commerce.order.application.RemoveOrderAppService;
import moe.ahao.commerce.order.infrastructure.publisher.DefaultProducer;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderDeliveryDetailMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
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

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = OrderApplication.class)
@ActiveProfiles("test")
public class OrderApiTest {
    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();
    @MockBean
    private DefaultProducer defaultProducer;

    @Autowired
    private RemoveOrderAppService removeOrderAppService;
    @Autowired
    private AdjustDeliveryAddressAppService adjustDeliveryAddressAppService;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDeliveryDetailMapper orderDeliveryDetailMapper;

    @BeforeEach
    public void beforeEach() {
        Mockito.doNothing().when(defaultProducer).sendMessage(any(), any(), any(), any());
        Mockito.doNothing().when(defaultProducer).sendMessage(any(), any(), any(), any(), any());
        Mockito.doNothing().when(defaultProducer).sendMessage(any(), any(), any(), any(), any(), any(), any());

    }

    @Test
    public void removeOrders() throws Exception {
        String orderId = "1021120832956929100";
        Assertions.assertEquals(0, orderInfoMapper.selectOneByOrderId(orderId).getDeleteStatus());

        RemoveOrderCommand removeOrderRequest = new RemoveOrderCommand();
        Set<String> orderIds = new HashSet<>();
        orderIds.add(orderId);
        removeOrderRequest.setOrderIds(orderIds);
        removeOrderAppService.removeOrders(removeOrderRequest);

        Assertions.assertEquals(1, orderInfoMapper.selectOneByOrderId(orderId).getDeleteStatus());
    }

    @Test
    public void adjustDeliveryAddress() throws Exception {
        String orderId = "1021121232909584100";
        String province = "北京";
        Assertions.assertNotEquals(province, orderDeliveryDetailMapper.selectOneByOrderId(orderId).getProvince());

        AdjustDeliveryAddressCommand adjustDeliveryAddressRequest = new AdjustDeliveryAddressCommand();
        adjustDeliveryAddressRequest.setOrderId(orderId);
        adjustDeliveryAddressRequest.setProvince(province);
        adjustDeliveryAddressAppService.adjustDeliveryAddress(adjustDeliveryAddressRequest);

        Assertions.assertEquals(province, orderDeliveryDetailMapper.selectOneByOrderId(orderId).getProvince());
    }
}
