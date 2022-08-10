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

import static moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO.TABLE_NAME;

/**
 * 订单表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(OrderInfoDO.TABLE_NAME)
@NoArgsConstructor
public class OrderInfoDO extends BaseDO {
    public static final String TABLE_NAME = "order_info";

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
     * 订单id
     */
    private String orderId;
    /**
     * 父订单id
     */
    private String parentOrderId;
    /**
     * 接入方订单id
     */
    private String businessOrderId;
    /**
     * 订单类型 1:一般订单  255:其它
     */
    private Integer orderType;
    /**
     * 订单状态 10:已创建, 30:已履约, 40:出库, 50:配送中, 60:已签收, 70:已取消, 100:已拒收, 255:无效订单
     */
    private Integer orderStatus;
    /**
     * 订单取消类型
     */
    private Integer cancelType;
    /**
     * 订单取消时间
     */
    private Date cancelTime;
    /**
     * 卖家id
     */
    private String sellerId;
    /**
     * 买家id
     */
    private String userId;
    /**
     * 交易总金额（以分为单位存储）
     */
    private BigDecimal totalAmount;
    /**
     * 交易支付金额
     */
    private BigDecimal payAmount;
    /**
     * 交易支付方式
     */
    private Integer payType;
    /**
     * 使用的优惠券id
     */
    private String couponId;
    /**
     * 支付时间
     */
    private Date payTime;
    /**
     * 支付订单截止时间
     */
    private Date expireTime;
    /**
     * 用户备注
     */
    private String userRemark;
    /**
     * 订单删除状态 0:未删除  1:已删除
     */
    private Integer deleteStatus;
    /**
     * 订单评论状态 0:未发表评论  1:已发表评论
     */
    private Integer commentStatus;
    /**
     * 扩展信息
     */
    private String extJson;

    public OrderInfoDO(OrderInfoDO that) {
        this.setId(that.id);
        this.setOrderId(that.orderId);
        this.setParentOrderId(that.parentOrderId);
        this.setBusinessIdentifier(that.businessIdentifier);
        this.setOrderType(that.orderType);
        this.setUserRemark(that.userRemark);
        this.setOrderStatus(that.orderStatus);
        this.setDeleteStatus(that.deleteStatus);
        this.setCommentStatus(that.commentStatus);
        this.setSellerId(that.sellerId);
        this.setUserId(that.userId);
        this.setTotalAmount(that.totalAmount);
        this.setPayAmount(that.payAmount);
        this.setPayType(that.payType);
        this.setCouponId(that.couponId);
        this.setPayTime(that.payTime);
        this.setExpireTime(that.expireTime);
        this.setCancelType(that.cancelType);
        this.setCancelTime(that.cancelTime);
        this.setExtJson(that.extJson);
        this.setCreateBy(that.getCreateBy());
        this.setUpdateBy(that.getUpdateBy());
        this.setCreateTime(that.getCreateTime());
        this.setUpdateTime(that.getUpdateTime());
    }
}
