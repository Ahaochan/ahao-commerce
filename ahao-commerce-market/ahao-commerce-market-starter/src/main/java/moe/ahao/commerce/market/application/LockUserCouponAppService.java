package moe.ahao.commerce.market.application;

import moe.ahao.commerce.market.api.command.LockUserCouponCommand;
import moe.ahao.commerce.common.enums.CouponUsedStatusEnum;
import moe.ahao.commerce.market.infrastructure.exception.MarketExceptionEnum;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.data.CouponDO;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.mapper.CouponMapper;
import moe.ahao.exception.CommonBizExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

@Service
public class LockUserCouponAppService {
    @Autowired
    private CouponMapper couponMapper;

    /**
     * 锁定用户优惠券
     * 这里不会有并发问题, 数据库会加上行锁
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean lockUserCoupon(LockUserCouponCommand command) {
        // 1. 检查入参
        this.check(command);

        // 2. 获取优惠券信息
        String userId = command.getUserId();
        String couponId = command.getCouponId();
        CouponDO userCouponDO = couponMapper.selectOneByUserIdAndCouponId(userId, couponId);
        if (userCouponDO == null) {
            throw MarketExceptionEnum.USER_COUPON_IS_NULL.msg();
        }
        // 3. 判断优惠券是否已经使用了
        if (!Objects.equals(CouponUsedStatusEnum.UN_USED.getCode(), userCouponDO.getUsed())) {
            throw MarketExceptionEnum.USER_COUPON_IS_USED.msg();
        }

        // 4. 锁定优惠券
        couponMapper.updateUsedById(CouponUsedStatusEnum.USED.getCode(), new Date(), userCouponDO.getId());
        return true;
    }

    private void check(LockUserCouponCommand command) {
        String userId = command.getUserId();
        String couponId = command.getCouponId();
        if (StringUtils.isAnyEmpty(userId, couponId)) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }
    }
}
