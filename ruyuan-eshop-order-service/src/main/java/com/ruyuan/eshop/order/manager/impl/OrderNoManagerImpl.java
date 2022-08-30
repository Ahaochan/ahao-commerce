package com.ruyuan.eshop.order.manager.impl;

import com.ruyuan.eshop.common.utils.DateFormatUtil;
import com.ruyuan.eshop.common.utils.NumberUtil;
import com.ruyuan.eshop.order.enums.OrderNoTypeEnum;
import com.ruyuan.eshop.order.exception.OrderBizException;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.generator.SegmentIDGen;
import com.ruyuan.eshop.order.manager.OrderNoManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class OrderNoManagerImpl implements OrderNoManager {

    @Autowired
    private SegmentIDGen segmentIDGen;

    /**
     * 19位，2位是业务类型，比如10开头是正向，20开头是逆向，然后中间6位是日期，然后中间8位是序列号，最后3位是用户ID后三位
     * 用户ID不足3位前面补0
     */
    @Override
    public String genOrderId(Integer orderNoType, String userId) {
        // 检查orderNoType是否正确
        OrderNoTypeEnum orderNoTypeEnum = OrderNoTypeEnum.getByCode(orderNoType);
        if (orderNoTypeEnum == null) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_NO_TYPE_ERROR);
        }
        return orderNoType + getOrderIdKey(orderNoType, userId);
    }

    /**
     * 获取订单ID
     */
    private String getOrderIdKey(Integer orderNoType, String userId) {
        return getDateTimeKey() + getAutoNoKey(orderNoType) + getUserIdKey(userId);
    }

    /**
     * 生成订单号的中间6位日期
     */
    private String getDateTimeKey() {
        return DateFormatUtil.format(new Date(), "yyMMdd");
    }

    /**
     * 生成订单号中间的8位序列号
     */
    private String getAutoNoKey(Integer orderNoType) {
        Long autoNo = segmentIDGen.genNewNo(orderNoType.toString());
        return String.valueOf(NumberUtil.genNo(autoNo, 8));
    }

    /**
     * 截取用户ID的后三位
     */
    private String getUserIdKey(String userId) {
        // 如果userId的长度大于或等于3，则直接返回
        if (userId.length() >= 3) {
            return userId.substring(userId.length() - 3);
        }

        // 如果userId的长度大于或等于3，则直接前面补0
        StringBuilder userIdKey = new StringBuilder(userId);
        while (userIdKey.length() != 3) {
            userIdKey.insert(0, "0");
        }
        return userIdKey.toString();
    }

}