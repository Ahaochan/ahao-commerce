package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.data.OrderOperateLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单操作日志表 Mapper 接口
 */
@Mapper
@Deprecated
public interface OrderOperateLogMapper extends BaseMapper<OrderOperateLogDO> {
    /**
     * 根据订单号查询订单操作日志
     */
    List<OrderOperateLogDO> selectListByOrderId(@Param("orderId") String orderId);
}
