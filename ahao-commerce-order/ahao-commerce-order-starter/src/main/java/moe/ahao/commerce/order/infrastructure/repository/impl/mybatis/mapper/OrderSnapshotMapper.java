package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.hbase.data.OrderSnapshotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单快照表 Mapper 接口
 */
@Mapper
@Deprecated
public interface OrderSnapshotMapper extends BaseMapper<OrderSnapshotDO> {
    /**
     * 根据订单号查询订单快照
     */
    List<OrderSnapshotDO> selectListByOrderId(@Param("orderId") String orderId);
}
