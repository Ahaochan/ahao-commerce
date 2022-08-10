package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderCancelScheduledTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 订单执行定时取消兜底任务Mapper
 */
@Mapper
public interface OrderCancelScheduledTaskMapper extends BaseMapper<OrderCancelScheduledTaskDO> {
    /**
     * 根据订单号删除订单任务记录
     */
    int deleteByOrderId(@Param("orderId") String orderId);

    /**
     * 根据订单号查询订单任务记录
     */
    OrderCancelScheduledTaskDO selectOneByOrderId(@Param("orderId") String orderId);

    /**
     * 查询所有支付截止时间 <= 当前时间 的未支付订单记录
     */
    List<OrderCancelScheduledTaskDO> selectListByExpireTime(@Param("expireTime") Date expireTime);
}
