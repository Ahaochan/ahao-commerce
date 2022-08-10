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
 * es 订单支付明细
 */
@Data
@Document(indexName = "es_order_payment_detail", createIndex = false)
public class EsOrderPaymentDetailDO implements EsBaseDO {
    /**
     * esId = orderId + payType
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
     * 账户类型
     */
    @Field(type = FieldType.Integer)
    private Integer accountType;
    /**
     * 支付类型  10:微信支付, 20:支付宝支付
     */
    @Field(type = FieldType.Integer)
    private Integer payType;
    /**
     * 支付状态 10:未支付,20:已支付
     */
    @Field(type = FieldType.Integer)
    private Integer payStatus;
    /**
     * 支付金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal payAmount;
    /**
     * 支付时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date payTime;
    /**
     * 支付流水号
     */
    @Field(type = FieldType.Keyword)
    private String outTradeNo;
    /**
     * 支付备注信息
     */
    @Field(type = FieldType.Keyword)
    private String payRemark;
}
