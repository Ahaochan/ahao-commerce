package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderAutoNoDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 订单编号表 Mapper 接口
 */
@Mapper
public interface OrderAutoNoMapper extends BaseMapper<OrderAutoNoDO> {
    /**
     * 更新maxid
     *
     * @param bizTag 业务标识
     * @return 返回
     */
    int updateMaxIdByBizTag(@Param("bizTag") String bizTag);

    /**
     * 使用动态计算出来额步长更新maxid
     *
     * @param bizTag      业务tag
     * @param dynamicStep 动态计算出来的步长
     * @return 返回
     */
    int updateMaxIdWithStepByBizTag(@Param("bizTag") String bizTag, @Param("step") int dynamicStep);

    /**
     * bizTag查询, 查到我的这个事务视图里可以看到的max_id和step当前这条数据
     *
     * @param bizTag 业务标识
     * @return 返回
     */
    OrderAutoNoDO selectOneByBizTag(@Param("bizTag") String bizTag);

    /**
     * 获取所有bizTag
     *
     * @return 返回
     */
    List<String> selectBizTagList();

}
