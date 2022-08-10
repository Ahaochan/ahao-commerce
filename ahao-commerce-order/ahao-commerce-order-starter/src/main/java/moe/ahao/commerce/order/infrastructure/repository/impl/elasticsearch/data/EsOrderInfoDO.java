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
 * es 订单
 */
@Data
@Document(indexName = "es_order_info", createIndex = false)
public class EsOrderInfoDO implements EsBaseDO {
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
     * 接入方业务线标识  1, "自营商城"
     */
    @Field(type = FieldType.Integer)
    private Integer businessIdentifier;
    /**
     * 订单编号
     */
    @Field(type = FieldType.Keyword)
    private String orderId;
    /**
     * 父订单编号
     */
    @Field(type = FieldType.Keyword)
    private String parentOrderId;
    /**
     * 接入方订单号
     */
    @Field(type = FieldType.Keyword)
    private String businessOrderId;
    /**
     * 订单类型 1:一般订单  255:其它
     */
    @Field(type = FieldType.Integer)
    private Integer orderType;
    /**
     * 订单状态 10:已创建, 30:已履约, 40:出库, 50:配送中, 60:已签收, 70:已取消, 100:已拒收, 255:无效订单
     */
    @Field(type = FieldType.Integer)
    private Integer orderStatus;
    /**
     * 订单取消类型
     */
    @Field(type = FieldType.Keyword)
    private Integer cancelType;
    /**
     * 订单取消时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date cancelTime;
    /**
     * 卖家编号
     */
    @Field(type = FieldType.Keyword)
    private String sellerId;
    /**
     * 买家编号
     */
    @Field(type = FieldType.Keyword)
    private String userId;
    /**
     * 交易总金额（以分为单位存储）
     */
    @Field(type = FieldType.Double)
    private BigDecimal totalAmount;
    /**
     * 交易支付金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal payAmount;
    /**
     * 交易支付方式
     */
    @Field(type = FieldType.Integer)
    private Integer payType;
    /**
     * 使用的优惠券编号
     */
    @Field(type = FieldType.Keyword)
    private String couponId;
    /**
     * 支付时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date payTime;
    /**
     * 支付订单截止时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd HH:mm:ss")
    private Date expireTime;
    /**
     * 用户备注
     */
    @Field(type = FieldType.Text)
    private String userRemark;
    /**
     * 订单删除状态 0:未删除  1:已删除
     */
    @Field(type = FieldType.Integer)
    private Integer deleteStatus;
    /**
     * 订单评论状态 0:未发表评论  1:已发表评论
     */
    @Field(type = FieldType.Integer)
    private Integer commentStatus;
    /**
     * 扩展信息
     */
    @Field(type = FieldType.Keyword)
    private String extJson;
}
