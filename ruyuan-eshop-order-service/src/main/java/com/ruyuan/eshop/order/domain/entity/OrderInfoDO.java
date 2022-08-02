package com.ruyuan.eshop.order.domain.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;
import com.ruyuan.eshop.order.elasticsearch.annotation.EsField;
import com.ruyuan.eshop.order.elasticsearch.enums.EsDataTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 订单表
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
@TableName("order_info")
public class OrderInfoDO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @EsField(type = EsDataTypeEnum.KEYWORD)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @EsField(type = EsDataTypeEnum.DATE)
    private Date gmtCreate;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @EsField(type = EsDataTypeEnum.DATE)
    private Date gmtModified;

    /**
     * 接入方业务线标识  1, "自营商城"
     */
    @EsField(type = EsDataTypeEnum.INTEGER)
    private Integer businessIdentifier;

    /**
     * 订单编号
     */
    @EsField(type = EsDataTypeEnum.KEYWORD)
    private String orderId;

    /**
     * 父订单编号
     */
    @EsField(type = EsDataTypeEnum.KEYWORD)
    private String parentOrderId;

    /**
     * 接入方订单号
     */
    @EsField(type = EsDataTypeEnum.KEYWORD)
    private String businessOrderId;

    /**
     * 订单类型 1:一般订单  255:其它
     */
    @EsField(type = EsDataTypeEnum.INTEGER)
    private Integer orderType;

    /**
     * 订单状态 10:已创建, 30:已履约, 40:出库, 50:配送中, 60:已签收, 70:已取消, 100:已拒收, 255:无效订单
     */
    @EsField(type = EsDataTypeEnum.INTEGER)
    private Integer orderStatus;

    /**
     * 订单取消类型
     */
    @EsField(type = EsDataTypeEnum.KEYWORD)
    private String cancelType;

    /**
     * 订单取消时间
     */
    @EsField(type = EsDataTypeEnum.DATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date cancelTime;

    /**
     * 卖家编号
     */
    @EsField(type = EsDataTypeEnum.KEYWORD)
    private String sellerId;

    /**
     * 买家编号
     */
    @EsField(type = EsDataTypeEnum.KEYWORD)
    private String userId;

    /**
     * 交易总金额（以分为单位存储）
     */
    @EsField(type = EsDataTypeEnum.INTEGER)
    private Integer totalAmount;

    /**
     * 交易支付金额
     */
    @EsField(type = EsDataTypeEnum.INTEGER)
    private Integer payAmount;

    /**
     * 交易支付方式
     */
    @EsField(type = EsDataTypeEnum.INTEGER)
    private Integer payType;

    /**
     * 使用的优惠券编号
     */
    @EsField(type = EsDataTypeEnum.KEYWORD)
    private String couponId;

    /**
     * 支付时间
     */
    @EsField(type = EsDataTypeEnum.DATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /**
     * 支付订单截止时间
     */
    @EsField(type = EsDataTypeEnum.DATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /**
     * 用户备注
     */
    @EsField(type = EsDataTypeEnum.TEXT)
    private String userRemark;

    /**
     * 订单删除状态 0:未删除  1:已删除
     */
    @EsField(type = EsDataTypeEnum.INTEGER)
    private Integer deleteStatus;

    /**
     * 订单评论状态 0:未发表评论  1:已发表评论
     */
    @EsField(type = EsDataTypeEnum.INTEGER)
    private Integer commentStatus;

    /**
     * 扩展信息
     */
    @EsField(type = EsDataTypeEnum.KEYWORD)
    private String extJson;
}
