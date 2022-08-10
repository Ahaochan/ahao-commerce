package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import moe.ahao.commerce.order.api.dto.OrderListDTO;
import moe.ahao.commerce.order.api.query.OrderPageQuery;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;


/**
 * 订单表 Mapper 接口
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfoDO> {
    /**
     * 软删除订单
     */
    int updateDeleteStatusByIds(@Param("ids") List<Long> ids, @Param("deleteStatus") Integer deleteStatus);

    /**
     * 更新订单扩展信息
     */
    int updateExtJsonByOrderId(@Param("orderId") String orderId, @Param("extJson") String extJson);

    /**
     * 更新订单状态
     */
    int updateOrderStatusByOrderId(@Param("orderId") String orderId, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);
    int updateOrderStatusByOrderIds(@Param("orderIds") List<String> orderId, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);

    /**
     * 更新订单状态和更新时间
     */
    int updateOrderStatusAndPayTimeByOrderId(@Param("orderId") String orderId, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus, @Param("payTime") Date payTime);
    int updateOrderStatusAndPayTimeByOrderIds(@Param("orderIds") List<String> orderId, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus, @Param("payTime") Date payTime);

    /**
     * 更新订单预支付信息
     */
    int updatePrePayInfoByOrderId(@Param("orderId") String orderId, @Param("payType") Integer payType, @Param("payTime") Date payTime);
    int updatePrePayInfoByOrderIds(@Param("orderIds") List<String> orderIds, @Param("payType") Integer payType, @Param("payTime") Date payTime);

    /**
     * 更新订单取消信息
     */
    int updateCancelInfoByOrderId(@Param("orderId") String orderId, @Param("cancelType") Integer cancelType, @Param("orderStatus") Integer orderStatus, @Param("cancelTime") Date cancelTime);
    int updateCancelInfoByOrderIds(@Param("orderIds") List<String> orderIds, @Param("cancelType") Integer cancelType, @Param("orderStatus") Integer orderStatus, @Param("cancelTime") Date cancelTime);

    /**
     * 根据订单号查询订单
     */
    List<OrderInfoDO> selectListByOrderIds(@Param("orderIds") List<String> orderIds);

    /**
     * 根据订单号查询子订单
     */
    List<OrderInfoDO> selectListByParentOrderId(@Param("parentOrderId") String parentOrderId);

    /**
     * 根据订单状态查询
     */
    List<OrderInfoDO> selectListByOrderStatus(@Param("orderStatus") List<Integer> orderStatus);

    /**
     * 根据订单号查询订单
     */
    OrderInfoDO selectOneByOrderId(@Param("orderId") String orderId);

    /**
     * 订单分页查询
     */
    Page<OrderListDTO> selectPage(Page<OrderListDTO> page, @Param("query") OrderPageQuery query);
}
