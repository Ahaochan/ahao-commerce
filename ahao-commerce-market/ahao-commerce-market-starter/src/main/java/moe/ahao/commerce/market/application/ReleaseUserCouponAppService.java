package moe.ahao.commerce.market.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.market.api.command.ReleaseUserCouponCommand;
import moe.ahao.commerce.common.enums.CouponUsedStatusEnum;
import moe.ahao.commerce.market.infrastructure.exception.MarketExceptionEnum;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.data.CouponDO;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.mapper.CouponMapper;
import moe.ahao.exception.CommonBizExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
public class ReleaseUserCouponAppService {
    @Autowired
    private ReleaseUserCouponAppService _this;
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private RedissonClient redissonClient;

    public boolean releaseUserCoupon(ReleaseUserCouponCommand event) {
        log.info("开始执行回滚优惠券, couponId:{}", event.getCouponId());
        String couponId = event.getCouponId();
        if (StringUtils.isEmpty(couponId)) {
            return true;
        }
        String lockKey = RedisLockKeyConstants.RELEASE_COUPON_KEY + couponId;

        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw MarketExceptionEnum.RELEASE_COUPON_FAILED.msg();
        }
        try {
            // 执行释放优惠券
            boolean result = _this.releaseUserCouponWithTx(event);
            return result;
        } finally {
            log.info("回滚优惠券成功, couponId:{}", event.getCouponId());
            lock.unlock();
        }
    }
    /**
     * 释放用户优惠券
     * 这里不会有并发问题, 数据库会加上行锁
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseUserCouponWithTx(ReleaseUserCouponCommand event) {
        // 1. 检查入参
        this.check(event);

        // 2. 获取优惠券信息
        String userId = event.getUserId();
        String couponId = event.getCouponId();
        CouponDO couponAchieve = couponMapper.selectOneByUserIdAndCouponId(userId, couponId);
        if (couponAchieve == null) {
            return true;
        }
        // 3. 判断优惠券是否已经使用了
        if (!Objects.equals(CouponUsedStatusEnum.USED.getCode(), couponAchieve.getUsed())) {
            log.info("当前用户未使用优惠券,不用回退,userId:{},couponId:{}", userId, couponId);
            return true;
        }

        // 4. 释放优惠券, 优惠券空回滚是没关系的, 状态一直停留在un_used
        couponMapper.updateUsedById(CouponUsedStatusEnum.UN_USED.getCode(), null, couponAchieve.getId());
        return true;
    }

    private void check(ReleaseUserCouponCommand command) {
        String userId = command.getUserId();
        String couponId = command.getCouponId();
        if (StringUtils.isAnyEmpty(userId, couponId)) {
            throw CommonBizExceptionEnum.SERVER_ILLEGAL_ARGUMENT_ERROR.msg();
        }
    }
}
