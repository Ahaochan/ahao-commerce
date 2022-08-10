package moe.ahao.commerce.aftersale.infrastructure.repository.impl.elasticsearch.data;

import lombok.Data;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data.EsBaseDO;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * es 售后单
 */
@Data
@Document(indexName = "es_after_sale_info", createIndex = false)
public class EsAfterSaleInfoDO implements EsBaseDO {
    /**
     * esId = afterSaleId
     */
    public static final String ES_ID = "esId";
    @Id
    @Field(name = ES_ID, type = FieldType.Keyword)
    private String esId;
    /**
     * 主键ID
     */
    public static final String ID = "id";
    @Field(name = ID, type = FieldType.Keyword)
    private Long id;
    /**
     * 创建时间
     */
    public static final String CREATE_TIME = "createTime";
    @Field(name = CREATE_TIME, type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 更新时间
     */
    public static final String UPDATE_TIME = "updateTime";
    @Field(name = UPDATE_TIME, type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date updateTime;
    /**
     * 售后id
     */
    public static final String AFTER_SALE_ID = "afterSaleId";
    @Field(name = AFTER_SALE_ID, type = FieldType.Keyword)
    private String afterSaleId;
    /**
     * 接入方业务标识
     */
    public static final String BUSINESS_IDENTIFIER = "businessIdentifier";
    @Field(name = BUSINESS_IDENTIFIER, type = FieldType.Integer)
    private Integer businessIdentifier;
    /**
     * 订单号
     */
    public static final String ORDER_ID = "orderId";
    @Field(name = ORDER_ID, type = FieldType.Keyword)
    private String orderId;
    /**
     * 购买用户id
     */
    public static final String USER_ID = "userId";
    @Field(name = USER_ID, type = FieldType.Keyword)
    private String userId;
    /**
     * 订单类型
     */
    public static final String ORDER_TYPE = "orderType";
    @Field(name = ORDER_TYPE, type = FieldType.Integer)
    private Integer orderType;
    /**
     * 申请售后来源
     */
    public static final String APPLY_SOURCE = "applySource";
    @Field(name = APPLY_SOURCE, type = FieldType.Integer)
    private Integer applySource;
    /**
     * 申请售后时间
     */
    public static final String APPLY_TIME = "applyTime";
    @Field(name = APPLY_TIME, type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date applyTime;
    /**
     * 申请原因编码
     */
    public static final String APPLY_REASON_CODE = "applyReasonCode";
    @Field(name = APPLY_REASON_CODE, type = FieldType.Integer)
    private Integer applyReasonCode;
    /**
     * 申请原因
     */
    public static final String APPLY_REASON = "applyReason";
    @Field(name = APPLY_REASON, type = FieldType.Keyword)
    private String applyReason;
    /**
     * 审核时间
     */
    public static final String REVIEW_TIME = "reviewTime";
    @Field(name = REVIEW_TIME, type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date reviewTime;
    /**
     * 客服审核来源
     */
    public static final String REVIEW_SOURCE = "reviewSource";
    @Field(name = REVIEW_SOURCE, type = FieldType.Integer)
    private Integer reviewSource;
    /**
     * 客服审核结果编码
     */
    public static final String REVIEW_REASON_CODE = "reviewReasonCode";
    @Field(name = REVIEW_REASON_CODE, type = FieldType.Integer)
    private Integer reviewReasonCode;
    /**
     * 客服审核结果
     */
    public static final String REVIEW_REASON = "reviewReason";
    @Field(name = REVIEW_REASON, type = FieldType.Keyword)
    private String reviewReason;
    /**
     * 售后类型
     */
    public static final String AFTER_SALE_TYPE = "afterSaleType";
    @Field(name = AFTER_SALE_TYPE, type = FieldType.Integer)
    private Integer afterSaleType;
    /**
     * 售后类型详情枚举
     */
    public static final String AFTER_SALE_TYPE_DETAIL = "afterSaleTypeDetail";
    @Field(name = AFTER_SALE_TYPE_DETAIL, type = FieldType.Integer)
    private Integer afterSaleTypeDetail;
    /**
     * 售后单状态
     */
    public static final String AFTER_SALE_STATUS = "afterSaleStatus";
    @Field(name = AFTER_SALE_STATUS, type = FieldType.Integer)
    private Integer afterSaleStatus;
    /**
     * 申请退款金额
     */
    public static final String APPLY_REFUND_AMOUNT = "applyRefundAmount";
    @Field(name = APPLY_REFUND_AMOUNT, type = FieldType.Double)
    private BigDecimal applyRefundAmount;
    /**
     * 实际退款金额
     */
    public static final String REAL_REFUND_AMOUNT = "realRefundAmount";
    @Field(name = REAL_REFUND_AMOUNT, type = FieldType.Double)
    private BigDecimal realRefundAmount;
    /**
     * 备注
     */
    public static final String REMARK = "remark";
    @Field(name = REMARK, type = FieldType.Keyword)
    private String remark;
}
