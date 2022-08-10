package moe.ahao.commerce.market;

import moe.ahao.commerce.market.api.command.LockUserCouponCommand;
import moe.ahao.commerce.market.api.command.ReleaseUserCouponCommand;
import moe.ahao.commerce.market.api.dto.UserCouponDTO;
import moe.ahao.commerce.market.api.query.GetUserCouponQuery;
import moe.ahao.commerce.market.application.CouponQueryService;
import moe.ahao.commerce.market.application.LockUserCouponAppService;
import moe.ahao.commerce.market.application.ReleaseUserCouponAppService;
import moe.ahao.embedded.RedisExtension;
import moe.ahao.exception.BizException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = MarketApplication.class)
@ActiveProfiles("test")
public class CouponTest {
    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();

    @Autowired
    private CouponQueryService couponQueryService;
    @Autowired
    private LockUserCouponAppService lockUserCouponService;
    @Autowired
    private ReleaseUserCouponAppService releaseUserCouponService;

    @Test
    public void get() {
        GetUserCouponQuery query = new GetUserCouponQuery();
        query.setUserId("100");
        query.setCouponId("1001001");
        UserCouponDTO result = couponQueryService.query(query);
        System.out.println(result);
        Assertions.assertNotNull(result);
    }

    @Test
    public void used() {
        LockUserCouponCommand command = new LockUserCouponCommand();
        command.setBusinessIdentifier(1);
        command.setOrderId("1021121945762025110");
        command.setSellerId("101");
        command.setUserId("100");
        command.setCouponId("1001001");
        Assertions.assertThrows(BizException.class, () -> lockUserCouponService.lockUserCoupon(command)).printStackTrace();
    }

    @Test
    public void lockAndRelease() {
        LockUserCouponCommand command1 = new LockUserCouponCommand();
        command1.setBusinessIdentifier(1);
        command1.setOrderId("1021121945762025110");
        command1.setSellerId("101");
        command1.setUserId("101");
        command1.setCouponId("1001002");
        Boolean locked = lockUserCouponService.lockUserCoupon(command1);
        Assertions.assertTrue(locked);

        ReleaseUserCouponCommand command2 = new ReleaseUserCouponCommand();
        command2.setUserId(command1.getUserId());
        command2.setCouponId(command1.getCouponId());
        command2.setOrderId(command1.getOrderId());
        command2.setAfterSaleId("123456789");
        Boolean released = releaseUserCouponService.releaseUserCoupon(command2);
        Assertions.assertTrue(released);
    }
}
