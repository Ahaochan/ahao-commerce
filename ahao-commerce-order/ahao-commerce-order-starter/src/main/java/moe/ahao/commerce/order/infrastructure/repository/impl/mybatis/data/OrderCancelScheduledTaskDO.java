package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import moe.ahao.domain.entity.BaseDO;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时执行订单取消兜底任务表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_cancel_scheduled_task")
public class OrderCancelScheduledTaskDO extends BaseDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单编号
     */
    private String orderId;
    /**
     * 订单支付截止时间
     */
    private Date expireTime;
}
