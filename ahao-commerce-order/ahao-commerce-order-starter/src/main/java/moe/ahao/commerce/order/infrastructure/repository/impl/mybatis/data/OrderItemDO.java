package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import moe.ahao.domain.entity.BaseDO;

import java.math.BigDecimal;

/**
 * 订单条目表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
@NoArgsConstructor
public class OrderItemDO extends BaseDO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 订单明细id
     */
    private String orderItemId;
    /**
     * 商品类型 1:普通商品,2:预售商品
     */
    private Integer productType;
    /**
     * 商品id
     */
    private String productId;
    /**
     * 商品图片
     */
    private String productImg;
    /**
     * 商品名称
     */
    private String productName;
    /**
     * sku编码
     */
    private String skuCode;
    /**
     * 销售数量
     */
    private BigDecimal saleQuantity;
    /**
     * 销售单价
     */
    private BigDecimal salePrice;
    /**
     * 当前商品支付原总价
     */
    private BigDecimal originAmount;
    /**
     * 交易支付金额
     */
    private BigDecimal payAmount;
    /**
     * 商品单位
     */
    private String productUnit;
    /**
     * 采购成本价
     */
    private BigDecimal purchasePrice;
    /**
     * 卖家id
     */
    private String sellerId;
    /**
     * 扩展信息
     */
    private String extJson;

    public OrderItemDO(OrderItemDO that) {
        this.setId(that.id);
        this.setOrderId(that.orderId);
        this.setOrderItemId(that.orderItemId);
        this.setProductType(that.getProductType());
        this.setProductId(that.productId);
        this.setProductImg(that.productImg);
        this.setProductName(that.productName);
        this.setSkuCode(that.skuCode);
        this.setSaleQuantity(that.saleQuantity);
        this.setSalePrice(that.salePrice);
        this.setOriginAmount(that.originAmount);
        this.setPayAmount(that.payAmount);
        this.setProductUnit(that.productUnit);
        this.setPurchasePrice(that.purchasePrice);
        this.setSellerId(that.sellerId);
        this.setExtJson(that.extJson);
        this.setCreateBy(that.getCreateBy());
        this.setUpdateBy(that.getUpdateBy());
        this.setCreateTime(that.getCreateTime());
        this.setUpdateTime(that.getUpdateTime());
    }
}
