package moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.data.MemberPointDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会员积分 Mapper 接口
 */
@Mapper
public interface MemberPointMapper extends BaseMapper<MemberPointDO> {
    /**
     * 根据userId查询会员积分
     */
    MemberPointDO selectOneByUserId(@Param("userId") String userId);

    /**
     * 添加用户积分
     */
    int increasePoint(@Param("userId") String userId, @Param("oldPoint") Integer oldPoint, @Param("increasedPoint") Integer increasedPoint);
}
