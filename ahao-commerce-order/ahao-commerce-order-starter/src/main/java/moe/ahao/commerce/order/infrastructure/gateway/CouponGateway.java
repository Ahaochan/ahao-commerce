package moe.ahao.commerce.order.infrastructure.gateway;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import moe.ahao.commerce.market.api.command.LockUserCouponCommand;
import moe.ahao.commerce.market.api.command.ReleaseUserCouponCommand;
import moe.ahao.commerce.market.api.dto.UserCouponDTO;
import moe.ahao.commerce.market.api.query.GetUserCouponQuery;
import moe.ahao.commerce.order.infrastructure.exception.OrderException;
import moe.ahao.commerce.order.infrastructure.gateway.feign.CouponFeignClient;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CouponGateway {
    @Autowired
    private CouponFeignClient couponFeignClient;

    /**
     * 获取用户优惠券
     */
    public UserCouponDTO get(GetUserCouponQuery query) {
        Result<UserCouponDTO> result = couponFeignClient.get(query);
        if (result.getCode() != Result.SUCCESS) {
            throw new OrderException(result.getCode(), result.getMsg());
        }
        return result.getObj();
    }

    /**
     * 锁定用户优惠券
     */
    @SentinelResource(value = "CouponGateway:lock")
    public Boolean lock(LockUserCouponCommand command) {
        Result<Boolean> result = couponFeignClient.lock(command);
        if (result.getCode() != Result.SUCCESS) {
            throw new OrderException(result.getCode(), result.getMsg());
        }
        return result.getObj();
    }

    /**
     * 释放用户优惠券
     */
    @SentinelResource(value = "MarketRemote:releaseUserCoupon")
    public Boolean releaseUserCoupon(ReleaseUserCouponCommand command) {
        Result<Boolean> result = couponFeignClient.release(command);
        if (result.getCode() != Result.SUCCESS) {
            throw new OrderException(result.getCode(), result.getMsg());
        }
        return result.getObj();
    }
}
