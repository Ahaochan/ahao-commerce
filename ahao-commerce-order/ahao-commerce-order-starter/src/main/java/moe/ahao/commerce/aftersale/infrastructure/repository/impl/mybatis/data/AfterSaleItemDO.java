package moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import moe.ahao.domain.entity.BaseDO;

import java.math.BigDecimal;

/**
 * 订单售后详情表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(AfterSaleItemDO.TABLE_NAME)
public class AfterSaleItemDO extends BaseDO {
    public static final String TABLE_NAME = "after_sale_item";
    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 售后id
     */
    private String afterSaleId;
    /**
     * 订单id
     */
    private String orderId;
    /**
     * sku code
     */
    private String skuCode;
    /**
     * 商品名
     */
    private String productName;
    /**
     * 商品图片地址
     */
    private String productImg;
    /**
     * 商品退货数量
     */
    private BigDecimal returnQuantity;
    /**
     * 商品总金额
     */
    private BigDecimal originAmount;
    /**
     * 申请退款金额
     */
    private BigDecimal applyRefundAmount;
    /**
     * 实际退款金额
     */
    private BigDecimal realRefundAmount;
    /**
     * 本条目退货完成标记 10:购买的sku未全部退货 20:购买的sku已全部退货
     */
    private Integer returnCompletionMark;

    /**
     * 售后条目类型 10:售后订单条目 20:尾笔条目退优惠券 30:尾笔条目退运费
     */
    private Integer afterSaleItemType;
}
