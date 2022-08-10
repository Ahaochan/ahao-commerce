package moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import moe.ahao.domain.entity.BaseDO;

import java.io.Serializable;
import java.util.Date;

/**
 * 履约单操作日志表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_fulfill_log")
public class OrderFulfillLogDO extends BaseDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 履约单id
     */
    private String fulfillId;
    /**
     * 操作类型
     */
    private Integer operateType;
    /**
     * 前置状态
     */
    private Integer preStatus;
    /**
     * 当前状态
     */
    private Integer currentStatus;
    /**
     * 备注说明
     */
    private String remark;
}
