package moe.ahao.commerce.product.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


/**
 * 商品sku信息
 */
@Data
public class ProductSkuDTO {
    /**
     * 商品编号
     */
    private String productId;
    /**
     * 商品类型 1:普通商品,2:预售商品
     */
    private Integer productType;
    /**
     * 商品sku编号
     */
    private String skuCode;
    /**
     * 商品名称
     */
    private String productName;
    /**
     * 商品图片
     */
    private String productImg;
    /**
     * 商品单位
     */
    private String productUnit;
    /**
     * 商品销售价格
     */
    private BigDecimal salePrice;
    /**
     * 商品采购价格
     */
    private BigDecimal purchasePrice;
    /**
     * 预售商品信息
     */
    private PreSaleInfoDTO preSaleInfo;

    @Data
    public static class PreSaleInfoDTO {
        /**
         * 预售时间
         */
        private Date preSaleTime;
    }
}
