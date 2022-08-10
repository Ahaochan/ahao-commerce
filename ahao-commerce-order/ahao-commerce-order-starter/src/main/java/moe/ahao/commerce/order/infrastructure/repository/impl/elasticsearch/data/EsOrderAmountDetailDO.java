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
 * es 订单价格明细
 */
@Data
@Document(indexName = "es_order_amount_detail", createIndex = false)
public class EsOrderAmountDetailDO implements EsBaseDO {
    /**
     * esId = orderId + skuCode + amountType
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
     * 产品类型
     */
    @Field(type = FieldType.Integer)
    private Integer productType;
    /**
     * 订单明细编号
     */
    @Field(type = FieldType.Keyword)
    private String orderItemId;
    /**
     * 商品编号
     */
    @Field(type = FieldType.Keyword)
    private String productId;
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
     * 收费类型
     */
    @Field(type = FieldType.Integer)
    private Integer amountType;
    /**
     * 收费金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal amount;
}
