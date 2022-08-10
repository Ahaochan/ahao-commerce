package moe.ahao.commerce.market.application;

import lombok.Data;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.market.api.command.MemberPointIncreaseCommand;
import moe.ahao.commerce.market.infrastructure.exception.MarketExceptionEnum;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.data.MemberPointDO;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.data.MemberPointDetailDO;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.mapper.MemberPointDetailMapper;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.mapper.MemberPointMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MemberPointIncreaseAppService {
    @Autowired
    @Lazy
    private MemberPointIncreaseAppService _this;

    @Autowired
    private MemberPointMapper memberPointMapper;
    @Autowired
    private MemberPointDetailMapper memberPointDetailMapper;

    @Autowired
    private RedissonClient redissonClient;

    public void increase(MemberPointIncreaseCommand command) {
        String userId = command.getUserId();

        String lockKey = RedisLockKeyConstants.UPDATE_USER_POINT_KEY + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw MarketExceptionEnum.UPDATE_POINT_ERROR.msg();
        }

        try {
            _this.doIncrease(command);
        } finally {
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void doIncrease(MemberPointIncreaseCommand command) {
        String userId = command.getUserId();
        Integer increasedPoint = command.getIncreasedPoint();

        // 1. 查询用户的积分
        MemberPointDO memberPoint = memberPointMapper.selectOneByUserId(userId);
        if (memberPoint == null) {
            memberPoint = new MemberPointDO();
            memberPoint.setPoint(0);
            memberPoint.setUserId(userId);
            memberPointMapper.insert(memberPoint);
        }

        // 2. 添加会员积分明细
        Integer oldPoint = memberPoint.getPoint();
        Integer newPoint = oldPoint + increasedPoint;

        MemberPointDetailDO memberPointDetail = new MemberPointDetailDO();
        memberPointDetail.setMemberPointId(memberPoint.getId());
        memberPointDetail.setUserId(userId);
        memberPointDetail.setOldPoint(oldPoint);
        memberPointDetail.setUpdatedPoint(increasedPoint);
        memberPointDetail.setNewPoint(newPoint);
        memberPointDetailMapper.insert(memberPointDetail);

        // 3. 更新会员积分
        memberPointMapper.increasePoint(userId, oldPoint, increasedPoint);
    }
}
