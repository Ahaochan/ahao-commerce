package moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import moe.ahao.domain.entity.BaseDO;

/**
 * 售后单变更表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("after_sale_log")
public class AfterSaleLogDO extends BaseDO {
    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 售后单id
     */
    private String afterSaleId;
    /**
     * 订单号
     */
    private String orderId;
    /**
     * 前一个状态
     */
    private Integer preStatus;
    /**
     * 当前状态
     */
    private Integer currentStatus;
    /**
     * 备注
     */
    private String remark;
}
