package moe.ahao.commerce.market.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import moe.ahao.domain.entity.BaseDO;

import java.util.Date;

/**
 * 会员积分表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_point")
public class MemberPointDO extends BaseDO {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 用户账号id
     */
    private String userId;
    /**
     * 会员积分
     */
    private Integer point;
}
