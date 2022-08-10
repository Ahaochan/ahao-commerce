package moe.ahao.commerce.market.adapter.http;


import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.market.api.CouponFeignApi;
import moe.ahao.commerce.market.api.command.LockUserCouponCommand;
import moe.ahao.commerce.market.api.command.ReleaseUserCouponCommand;
import moe.ahao.commerce.market.api.dto.UserCouponDTO;
import moe.ahao.commerce.market.api.query.GetUserCouponQuery;
import moe.ahao.commerce.market.application.CouponQueryService;
import moe.ahao.commerce.market.application.LockUserCouponAppService;
import moe.ahao.commerce.market.application.ReleaseUserCouponAppService;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(CouponFeignApi.PATH)
public class CouponController implements CouponFeignApi {
    @Autowired
    private CouponQueryService couponQueryService;
    @Autowired
    private LockUserCouponAppService lockUserCouponService;
    @Autowired
    private ReleaseUserCouponAppService releaseUserCouponService;

    @Override
    public Result<UserCouponDTO> get(@RequestBody GetUserCouponQuery query) {
        UserCouponDTO userCouponDTO = couponQueryService.query(query);
        return Result.success(userCouponDTO);
    }

    @Override
    public Result<Boolean> lock(@RequestBody LockUserCouponCommand command) {
        Boolean result = lockUserCouponService.lockUserCoupon(command);
        return Result.success(result);
    }

    @Override
    public Result<Boolean> release(@RequestBody ReleaseUserCouponCommand command) {
        Boolean result = releaseUserCouponService.releaseUserCoupon(command);
        return Result.success(result);
    }
}
