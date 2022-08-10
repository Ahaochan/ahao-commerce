package moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单列表查询es index
 * 里面的属性来自于order_info,order_item,order_delivery_detail,order_payment_detail
 */
@Data
@Document(indexName = EsOrderListQueryDO.INDEX, createIndex = false)
@Setting(settingPath = "/elastic/index.setting.json")
public class EsOrderListQueryDO implements EsBaseDO {
    public static final String INDEX = "es_order_list_query_index";
    /**
     * 唯一id
     */
    public static final String ES_ID = "esId";
    @Field(name = ES_ID, type = FieldType.Keyword)
    @Id
    private String esId;
    /**
     * 业务线
     */
    public static final String BUSINESS_IDENTIFIER = "businessIdentifier";
    @Field(name = BUSINESS_IDENTIFIER, type = FieldType.Integer)
    private Integer businessIdentifier;
    /**
     * 订单类型
     */
    public static final String ORDER_TYPE = "orderType";
    @Field(name = ORDER_TYPE, type = FieldType.Integer)
    private Integer orderType;
    /**
     * 订单号
     */
    public static final String ORDER_ID = "orderId";
    @Field(name = ORDER_ID, type = FieldType.Keyword)
    private String orderId;
    /**
     * 订单明细编号
     */
    public static final String ORDER_ITEM_ID = "orderItemId";
    @Field(name = ORDER_ITEM_ID, type = FieldType.Keyword)
    private String orderItemId;
    /**
     * 商品类型 1:普通商品,2:预售商品
     */
    public static final String PRODUCT_TYPE = "productType";
    @Field(name = PRODUCT_TYPE, type = FieldType.Integer)
    private Integer productType;
    /**
     * 卖家ID
     */
    public static final String SELLER_ID = "sellerId";
    @Field(name = SELLER_ID, type = FieldType.Keyword)
    private String sellerId;
    /**
     * 父订单号
     */
    public static final String PARENT_ORDER_ID = "parentOrderId";
    @Field(name = PARENT_ORDER_ID, type = FieldType.Keyword)
    private String parentOrderId;
    /**
     * 用户ID
     */
    public static final String USER_ID = "userId";
    @Field(name = USER_ID, type = FieldType.Keyword)
    private String userId;
    /**
     * 订单状态
     */
    public static final String ORDER_STATUS = "orderStatus";
    @Field(name = ORDER_STATUS, type = FieldType.Integer)
    private Integer orderStatus;
    /**
     * 收货人手机号
     */
    public static final String RECEIVER_PHONE = "receiverPhone";
    @Field(type = FieldType.Keyword)
    private String receiverPhone;
    /**
     * 收货人姓名
     */
    public static final String RECEIVER_NAME = "receiverName";
    @Field(name = RECEIVER_NAME, type = FieldType.Keyword)
    private String receiverName;
    /**
     * 交易流水号
     */
    public static final String TRADE_NO = "tradeNo";
    @Field(name = TRADE_NO, type = FieldType.Keyword)
    private String tradeNo;
    /**
     * sku code
     */
    public static final String SKU_CODE = "skuCode";
    @Field(name = SKU_CODE, type = FieldType.Keyword)
    private String skuCode;
    /**
     * sku商品名称
     */
    public static final String PRODUCT_NAME = "productName";
    @Field(name = PRODUCT_NAME, type = FieldType.Keyword)
    private String productName;
    /**
     * 创建时间
     */
    public static final String CREATED_TIME = "createdTime";
    @Field(name = CREATED_TIME, type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date createdTime;
    /**
     * 支付时间
     */
    public static final String PAY_TIME = "payTime";
    @Field(name = PAY_TIME, type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date payTime;
    /**
     * 支付支付类型
     */
    public static final String PAY_TYPE = "payType";
    @Field(name = PAY_TYPE, type = FieldType.Integer)
    private Integer payType;
    /**
     * 支付金额
     */
    public static final String PAY_AMOUNT = "payAmount";
    @Field(name = PAY_AMOUNT, type = FieldType.Double)
    private BigDecimal payAmount;
}
