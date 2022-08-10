package moe.ahao.commerce.market.infrastructure.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import moe.ahao.exception.BizExceptionEnum;

@Getter
@AllArgsConstructor
public enum MarketExceptionEnum implements BizExceptionEnum<MarketException> {
    USER_COUPON_IS_NULL(300001, "优惠券记录不存在"),
    USER_COUPON_IS_USED(300002, "优惠券记录已经被使用了"),
    USER_COUPON_CONFIG_IS_NULL(300003, "优惠券活动配置记录不存在"),
    SEND_MQ_FAILED(300004, "发送MQ消息失败"),
    CONSUME_MQ_FAILED(300005, "消费MQ消息失败"),
    RELEASE_COUPON_FAILED(300006, "释放优惠券失败"),
    UPDATE_POINT_ERROR(300007, "更新会员积分异常"),
    ;

    private final int code;
    private final String message;

    public MarketException msg(Object... args) {
        return new MarketException(code, String.format(message, args));
    }
}
