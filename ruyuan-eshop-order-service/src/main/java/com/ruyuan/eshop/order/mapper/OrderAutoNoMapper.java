package com.ruyuan.eshop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyuan.eshop.order.domain.entity.OrderAutoNoDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Mapper
public interface OrderAutoNoMapper extends BaseMapper<OrderAutoNoDO> {

    /**
     * 更新maxid
     *
     * @param bizTag 业务标识
     * @return 返回
     */
    @Update("UPDATE order_auto_no SET max_id = max_id + step WHERE biz_tag = #{bizTag}")
    int updateMaxId(@Param("bizTag") String bizTag);

    /**
     * bizTag查询
     *
     * @param bizTag 业务标识
     * @return 返回
     */
    @Select("SELECT * FROM order_auto_no WHERE biz_tag = #{bizTag}")
    @Results(value = {
            @Result(id = true, column = "id", property = "id"),
            @Result(column = "biz_tag", property = "bizTag"),
            @Result(column = "max_id", property = "maxId"),
            @Result(column = "step", property = "step"),
            @Result(column = "desc", property = "desc"),
            @Result(column = "gmt_create", property = "gmtCreate"),
            @Result(column = "gmt_modified", property = "gmtModified")
    })
    OrderAutoNoDO findByBizTag(@Param("bizTag") String bizTag);

    /**
     * 使用动态计算出来额步长更新maxid
     *
     * @param bizTag      业务tag
     * @param dynamicStep 动态计算出来的步长
     * @return 返回
     */
    @Update("UPDATE order_auto_no SET max_id = max_id + #{step} WHERE biz_tag = #{bizTag}")
    int updateMaxIdByDynamicStep(@Param("bizTag") String bizTag, @Param("step") int dynamicStep);

    /**
     * 获取所有bizTag
     *
     * @return 返回
     */
    @Select("SELECT biz_tag FROM order_auto_no")
    List<String> listAllBizTag();

}