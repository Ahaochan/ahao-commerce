package moe.ahao.commerce.tms;

import moe.ahao.commerce.common.enums.PayTypeEnum;
import moe.ahao.commerce.tms.api.command.CancelSendOutCommand;
import moe.ahao.commerce.tms.api.command.SendOutCommand;
import moe.ahao.commerce.tms.application.CancelSendOutAppService;
import moe.ahao.commerce.tms.application.SendOutAppService;
import moe.ahao.commerce.tms.infrastructure.repository.impl.mybatis.mapper.LogisticOrderMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Arrays;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TmsApplication.class)
@ActiveProfiles("test")
public class TmsTest {
    @Autowired
    private SendOutAppService sendOutAppService;
    @Autowired
    private CancelSendOutAppService cancelSendOutAppService;

    @Autowired
    private LogisticOrderMapper logisticOrderMapper;

    @Test
    public void test() {
        // 1. 测试发货
        SendOutCommand command1 = new SendOutCommand();
        command1.setBusinessIdentifier(1);
        command1.setOrderId("1021121945762025110");
        command1.setSellerId("101");
        command1.setUserId("110");
        command1.setDeliveryType(1);
        command1.setReceiverName("??");
        command1.setReceiverPhone("13434545545");
        command1.setReceiverProvince("110000");
        command1.setReceiverCity("110100");
        command1.setReceiverArea("110105");
        command1.setReceiverStreet("110101007");
        command1.setReceiverDetailAddress("北京北京市东城区朝阳门街道???10?");
        command1.setReceiverLon(new BigDecimal("1010.2010100000"));
        command1.setReceiverLat(new BigDecimal("100.1000000000"));
        command1.setUserRemark(null);
        command1.setPayType(PayTypeEnum.WEIXIN_PAY.getCode());
        command1.setPayAmount(new BigDecimal("10000"));
        command1.setTotalAmount(new BigDecimal("10000"));
        command1.setDeliveryAmount(BigDecimal.ZERO);
        command1.setTmsException(null);
        command1.setOrderItems(Arrays.asList(
            new SendOutCommand.OrderItem("skuCode411", "压测数据411", new BigDecimal("1000"), new BigDecimal("10"), "个", new BigDecimal("10000"), new BigDecimal("10000"))
        ));
        sendOutAppService.sendOut(command1);
        Assertions.assertTrue(logisticOrderMapper.selectListByOrderId(command1.getOrderId()).size() > 0);

        // 2. 测试撤销发货
        CancelSendOutCommand command2 = new CancelSendOutCommand();
        command2.setOrderId(command1.getOrderId());
        cancelSendOutAppService.cancelSendOut(command2);
        Assertions.assertTrue(logisticOrderMapper.selectListByOrderId(command1.getOrderId()).size() <= 0);
    }
}
