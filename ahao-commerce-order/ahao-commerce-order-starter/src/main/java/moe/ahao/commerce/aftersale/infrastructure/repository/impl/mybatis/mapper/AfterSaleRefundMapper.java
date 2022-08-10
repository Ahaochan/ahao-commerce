package moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleRefundDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 售后支付表 Mapper 接口
 */
@Mapper
public interface AfterSaleRefundMapper extends BaseMapper<AfterSaleRefundDO> {
    int updateRefundInfoByAfterSaleId(@Param("afterSaleId") String afterSaleId, @Param("refundStatus") Integer refundStatus, @Param("refundPayTime") Date refundPayTime, @Param("remark") String remark);

    /**
     * 根据售后单号查询售后单支付记录
     */
    AfterSaleRefundDO selectOneByAfterSaleId(@Param("afterSaleId") String afterSaleId);
    List<AfterSaleRefundDO> selectListByAfterSaleIds(@Param("afterSaleIds") List<String> afterSaleIds);
}
