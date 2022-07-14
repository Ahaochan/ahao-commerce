package moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import moe.ahao.domain.entity.BaseDO;

import java.math.BigDecimal;

/**
 * 订单履约条目
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_fulfill_item")
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
}
