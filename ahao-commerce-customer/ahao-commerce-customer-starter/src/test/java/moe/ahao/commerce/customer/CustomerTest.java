package moe.ahao.commerce.customer;

import moe.ahao.commerce.common.enums.AfterSaleTypeEnum;
import moe.ahao.commerce.common.enums.CustomerAuditResult;
import moe.ahao.commerce.customer.api.command.CustomerReviewReturnGoodsCommand;
import moe.ahao.commerce.customer.api.event.CustomerReceiveAfterSaleEvent;
import moe.ahao.commerce.customer.application.CustomerAuditAppService;
import moe.ahao.commerce.customer.application.ReceivableAfterSaleAppService;
import moe.ahao.commerce.customer.infrastructure.gateway.feign.AfterSaleFeignClient;
import moe.ahao.domain.entity.Result;
import moe.ahao.embedded.RedisExtension;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = CustomerApplication.class)
@ActiveProfiles("test")

@EnableAutoConfiguration(exclude = RocketMQAutoConfiguration.class)
public class CustomerTest {
    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();

    @MockBean
    private AfterSaleFeignClient afterSaleFeignClient;

    @Autowired @InjectMocks
    private CustomerAuditAppService customerAuditAppService;
    @Autowired
    private ReceivableAfterSaleAppService receivableAfterSaleAppService;

    @BeforeEach
    public void beforeEach() {
        Mockito.when(afterSaleFeignClient.receiveCustomerAuditResult(any())).thenReturn(Result.success(true));
    }

    @Test
    public void receive() {
        CustomerReceiveAfterSaleEvent command = new CustomerReceiveAfterSaleEvent();
        command.setUserId("用户id");
        command.setOrderId("订单id");
        command.setAfterSaleId("售后单id");
        command.setAfterSaleRefundId("售后退款单id");
        command.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());
        command.setReturnGoodAmount(new BigDecimal("100"));
        command.setApplyRefundAmount(new BigDecimal("100"));

        //  客服接收订单系统的售后申请
        boolean result = receivableAfterSaleAppService.handler(command);
        Assertions.assertTrue(result);
    }

    @Test
    public void audit() {
        CustomerReviewReturnGoodsCommand command = new CustomerReviewReturnGoodsCommand();
        command.setAfterSaleId("售后单id");
        command.setCustomerId("客服id");
        command.setAuditResult(CustomerAuditResult.ACCEPT.getCode());
        command.setAuditResultDesc("审核结果描述");

        Boolean success = customerAuditAppService.audit(command);
        Assertions.assertTrue(success);
    }
}
