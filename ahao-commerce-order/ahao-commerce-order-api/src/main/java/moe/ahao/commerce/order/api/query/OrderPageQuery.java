package moe.ahao.commerce.order.api.query;

import lombok.Data;
import moe.ahao.commerce.order.api.enums.OrderQueryDataTypeEnums;
import moe.ahao.commerce.order.api.enums.OrderQuerySortField;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

/**
 * 接收 订单列表查询请求的入参
 */
@Data
public class OrderPageQuery implements Serializable {
    public static final Integer MAX_PAGE_SIZE = 100;

    /**
     * 业务线
     */
    private Integer businessIdentifier;
    /**
     * 订单类型
     */
    private Set<Integer> orderTypes;
    /**
     * 订单号
     */
    private Set<String> orderIds;
    /**
     * 卖家ID
     */
    private Set<String> sellerIds;
    /**
     * 父订单号
     */
    private Set<String> parentOrderIds;
    /**
     * 用户ID
     */
    private Set<String> userIds;
    /**
     * 订单状态
     */
    private Set<Integer> orderStatus;
    /**
     * 收货人手机号
     */
    private Set<String> receiverPhones;
    /**
     * 收货人姓名
     */
    private Set<String> receiverNames;
    /**
     * 交易流水号
     */
    private Set<String> tradeNos;

    /**
     * sku code
     */
    private Set<String> skuCodes;
    /**
     * sku商品名称
     */
    private Set<String> productNames;

    /**
     * 查询创建时间的区间范围
     */
    private Date queryStartCreatedTime;

    private Date queryEndCreatedTime;

    /**
     * 查询支付时间的区间范围
     */
    private Date queryStartPayTime;

    private Date queryEndPayTime;

    /**
     * 查询支付金额的区间范围
     */
    private BigDecimal queryStartPayAmount;

    private BigDecimal queryEndPayAmount;

    /**
     * 排序
     */
    private OrderQuerySortField sort = OrderQuerySortField.CREATE_TIME_DESC;
    /**
     * 订单项查询枚举
     */
    private OrderQueryDataTypeEnums[] queryDataTypes;

    /**
     * 页码
     */
    private Integer pageNo = 1;
    /**
     * 每页条数
     */
    private Integer pageSize = 20;
}
