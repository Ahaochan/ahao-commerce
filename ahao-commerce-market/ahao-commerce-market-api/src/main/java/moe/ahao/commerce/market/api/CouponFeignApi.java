package moe.ahao.commerce.market.api;

import moe.ahao.commerce.market.api.command.LockUserCouponCommand;
import moe.ahao.commerce.market.api.command.ReleaseUserCouponCommand;
import moe.ahao.commerce.market.api.dto.UserCouponDTO;
import moe.ahao.commerce.market.api.query.GetUserCouponQuery;
import moe.ahao.domain.entity.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface CouponFeignApi {
    String PATH = "/api/coupon";

    /**
     * 查询用户的优惠券
     */
    @PostMapping("/get")
    Result<UserCouponDTO> get(@RequestBody GetUserCouponQuery query);

    /**
     * 锁定用户优惠券记录
     */
    @PostMapping("/lock")
    Result<Boolean> lock(@RequestBody LockUserCouponCommand command);

    /**
     * 释放用户已使用的优惠券
     */
    @PostMapping("/release")
    Result<Boolean> release(@RequestBody ReleaseUserCouponCommand command);
}
