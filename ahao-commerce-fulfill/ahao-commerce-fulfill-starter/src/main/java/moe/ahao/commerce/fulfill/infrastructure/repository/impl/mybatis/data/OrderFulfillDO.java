package moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import moe.ahao.domain.entity.BaseDO;

import java.math.BigDecimal;

/**
 * 订单履约表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_fulfill")
@NoArgsConstructor
public class OrderFulfillDO extends BaseDO {
    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 接入方业务线标识  1, "自营商城"
     */
    private Integer businessIdentifier;
    /**
     * 履约单id
     */
    private String fulfillId;
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 商家id
     */
    private String sellerId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 履约单状态
     */
    private Integer status;
    /**
     * 履约单类型
     */
    private Integer orderFulfillType;
    /**
     * 配送类型，默认是自配送
     */
    private Integer deliveryType;
    /**
     * 收货人姓名
     */
    private String receiverName;
    /**
     * 收货人电话
     */
    private String receiverPhone;
    /**
     * 省
     */
    private String receiverProvince;
    /**
     * 市
     */
    private String receiverCity;
    /**
     * 区
     */
    private String receiverArea;
    /**
     * 街道地址
     */
    private String receiverStreet;
    /**
     * 详细地址
     */
    private String receiverDetailAddress;
    /**
     * 经度 六位小数点
     */
    private BigDecimal receiverLon;
    /**
     * 纬度 六位小数点
     */
    private BigDecimal receiverLat;
    /**
     * 配送员编号
     */
    private String delivererNo;
    /**
     * 配送员姓名
     */
    private String delivererName;
    /**
     * 配送员手机号
     */
    private String delivererPhone;
    /**
     * 物流单号
     */
    private String logisticsCode;
    /**
     * 用户备注
     */
    private String userRemark;
    /**
     * 支付方式
     */
    private Integer payType;
    /**
     * 付款总金额
     */
    private BigDecimal payAmount;
    /**
     * 交易总金额
     */
    private BigDecimal totalAmount;
    /**
     * 运费
     */
    private BigDecimal deliveryAmount;
    /**
     * 扩展字段
     */
    private String extJson;

    public OrderFulfillDO(OrderFulfillDO that) {
        this.setId(that.id);
        this.setBusinessIdentifier(that.businessIdentifier);
        this.setFulfillId(that.fulfillId);
        this.setOrderId(that.orderId);
        this.setSellerId(that.sellerId);
        this.setUserId(that.userId);
        this.setStatus(that.status);
        this.setOrderFulfillType(that.orderFulfillType);
        this.setDeliveryType(that.deliveryType);
        this.setReceiverName(that.receiverName);
        this.setReceiverPhone(that.receiverPhone);
        this.setReceiverProvince(that.receiverProvince);
        this.setReceiverCity(that.receiverCity);
        this.setReceiverArea(that.receiverArea);
        this.setReceiverStreet(that.receiverStreet);
        this.setReceiverDetailAddress(that.receiverDetailAddress);
        this.setReceiverLon(that.receiverLon);
        this.setReceiverLat(that.receiverLat);
        this.setDelivererNo(that.delivererNo);
        this.setDelivererName(that.delivererName);
        this.setDelivererPhone(that.delivererPhone);
        this.setLogisticsCode(that.logisticsCode);
        this.setUserRemark(that.userRemark);
        this.setPayType(that.payType);
        this.setPayAmount(that.payAmount);
        this.setTotalAmount(that.totalAmount);
        this.setDeliveryAmount(that.deliveryAmount);
        this.setExtJson(that.extJson);
        this.setCreateBy(that.getCreateBy());
        this.setUpdateBy(that.getUpdateBy());
        this.setCreateTime(that.getCreateTime());
        this.setUpdateTime(that.getUpdateTime());
    }
}
