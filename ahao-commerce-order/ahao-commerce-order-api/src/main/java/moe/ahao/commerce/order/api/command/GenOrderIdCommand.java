package moe.ahao.commerce.order.api.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenOrderIdCommand {
    /**
     * 订单号类型枚举
     */
    private Integer orderIdType;
    /**
     * 用户id
     */
    private String userId;
}
