package moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch.data;

import lombok.Data;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data.EsBaseDO;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;

/**
 * es 售后单条目
 */
@Data
@Document(indexName = "es_after_sale_item", createIndex = false)
public class EsAfterSaleItemDO implements EsBaseDO {
    /**
     * esId = afterSaleId+skuCode
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
     * 售后id
     */
    @Field(type = FieldType.Keyword)
    private String afterSaleId;
    /**
     * 订单id
     */
    @Field(type = FieldType.Keyword)
    private String orderId;
    /**
     * sku code
     */
    @Field(type = FieldType.Keyword)
    private String skuCode;
    /**
     * 商品名
     */
    @Field(type = FieldType.Keyword)
    private String productName;
    /**
     * 商品图片地址
     */
    @Field(type = FieldType.Keyword)
    private String productImg;
    /**
     * 商品退货数量
     */
    @Field(type = FieldType.Double)
    private BigDecimal returnQuantity;
    /**
     * 商品总金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal originAmount;
    /**
     * 申请退款金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal applyRefundAmount;
    /**
     * 实际退款金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal realRefundAmount;
    /**
     * 本条目退货完成标记 10:购买的sku未全部退货 20:购买的sku已全部退货
     */
    @Field(type = FieldType.Integer)
    private Integer returnCompletionMark;
    /**
     * 售后条目类型 10:售后订单条目 20:尾笔条目退优惠券 30:尾笔条目退运费
     */
    @Field(type = FieldType.Integer)
    private Integer afterSaleItemType;
}
