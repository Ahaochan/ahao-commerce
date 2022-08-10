package moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import moe.ahao.domain.entity.BaseDO;

import java.util.Date;


/**
 * 会员积分明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_point_detail")
public class MemberPointDetailDO extends BaseDO {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 会员积分ID
     */
    private Long memberPointId;
    /**
     * 用户账号id
     */
    private String userId;
    /**
     * 本次变更之前的积分
     */
    private Integer oldPoint;
    /**
     * 本次变更的积分
     */
    private Integer updatedPoint;
    /**
     * 本次变更之后的积分
     */
    private Integer newPoint;
}
