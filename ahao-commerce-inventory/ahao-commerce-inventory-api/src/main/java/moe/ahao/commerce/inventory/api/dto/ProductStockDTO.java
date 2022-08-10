package moe.ahao.commerce.inventory.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductStockDTO {
    private String skuCode;
    private BigDecimal saleStockQuantity;
    private BigDecimal saledStockQuantity;
}
