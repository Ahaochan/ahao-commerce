package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderDeliveryDetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单配送信息表 Mapper 接口
 */
@Mapper
public interface OrderDeliveryDetailMapper extends BaseMapper<OrderDeliveryDetailDO> {
    /**
     * 更新出库时间
     */
    int updateOutStockTimeByOrderId(@Param("orderId") String orderId, @Param("outStockTime") Date outStockTime);

    /**
     * 更新配送员信息
     */
    int updateDelivererByOrderId(@Param("orderId") String orderId, @Param("delivererNo") String delivererNo, @Param("delivererName") String delivererName, @Param("delivererPhone") String delivererPhone);

    /**
     * 更新签收时间
     */
    int updateSignedTimeByOrderId(@Param("orderId") String orderId, @Param("signedTime") Date signedTime);

    /**
     * 更新配送地址信息
     */
    int updateDeliveryAddressByOrderId(@Param("orderId") String orderId, @Param("province") String province, @Param("city") String city, @Param("area") String area, @Param("street") String street, @Param("detailAddress") String detailAddress, @Param("lat") BigDecimal lat, @Param("lon") BigDecimal lon);

    /**
     * 根据订单号查询订单配送信息
     */
    OrderDeliveryDetailDO selectOneByOrderId(@Param("orderId") String orderId);
    List<OrderDeliveryDetailDO> selectListByOrderIds(@Param("orderIds") List<String> orderIds);
}
