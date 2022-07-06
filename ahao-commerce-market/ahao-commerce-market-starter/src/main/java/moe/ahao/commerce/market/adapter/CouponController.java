package moe.ahao.commerce.market.adapter;


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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(CouponFeignApi.CONTEXT)
public class CouponController implements CouponFeignApi {
    @Autowired
    private CouponQueryService couponQueryService;
    @Autowired
    private LockUserCouponAppService lockUserCouponService;
    @Autowired
    private ReleaseUserCouponAppService releaseUserCouponService;

    @Override
    public Result<UserCouponDTO> get(GetUserCouponQuery query) {
        UserCouponDTO userCouponDTO = couponQueryService.query(query);
        return Result.success(userCouponDTO);
    }

    @Override
    public Result<Boolean> lock(LockUserCouponCommand command) {
        Boolean result = lockUserCouponService.lockUserCoupon(command);
        return Result.success(result);
    }

    @Override
    public Result<Boolean> release(ReleaseUserCouponCommand command) {
        Boolean result = releaseUserCouponService.releaseUserCoupon(command);
        return Result.success(result);
    }
}