package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单条目表 Mapper 接口
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemDO> {
    /**
     * 根据订单号查询订单条目
     */
    List<OrderItemDO> selectListByOrderId(@Param("orderId") String orderId);
    List<OrderItemDO> selectListByOrderIds(@Param("orderIds") List<String> orderIds);

    /**
     * 按订单号与产品类型查询订单条目
     */
    List<OrderItemDO> selectListByOrderIdAndProductType(@Param("orderId") String orderId, @Param("productType") Integer productType);

    /**
     * 根据订单号和skuCode查询订单条目
     */
    OrderItemDO selectOneByOrderIdAndSkuCode(@Param("orderId") String orderId, @Param("skuCode") String skuCode);
}
