package moe.ahao.commerce.order.api.dto;

import lombok.Data;
import moe.ahao.commerce.aftersale.api.dto.OrderLackItemDTO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 订单详情DTO
 */
@Data
public class OrderDetailDTO {
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
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 订单条目
     */
    private List<OrderItemDTO> orderItems;
    /**
     * 订单费用明细
     */
    private List<OrderAmountDetailDTO> orderAmountDetails;
    /**
     * 订单配送信息表
     */
    private OrderDeliveryDetailDTO orderDeliveryDetail;
    /**
     * 订单支付明细
     */
    private List<OrderPaymentDetailDTO> orderPaymentDetails;

    /**
     * 费用类型
     */
    private Map<Integer, BigDecimal> orderAmounts;

    /**
     * 订单操作日志
     */
    private List<OrderOperateLogDTO> orderOperateLogs;

    /**
     * 订单快照信息
     */
    private List<OrderSnapshotDTO> orderSnapshots;

    /**
     * 订单缺品信息
     */
    private List<OrderLackItemDTO> lackItems;

    /**
     * 订单条目DTO
     */
    @Data
    public static class OrderItemDTO {
        /**
         * 订单id
         */
        private String orderId;
        /**
         * 订单明细id
         */
        private String orderItemId;
        /**
         * 商品类型 1:普通商品,2:预售商品
         */
        private Integer productType;
        /**
         * 商品id
         */
        private String productId;
        /**
         * 商品图片
         */
        private String productImg;
        /**
         * 商品名称
         */
        private String productName;
        /**
         * sku编码
         */
        private String skuCode;
        /**
         * 销售数量
         */
        private BigDecimal saleQuantity;
        /**
         * 销售单价
         */
        private BigDecimal salePrice;
        /**
         * 当前商品支付原总价
         */
        private BigDecimal originAmount;
        /**
         * 交易支付金额
         */
        private BigDecimal payAmount;
        /**
         * 商品单位
         */
        private String productUnit;
        /**
         * 采购成本价
         */
        private BigDecimal purchasePrice;
        /**
         * 卖家id
         */
        private String sellerId;
    }

    /**
     * 订单费用明细表
     */
    @Data
    public static class OrderAmountDetailDTO {
        /**
         * 订单id
         */
        private String orderId;
        /**
         * 产品类型
         */
        private Integer productType;
        /**
         * 订单明细id
         */
        private String orderItemId;
        /**
         * 商品id
         */
        private String productId;
        /**
         * sku编码
         */
        private String skuCode;
        /**
         * 销售数量
         */
        private BigDecimal saleQuantity;
        /**
         * 销售单价
         */
        private BigDecimal salePrice;
        /**
         * 收费类型
         */
        private Integer amountType;
        /**
         * 收费金额
         */
        private BigDecimal amount;
    }

    /**
     * 订单配送信息DTO
     */
    @Data
    public static class OrderDeliveryDetailDTO {
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
    }

    /**
     * 订单支付明细DTO
     */
    @Data
    public static class OrderPaymentDetailDTO {
        /**
         * 订单id
         */
        private String orderId;
        /**
         * 账户类型
         */
        private Integer accountType;
        /**
         * 支付类型  10:微信支付, 20:支付宝支付
         */
        private Integer payType;
        /**
         * 支付状态 10:未支付,20:已支付
         */
        private Integer payStatus;
        /**
         * 支付金额
         */
        private BigDecimal payAmount;
        /**
         * 支付时间
         */
        private Date payTime;
        /**
         * 支付流水号
         */
        private String outTradeNo;
        /**
         * 支付备注信息
         */
        private String payRemark;
    }

    /**
     * 订单快照DTO
     */
    @Data
    public static class OrderSnapshotDTO {
        /**
         * 订单号
         */
        private String orderId;
        /**
         * 订单快照内容
         */
        private String snapshotJson;
    }

    /**
     * 订单操作日志DTO
     */
    @Data
    public static class OrderOperateLogDTO {
        /**
         * 订单id
         */
        private String orderId;
        /**
         * 操作类型
         */
        private Integer operateType;
        /**
         * 前置状态
         */
        private Integer preStatus;
        /**
         * 当前状态
         */
        private Integer currentStatus;
        /**
         * 备注说明
         */
        private String remark;
    }
}
