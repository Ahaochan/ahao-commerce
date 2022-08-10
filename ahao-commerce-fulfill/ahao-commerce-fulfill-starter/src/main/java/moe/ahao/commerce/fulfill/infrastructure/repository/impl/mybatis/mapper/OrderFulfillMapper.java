package moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单履约表 Mapper 接口
 */
@Mapper
public interface OrderFulfillMapper extends BaseMapper<OrderFulfillDO> {
    /**
     * 保存物流单号
     *
     * @param fulfillId     履约单id
     * @param logisticsCode 物流单号
     * @return 影响条数
     */
    int updateLogisticsCodeByFulfillId(@Param("fulfillId") String fulfillId, @Param("logisticsCode") String logisticsCode);

    /**
     * 更新配送员信息
     *
     * @param fulfillId      履约单id
     * @param delivererNo    配送单号
     * @param delivererName  配送员名称
     * @param delivererPhone 配送员电话
     * @return 影响条数
     */
    int updateDelivererInfoByFulfillId(@Param("fulfillId") String fulfillId, @Param("delivererNo") String delivererNo, @Param("delivererName") String delivererName, @Param("delivererPhone") String delivererPhone);

    /**
     * 更新履约单状态
     */
    int updateFulfillStatusByOrderId(@Param("orderId") String orderId, @Param("formStatus") Integer formStatus, @Param("toStatus") Integer toStatus);
    int updateFulfillStatusByFulfillId(@Param("fulfillId") String fulfillId, @Param("formStatus") Integer formStatus, @Param("toStatus") Integer toStatus);

    /**
     * 查询履约单
     *
     * @param orderId 订单id
     * @return 履约单数据
     */
    OrderFulfillDO selectOneByOrderId(@Param("orderId") String orderId);

    /**
     * 查询履约单
     *
     * @param fulfillId 履约单id
     * @return 履约单数据
     */
    OrderFulfillDO selectOneByFulfillId(@Param("fulfillId") String fulfillId);

    /**
     * 查询履约单
     *
     * @param orderId 订单id
     * @return 履约单数据
     */
    List<OrderFulfillDO> selectListByOrderId(@Param("orderId") String orderId);

    /**
     * 查询履约单
     *
     * @param type   履约类型
     * @param status 履约单状态
     * @return 履约单数据
     * @see moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillTypeEnum 履约单类型
     * @see moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillStatusEnum 履约单状态
     */
    List<OrderFulfillDO> selectListByFulfillTypeAndStatus(@Param("type") Integer type, @Param("status") Integer status);
}
