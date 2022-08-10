package moe.ahao.commerce.aftersale.api.query;

import lombok.Data;
import moe.ahao.commerce.aftersale.api.enums.AfterSaleQueryDataTypeEnums;
import moe.ahao.commerce.aftersale.api.enums.AfterSaleQuerySortField;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

@Data
public class AfterSalePageQuery {
    public static final Integer MAX_PAGE_SIZE = 100;

    /**
     * 接入方业务线标识  1, "自营商城"
     */
    private Integer businessIdentifier;
    /**
     * 订单类型
     */
    private Set<Integer> orderTypes;
    /**
     * 售后单状态
     */
    private Set<Integer> afterSaleStatus;
    /**
     * 售后申请来源
     */
    private Set<Integer> applySources;
    /**
     * 售后类型
     */
    private Set<Integer> afterSaleTypes;
    /**
     * 售后单号
     */
    private Set<String> afterSaleIds;
    /**
     * 订单号
     */
    private Set<String> orderIds;
    /**
     * 用户ID
     */
    private Set<String> userIds;
    /**
     * sku code
     */
    private Set<String> skuCodes;
    /**
     * 创建时间-查询区间
     */
    private Date queryStartCreatedTime;
    private Date queryEndCreatedTime;

    /**
     * 售后申请时间-查询区间
     */
    private Date queryStartApplyTime;
    private Date queryEndApplyTime;
    /**
     * 售后客服审核时间-查询区间
     */
    private Date queryStartReviewTime;
    private Date queryEndReviewTime;
    /**
     * 退款支付时间-查询区间
     */
    private Date queryStartRefundPayTime;
    private Date queryEndRefundPayTime;
    /**
     * 退款金额-查询区间
     */
    private BigDecimal queryStartRefundAmount;
    private BigDecimal queryEndRefundAmount;

    /**
     * 排序
     */
    private AfterSaleQuerySortField sort = AfterSaleQuerySortField.CREATE_TIME_DESC;
    /**
     * 售后单项查询枚举
     */
    private AfterSaleQueryDataTypeEnums[] queryDataTypes;

    /**
     * 页码；默认为1；
     */
    private Integer pageNo = 1;
    /**
     * 每页条数. 默认为20
     */
    private Integer pageSize = 20;
}
