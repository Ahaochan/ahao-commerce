package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import moe.ahao.domain.entity.BaseDO;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单配送信息表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(OrderDeliveryDetailDO.TABLE_NAME)
@NoArgsConstructor
public class OrderDeliveryDetailDO extends BaseDO {
    public static final String TABLE_NAME = "order_delivery_detail";

    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 配送类型
     */
    private Integer deliveryType;
    /**
     * 省
     */
    private String province;
    /**
     * 市
     */
    private String city;
    /**
     * 区
     */
    private String area;
    /**
     * 街道
     */
    private String street;
    /**
     * 详细地址
     */
    private String detailAddress;
    /**
     * 经度
     */
    private BigDecimal lon;
    /**
     * 维度
     */
    private BigDecimal lat;
    /**
     * 收货人姓名
     */
    private String receiverName;
    /**
     * 收货人电话
     */
    private String receiverPhone;
    /**
     * 调整地址次数
     */
    private Integer modifyAddressCount;
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
     * 出库时间
     */
    private Date outStockTime;
    /**
     * 签收时间
     */
    private Date signedTime;

    public OrderDeliveryDetailDO(OrderDeliveryDetailDO that) {
        this.setId(that.id);
        this.setOrderId(that.orderId);
        this.setDeliveryType(that.deliveryType);
        this.setProvince(that.province);
        this.setCity(that.city);
        this.setArea(that.area);
        this.setStreet(that.street);
        this.setDetailAddress(that.detailAddress);
        this.setLon(that.lon);
        this.setLat(that.lat);
        this.setReceiverName(that.receiverName);
        this.setReceiverPhone(that.receiverPhone);
        this.setModifyAddressCount(that.modifyAddressCount);
        this.setDelivererNo(that.delivererNo);
        this.setDelivererName(that.delivererName);
        this.setDelivererPhone(that.delivererPhone);
        this.setOutStockTime(that.outStockTime);
        this.setSignedTime(that.signedTime);
        this.setCreateBy(that.getCreateBy());
        this.setUpdateBy(that.getUpdateBy());
        this.setCreateTime(that.getCreateTime());
        this.setUpdateTime(that.getUpdateTime());
    }
}
