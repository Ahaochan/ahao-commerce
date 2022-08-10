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
 * es 订单配送信息
 */
@Data
@Document(indexName = "es_order_delivery", createIndex = false)
public class EsOrderDeliveryDetailDO implements EsBaseDO {
    /**
     * esId = orderId
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
     * 配送类型
     */
    @Field(type = FieldType.Integer)
    private Integer deliveryType;
    /**
     * 省
     */
    @Field(type = FieldType.Keyword)
    private String province;
    /**
     * 市
     */
    @Field(type = FieldType.Keyword)
    private String city;
    /**
     * 区
     */
    @Field(type = FieldType.Keyword)
    private String area;
    /**
     * 街道
     */
    @Field(type = FieldType.Keyword)
    private String street;
    /**
     * 详细地址
     */
    @Field(type = FieldType.Keyword)
    private String detailAddress;
    /**
     * 经度
     */
    @Field(type = FieldType.Keyword)
    private BigDecimal lon;
    /**
     * 维度
     */
    @Field(type = FieldType.Keyword)
    private BigDecimal lat;
    /**
     * 收货人姓名
     */
    @Field(type = FieldType.Keyword)
    private String receiverName;
    /**
     * 收货人电话
     */
    @Field(type = FieldType.Keyword)
    private String receiverPhone;
    /**
     * 调整地址次数
     */
    @Field(type = FieldType.Integer)
    private Integer modifyAddressCount;
    /**
     * 配送员编号
     */
    @Field(type = FieldType.Keyword)
    private String delivererNo;
    /**
     * 配送员姓名
     */
    @Field(type = FieldType.Keyword)
    private String delivererName;
    /**
     * 配送员手机号
     */
    @Field(type = FieldType.Keyword)
    private String delivererPhone;
    /**
     * 出库时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date outStockTime;
    /**
     * 签收时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date signedTime;
}
