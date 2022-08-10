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
 * es 售后单
 */
@Data
@Document(indexName = "es_after_sale_refund", createIndex = false)
public class EsAfterSaleRefundDO implements EsBaseDO {
    /**
     * esId = afterSaleId
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
     * 售后单号
     */
    @Field(type = FieldType.Keyword)
    private String afterSaleId;
    /**
     * 订单号
     */
    @Field(type = FieldType.Keyword)
    private String orderId;
    /**
     * 售后批次号
     */
    @Field(type = FieldType.Keyword)
    private String afterSaleBatchNo;
    /**
     * 账户类型
     */
    @Field(type = FieldType.Integer)
    private Integer accountType;
    /**
     * 支付类型
     */
    @Field(type = FieldType.Integer)
    private Integer payType;
    /**
     * 退款状态
     */
    @Field(type = FieldType.Integer)
    private Integer refundStatus;
    /**
     * 退款金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal refundAmount;
    /**
     * 退款支付时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date refundPayTime;
    /**
     * 交易单号
     */
    @Field(type = FieldType.Keyword)
    private String outTradeNo;
    /**
     * 备注
     */
    @Field(type = FieldType.Keyword)
    private String remark;
}
