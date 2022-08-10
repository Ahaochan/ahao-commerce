package moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单履约操作日志表 Mapper 接口
 */
@Mapper
public interface OrderFulfillLogMapper extends BaseMapper<OrderFulfillLogDO> {
    List<OrderFulfillLogDO> selectListByOrderIdAndStatus(@Param("orderId") String orderId, @Param("status") Integer status);
}
