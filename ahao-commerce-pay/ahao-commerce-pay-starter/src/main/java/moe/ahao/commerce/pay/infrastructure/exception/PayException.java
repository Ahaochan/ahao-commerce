package moe.ahao.commerce.pay.infrastructure.exception;

import moe.ahao.exception.BizException;

public class PayException extends BizException {
    public PayException(int code, String message) {
        super(code, message);
    }
}
