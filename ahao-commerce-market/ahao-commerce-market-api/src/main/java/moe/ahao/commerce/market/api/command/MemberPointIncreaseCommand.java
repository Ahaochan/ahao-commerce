package moe.ahao.commerce.market.api.command;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MemberPointIncreaseCommand {
    /**
     * 用户id
     */
    private String userId;
    /**
     * 要增加的积分
     */
    private Integer increasedPoint;
}
