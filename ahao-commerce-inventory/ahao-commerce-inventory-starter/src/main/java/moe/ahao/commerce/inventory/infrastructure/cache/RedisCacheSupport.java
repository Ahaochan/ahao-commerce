package moe.ahao.commerce.inventory.infrastructure.cache;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 库存缓存key-value support
 */
public interface RedisCacheSupport {

    String PREFIX_PRODUCT_STOCK = "PRODUCT_STOCK:";

    /**
     * 可销售库存key
     */
    String SALE_STOCK = "saleStockQuantity";

    /**
     * 已销售库存key
     */
    String SALED_STOCK = "saledStockQuantity";

    /**
     * 构造缓存商品库存key
     *
     * @param skuCode
     * @return
     */
    static String buildProductStockKey(String skuCode) {
        return PREFIX_PRODUCT_STOCK + ":" + skuCode;
    }

    /**
     * 构造缓存商品库存value
     *
     * @param saleStockQuantity
     * @param saledStockQuantity
     * @return
     */
    static Map<String, String> buildProductStockValue(BigDecimal saleStockQuantity, BigDecimal saledStockQuantity) {
        Map<String, String> value = new HashMap<>();
        value.put(SALE_STOCK, String.valueOf(saleStockQuantity));
        value.put(SALED_STOCK, String.valueOf(saledStockQuantity));
        return value;
    }
}
