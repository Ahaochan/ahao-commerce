package moe.ahao.commerce.wms.api.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 捡货请求
 */
@Data
public class PickGoodsCommand {
    /**
     * 接入方业务线标识  1, "自营商城"
     */
    private Integer businessIdentifier;
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
     * 订单商品明细
     */
    private List<OrderItem> orderItems;
    /**
     * 用于模拟wms服务异常
     */
    private String wmsException;

    /**
     * 订单商品明细请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        /**
         * 商品sku
         */
        private String skuCode;
        /**
         * 商品名称
         */
        private String productName;
        /**
         * 销售单价
         */
        private BigDecimal salePrice;
        /**
         * 销售数量
         */
        private BigDecimal saleQuantity;
        /**
         * 商品单位
         */
        private String productUnit;
        /**
         * 付款金额
         */
        private BigDecimal payAmount;
        /**
         * 当前商品支付原总价
         */
        private BigDecimal originAmount;
    }
}
