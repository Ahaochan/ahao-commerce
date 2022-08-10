package moe.ahao.commerce.inventory.infrastructure.tcc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.ahao.commerce.inventory.infrastructure.repository.impl.mybatis.data.ProductStockDO;

import java.math.BigDecimal;

/**
 * 扣减库存DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Deprecated
public class DeductStockDTO {
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 商品skuCode
     */
    private String skuCode;
    /**
     * 销售数量
     */
    private BigDecimal saleQuantity;
    /**
     * sku库存数据
     */
    private ProductStockDO productStockDO;
}
