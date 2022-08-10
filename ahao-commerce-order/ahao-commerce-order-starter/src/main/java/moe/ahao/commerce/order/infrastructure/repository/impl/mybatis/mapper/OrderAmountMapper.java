package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderAmountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单价格表 Mapper 接口
 */
@Mapper
public interface OrderAmountMapper extends BaseMapper<OrderAmountDO> {
    /**
     * 根据订单号查询订单价格
     */
    List<OrderAmountDO> selectListByOrderId(@Param("orderId") String orderId);
    List<OrderAmountDO> selectListByOrderIds(@Param("orderIds") List<String> orderIds);

    /**
     * 查询订单指定类型费用
     */
    OrderAmountDO selectOneByOrderIdAndAmountType(@Param("orderId") String orderId, @Param("amountType") Integer amountType);
}
