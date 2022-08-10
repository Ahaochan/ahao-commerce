package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponUsedStatusEnum {
    UN_USED(0, "未使用"),
    USED(1, "已使用"),
    ;
    private final Integer code;
    private final String msg; // TODO 改为name
}
