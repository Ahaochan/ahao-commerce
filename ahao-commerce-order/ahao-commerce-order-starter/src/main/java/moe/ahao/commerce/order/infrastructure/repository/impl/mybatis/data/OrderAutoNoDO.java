package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import moe.ahao.domain.entity.BaseDO;

/**
 * 订单编号表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_auto_no")
public class OrderAutoNoDO extends BaseDO {
    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 业务标识
     */
    private String bizTag;
    /**
     * 号段最大值
     */
    private Long maxId;
    /**
     * 下一个号段的步长
     */
    private Integer step;
    /**
     * 说明
     */
    private String desc;
}
