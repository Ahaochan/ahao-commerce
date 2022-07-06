package com.ruyuan.eshop.common.message;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Data
public class ActualRefundMessage implements Serializable {
    /**
     * 售后id
     */
    private String afterSaleId;
    /**
     * 订单id
     */
    private String orderId;
    /**
     * 当前订单是否是退最后一笔
     */
    private boolean lastReturnGoods = false;
}
