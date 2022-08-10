package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderAmountDetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单价格明细表 Mapper 接口
 */
@Mapper
public interface OrderAmountDetailMapper extends BaseMapper<OrderAmountDetailDO> {
    /**
     * 根据订单号查询订单费用明细
     */
    List<OrderAmountDetailDO> selectListByOrderId(@Param("orderId") String orderId);
    List<OrderAmountDetailDO> selectListByOrderIds(@Param("orderIds") List<String> orderIds);

    /**
     * 根据订单号查询订单费用明细
     */
    List<OrderAmountDetailDO> selectListByOrderIdAndOrderItemId(@Param("orderId") String orderId, @Param("orderItemId") String orderItemId);
}
