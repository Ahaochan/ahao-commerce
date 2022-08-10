package moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import moe.ahao.domain.entity.BaseDO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * 订单操作日志表
 */
@Data
@EqualsAndHashCode(callSuper = true)
// @TableName("order_operate_log")
@Document("order_operate_log")
@NoArgsConstructor
public class OrderOperateLogDO extends BaseDO {
    /**
     * 主键id
     */
    // @TableId(value = "id", type = IdType.AUTO)
    @Id
    @Indexed
    private String id;
    /**
     * 订单id
     */
    @Indexed(name = "idx_order_id", direction = IndexDirection.ASCENDING, background = true)
    @Field(value = "order_id")
    private String orderId;
    /**
     * 操作类型
     */
    @Field(value = "operate_type")
    private Integer operateType;
    /**
     * 前置状态
     */
    @Field(value = "pre_status")
    private Integer preStatus;
    /**
     * 当前状态
     */
    @Field(value = "current_status")
    private Integer currentStatus;
    /**
     * 备注说明
     */
    @Field(value = "remark")
    private String remark;
    /**
     * 创建时间
     */
    @Field(value = "create_time")
    private Date createTime;
    /**
     * 更新时间
     */
    @Field(value = "update_time")
    private Date updateTime;

    public OrderOperateLogDO(OrderOperateLogDO that) {
        this.setId(that.id);
        this.setOrderId(that.orderId);
        this.setOperateType(that.operateType);
        this.setPreStatus(that.preStatus);
        this.setCurrentStatus(that.currentStatus);
        this.setRemark(that.remark);
        this.setCreateBy(that.getCreateBy());
        this.setUpdateBy(that.getUpdateBy());
        this.setCreateTime(that.getCreateTime());
        this.setUpdateTime(that.getUpdateTime());
    }
}
