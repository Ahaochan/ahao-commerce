package moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;

/**
 * es 订单条目
 */

@Data
@Document(indexName = "es_order_item", createIndex = false)
public class EsOrderItemDO implements EsBaseDO {
    /**
     * esId = orderItemId
     */
    @Id
    @Field(type = FieldType.Keyword)
    private String esId;
    /**
     * 主键ID
     */
    @Field(type = FieldType.Keyword)
    private Long id;
    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date updateTime;
    /**
     * 订单编号
     */
    @Field(type = FieldType.Keyword)
    private String orderId;
    /**
     * 订单明细编号
     */
    @Field(type = FieldType.Keyword)
    private String orderItemId;
    /**
     * 商品类型 1:普通商品,2:预售商品
     */
    @Field(type = FieldType.Integer)
    private Integer productType;
    /**
     * 商品编号
     */
    @Field(type = FieldType.Keyword)
    private String productId;
    /**
     * 商品图片
     */
    @Field(type = FieldType.Keyword)
    private String productImg;
    /**
     * 商品名称
     */
    @Field(type = FieldType.Keyword)
    private String productName;
    /**
     * sku编码
     */
    @Field(type = FieldType.Keyword)
    private String skuCode;
    /**
     * 销售数量
     */
    @Field(type = FieldType.Double)
    private BigDecimal saleQuantity;
    /**
     * 销售单价
     */
    @Field(type = FieldType.Double)
    private BigDecimal salePrice;
    /**
     * 当前商品支付原总价
     */
    @Field(type = FieldType.Double)
    private BigDecimal originAmount;
    /**
     * 交易支付金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal payAmount;
    /**
     * 商品单位
     */
    @Field(type = FieldType.Keyword)
    private String productUnit;
    /**
     * 采购成本价
     */
    @Field(type = FieldType.Double)
    private BigDecimal purchasePrice;
    /**
     * 卖家ID
     */
    @Field(type = FieldType.Keyword)
    private String sellerId;
    /**
     * 扩展信息
     */
    @Field(type = FieldType.Keyword)
    private String extJson;
}
