package moe.ahao.commerce.wms;

import moe.ahao.commerce.common.enums.PayTypeEnum;
import moe.ahao.commerce.wms.api.command.CancelPickGoodsCommand;
import moe.ahao.commerce.wms.api.command.PickGoodsCommand;
import moe.ahao.commerce.wms.application.CancelPickGoodsAppService;
import moe.ahao.commerce.wms.application.PickGoodsAppService;
import moe.ahao.commerce.wms.infrastructure.repository.impl.mybatis.mapper.DeliveryOrderMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Arrays;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WmsApplication.class)
@ActiveProfiles("test")
public class WmsTest {

    @Autowired
    private PickGoodsAppService pickGoodsAppService;
    @Autowired
    private CancelPickGoodsAppService cancelPickGoodsAppService;

    @Autowired
    private DeliveryOrderMapper deliveryOrderMapper;

    @Test
    public void test() {
        // 1. 测试拣货
        PickGoodsCommand command1 = new PickGoodsCommand();
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
        command1.setWmsException(null);
        command1.setOrderItems(Arrays.asList(
            new PickGoodsCommand.OrderItem("skuCode411", "压测数据411", new BigDecimal("1000"), new BigDecimal("10"), "个", new BigDecimal("10000"), new BigDecimal("10000"))
        ));
        pickGoodsAppService.pickGoods(command1);
        Assertions.assertTrue(deliveryOrderMapper.selectListByOrderId(command1.getOrderId()).size() > 0);

        // 2. 测试撤销拣货
        CancelPickGoodsCommand command2 = new CancelPickGoodsCommand();
        command2.setOrderId(command1.getOrderId());
        cancelPickGoodsAppService.cancelPickGoods(command2);
        Assertions.assertTrue(deliveryOrderMapper.selectListByOrderId(command1.getOrderId()).size() <= 0);
    }
}
