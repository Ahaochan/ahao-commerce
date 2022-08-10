package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderPaymentDetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 订单支付明细表 Mapper 接口
 */
@Mapper
public interface OrderPaymentDetailMapper extends BaseMapper<OrderPaymentDetailDO> {
    /**
     * 更新支付明细预支付信息
     */
    int updatePrePayInfoByOrderId(@Param("orderId") String orderId, @Param("payType") Integer payType, @Param("payTime") Date payTime, @Param("outTradeNo") String outTradeNo);
    /**
     * 更新支付明细预支付信息
     */
    int updatePrePayInfoByOrderIds(@Param("orderIds") List<String> orderIds, @Param("payType") Integer payType, @Param("payTime") Date payTime, @Param("outTradeNo") String outTradeNo);
    /**
     * 更新支付明细的支付状态
     */
    int updatePayStatusByOrderId(@Param("orderId") String orderId, @Param("payStatus") Integer payStatus);
    /**
     * 更新支付明细的支付状态
     */
    int updatePayStatusByOrderIds(@Param("orderIds") List<String> orderIds, @Param("payStatus") Integer payStatus);
    /**
     * 更新支付明细的支付状态
     */
    int updatePayStatusAndPayTimeByOrderId(@Param("orderId") String orderId, @Param("payStatus") Integer payStatus, @Param("payTime") Date payTime);
    /**
     * 更新支付明细的支付状态
     */
    int updatePayStatusAndPayTimeByOrderIds(@Param("orderIds") List<String> orderIds, @Param("payStatus") Integer payStatus, @Param("payTime") Date payTime);

    /**
     * 根据订单号查询支付明细
     */
    List<OrderPaymentDetailDO> selectListByOrderId(@Param("orderId") String orderId);

    /**
     * 根据多个订单号查询支付明细
     */
    List<OrderPaymentDetailDO> selectListByOrderIds(@Param("orderIds") List<String> orderId);

    /**
     * 根据订单号查询支付明细
     */
    OrderPaymentDetailDO selectOneByOrderId(@Param("orderId") String orderId);
}
