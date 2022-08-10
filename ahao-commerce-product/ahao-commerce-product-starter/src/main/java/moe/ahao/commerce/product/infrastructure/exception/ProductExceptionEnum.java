package moe.ahao.commerce.product.infrastructure.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import moe.ahao.exception.BizExceptionEnum;

@Getter
@AllArgsConstructor
public enum ProductExceptionEnum implements BizExceptionEnum<ProductException> {
    SKU_CODE_IS_NULL(200001, "sku编号不能为空"),
    PRE_SALE_INFO_IS_NULL(200002, "预售商品信息不能为空"),
    ;
    private final int code;
    private final String message;

    public ProductException msg(Object... args) {
        return new ProductException(code, String.format(message, args));
    }
}
