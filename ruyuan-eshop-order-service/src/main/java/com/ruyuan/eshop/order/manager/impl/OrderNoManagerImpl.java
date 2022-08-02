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
        // 他其实是一个字符串的拼接，这块订单号生成，其实一直都没太大的变化
        // 订单号的生成，yymmdd年月日 + 序列号 + 用户id后三位，订单号里可以反映出来，时间，当天第几个订单，哪个用户来生成的
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
        // 基于数据库的内存双缓冲发号，全局唯一的序号发号
        // 对于这个序号，我们是不能直接拼接在字符串，订单号是对外暴露出去的
        // 导致一个问题，订单号是全局增长的，竞对通过订单号，就可以猜出来你历史上一共有多少订单，泄漏商业机密
        // 混淆加密，数字序号，long，混淆，转换为一个其他的唯一的数字
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