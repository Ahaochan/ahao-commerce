package moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单售后条目表 Mapper 接口
 */
@Mapper
public interface AfterSaleItemMapper extends BaseMapper<AfterSaleItemDO> {
    /**
     * 更新条目售后完成标记
     */
    int updateReturnCompletionMark(@Param("orderId") String orderId, @Param("skuCode") String skuCode, @Param("mark") Integer mark);
    /**
     * 根据售后单号查询售后单条目记录
     */
    List<AfterSaleItemDO> selectListByAfterSaleId(@Param("afterSaleId") String afterSaleId);
    /**
     * 查询售后单条目
     */
    List<AfterSaleItemDO> selectListByAfterSaleIds(@Param("afterSaleIds") List<String> afterSaleIds);
    /**
     * 根据订单号查询售后单条目
     */
    List<AfterSaleItemDO> selectListByOrderId(@Param("orderId") String orderId);
    /**
     * 根据orderId和skuCode查询售后单条目
     * 这里做成list便于以后扩展
     * 目前仅支持整笔条目的退货，所以当前list里只有一条
     */
    List<AfterSaleItemDO> selectListByOrderIdAndSkuCode(@Param("orderId") String orderId, @Param("skuCode") String skuCode);
    /**
     * 查询出不包含当前afterSaleId的售后条目
     */
    List<AfterSaleItemDO> selectListByOrderIdAndExcludeAfterSaleId(@Param("orderId") String orderId, @Param("afterSaleId") String afterSaleId);
    /**
     * 根据orderId查询指定订单中returnMark标记的售后条目
     */
    List<AfterSaleItemDO> selectListByOrderIdAndReturnCompletionMark(@Param("orderId") String orderId, @Param("returnCompletionMark") Integer returnCompletionMark);
    /**
     * 查询全部优惠券售后单和运费售后单
     */
    List<AfterSaleItemDO> selectListByOrderIdAndType(@Param("orderId") String orderId, @Param("afterSaleItemTypes") Integer... afterSaleItemType);
    /**
     * 根据orderId、afterSaleId、skuCode查询售后订单条目
     */
    AfterSaleItemDO selectLastOne(@Param("orderId") String orderId, @Param("afterSaleId") String afterSaleId, @Param("afterSaleItemMark") Integer afterSaleItemMark);
}
