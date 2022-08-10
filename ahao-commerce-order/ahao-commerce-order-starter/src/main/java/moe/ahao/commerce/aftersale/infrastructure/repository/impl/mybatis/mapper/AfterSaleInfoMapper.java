package moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import moe.ahao.commerce.aftersale.api.dto.AfterSaleOrderListDTO;
import moe.ahao.commerce.aftersale.api.query.AfterSalePageQuery;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 订单售后表 Mapper 接口
 */
@Mapper
public interface AfterSaleInfoMapper extends BaseMapper<AfterSaleInfoDO> {
    /**
     * 更新售后单状态
     */
    int updateAfterSaleStatusByAfterSaleId(@Param("afterSaleId") String afterSaleId, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);

    int updateReviewInfoByAfterSaleId(@Param("afterSaleId") String afterSaleId, @Param("afterSaleStatus") Integer afterSaleStatus, @Param("reviewReason") String reviewReason, @Param("reviewReasonCode") Integer reviewReasonCode, @Param("reviewSource") Integer reviewSource, @Param("reviewTime") Date reviewTime);

    /**
     * 根据订单编号，售后类型详情查询售后单
     */
    List<AfterSaleInfoDO> selectListByOrderIdAndAfterSaleTypeDetails(@Param("orderId") String orderId, @Param("afterSaleTypeDetails") List<Integer> afterSaleTypeDetails);
    List<AfterSaleInfoDO> selectListByAfterSaleIds(@Param("afterSaleIds") List<String> afterSaleIds);

    AfterSaleInfoDO selectOneByAfterSaleId(@Param("afterSaleId") String afterSaleId);
    AfterSaleInfoDO selectOneByOrderId(@Param("orderId") String orderId);

    /**
     * 售后单分页查询
     */
    Page<AfterSaleOrderListDTO> selectPage(Page<AfterSaleOrderListDTO> page, @Param("query") AfterSalePageQuery query);
}
