package moe.ahao.commerce.product.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import moe.ahao.domain.entity.BaseDO;

import java.math.BigDecimal;

/**
 * 商品sku记录表
 */
@Data
@TableName("product_sku")
public class ProductSkuDO extends BaseDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 商品编号
     */
    private String productId;
    /**
     * 商品类型 1:普通商品,2:预售商品
     */
    private Integer productType;
    /**
     * 商品SKU编码
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
     * 扩展信息
     */
    private String extJson;
}
