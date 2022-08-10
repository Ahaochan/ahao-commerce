package moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch.data;

import lombok.Data;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data.EsBaseDO;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 售后单列表查询es index
 * 里面的属性来自于after_sale_info,after_sale_item,after_sale_refund
 */
@Data
@Document(indexName = EsAfterSaleListQueryDO.INDEX, createIndex = false)
public class EsAfterSaleListQueryDO implements EsBaseDO {
    public static final String INDEX = "es_after_sale_list_query_index";
    /**
     * 唯一id
     */
    public static final String ES_ID = "esId";
    @Id
    @Field(name = ES_ID, type = FieldType.Keyword)
    private String esId;
    /**
     * 业务线
     */
    public static final String BUSINESS_IDENTIFIER = "businessIdentifier";
    @Field(name = BUSINESS_IDENTIFIER, type = FieldType.Integer)
    private Integer businessIdentifier;
    /**
     * 售后单号
     */
    public static final String AFTER_SALE_ID = "afterSaleId";
    @Field(name = AFTER_SALE_ID, type = FieldType.Keyword)
    private String afterSaleId;
    /**
     * 订单号
     */
    public static final String ORDER_ID = "orderId";
    @Field(name = ORDER_ID, type = FieldType.Keyword)
    private String orderId;
    /**
     * 订单类型
     */
    public static final String ORDER_TYPE = "orderType";
    @Field(name = ORDER_TYPE, type = FieldType.Integer)
    private Integer orderType;
    /**
     * 售后单状态
     */
    public static final String AFTER_SALE_STATUS = "afterSaleStatus";
    @Field(name = AFTER_SALE_STATUS, type = FieldType.Integer)
    private Integer afterSaleStatus;
    /**
     * 售后申请来源
     */
    public static final String APPLY_SOURCE = "applySource";
    @Field(name = APPLY_SOURCE, type = FieldType.Integer)
    private Integer applySource;
    /**
     * 售后类型
     */
    public static final String AFTER_SALE_TYPE = "afterSaleType";
    @Field(name = AFTER_SALE_TYPE, type = FieldType.Integer)
    private Integer afterSaleType;
    /**
     * 用户ID
     */
    public static final String USER_ID = "userId";
    @Field(name = USER_ID, type = FieldType.Keyword)
    private String userId;
    /**
     * sku code
     */
    public static final String SKU_CODE = "skuCode";
    @Field(name = SKU_CODE, type = FieldType.Keyword)
    private String skuCode;
    /**
     * 创建时间
     */
    public static final String CREATED_TIME = "createdTime";
    @Field(name = CREATED_TIME, type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date createdTime;
    /**
     * 售后申请时间
     */
    public static final String APPLY_TIME = "applyTime";
    @Field(name = APPLY_TIME, type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date applyTime;
    /**
     * 售后客服审核时间
     */
    public static final String REVIEW_TIME = "reviewTime";
    @Field(name = REVIEW_TIME, type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date reviewTime;
    /**
     * 退款支付时间
     */
    public static final String REFUND_PAY_TIME = "refundPayTime";
    @Field(name = REFUND_PAY_TIME, type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date refundPayTime;
    /**
     * 退款金额
     */
    public static final String REFUND_AMOUNT = "refundAmount";
    @Field(name = REFUND_AMOUNT, type = FieldType.Double)
    private BigDecimal refundAmount;
}
