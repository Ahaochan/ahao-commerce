package moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import moe.ahao.domain.entity.BaseDO;

import java.math.BigDecimal;

/**
 * 订单履约条目
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_fulfill_item")
@NoArgsConstructor
public class OrderFulfillItemDO extends BaseDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 履约单id
     */
    private String fulfillId;
    /**
     * 商品skuCode
     */
    private String skuCode;
    /**
     * 商品类型
     */
    private Integer productType;
    /**
     * 商品名称
     */
    private String productName;
    /**
     * 销售单价
     */
    private BigDecimal salePrice;
    /**
     * 销售数量
     */
    private BigDecimal saleQuantity;
    /**
     * 商品单位
     */
    private String productUnit;
    /**
     * 付款金额
     */
    private BigDecimal payAmount;
    /**
     * 当前商品支付原总价
     */
    private BigDecimal originAmount;

    public OrderFulfillItemDO(OrderFulfillItemDO that) {
        this.setId(that.id);
        this.setFulfillId(that.fulfillId);
        this.setSkuCode(that.skuCode);
        this.setProductType(that.productType);
        this.setProductName(that.productName);
        this.setSalePrice(that.salePrice);
        this.setSaleQuantity(that.saleQuantity);
        this.setProductUnit(that.productUnit);
        this.setPayAmount(that.payAmount);
        this.setOriginAmount(that.originAmount);
        this.setCreateBy(that.getCreateBy());
        this.setUpdateBy(that.getUpdateBy());
        this.setCreateTime(that.getCreateTime());
        this.setUpdateTime(that.getUpdateTime());
    }
}
