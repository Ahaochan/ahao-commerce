package moe.ahao.commerce.order.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.api.command.GenOrderIdCommand;
import moe.ahao.commerce.common.enums.BusinessIdentifierEnum;
import moe.ahao.commerce.order.infrastructure.component.idgen.SegmentIDGen;
import moe.ahao.commerce.order.infrastructure.enums.OrderIdTypeEnum;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderAutoNoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderAutoNoMapper;
import moe.ahao.commerce.order.infrastructure.utils.ObfuscateNumberHelper;
import moe.ahao.util.commons.lang.time.DateHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 订单号生成AppService
 */
@Slf4j
@Service
public class GenOrderIdAppService {
    @Autowired
    private SegmentIDGen segmentIDGen;

    public String generate(GenOrderIdCommand command) {
        OrderIdTypeEnum orderNoTypeEnum = OrderIdTypeEnum.getByCode(command.getOrderIdType());
        return this.generate(orderNoTypeEnum, command.getUserId());
    }

    /**
     * 19位，2位是业务类型，比如10开头是正向，20开头是逆向，然后中间6位是日期，然后中间8位是序列号，最后3位是用户ID后三位
     * 用户ID不足3位前面补0
     */
    public String generate(OrderIdTypeEnum orderIdTypeEnum, String userId) {
        this.check(orderIdTypeEnum, userId);
        Integer orderIdType = orderIdTypeEnum.getCode();

        // 他其实是一个字符串的拼接，这块订单号生成，其实一直都没太大的变化
        // 订单号的生成，yymmdd年月日 + 序列号 + 用户id后三位，订单号里可以反映出来，时间，当天第几个订单，哪个用户来生成的
        String part1 = String.valueOf(orderIdType);
        String part2 = this.getDateTimeKey();
        String part3 = this.getAutoNoKey(orderIdType);
        String part4 = this.getUserIdKey(userId);
        String orderId = part1 + part2 + part3 + part4;

        return orderId;
    }

    private void check(OrderIdTypeEnum orderIdTypeEnum, String userId) {
        if (StringUtils.isEmpty(userId)) {
            throw OrderExceptionEnum.USER_ID_IS_NULL.msg();
        }
        if (orderIdTypeEnum == null) {
            throw OrderExceptionEnum.ORDER_NO_TYPE_ERROR.msg();
        }
    }

    /**
     * 生成订单号的中间6位日期
     */
    private String getDateTimeKey() {
        return DateHelper.getString(new Date(), "yyMMdd");
    }

    /**
     * 生成订单号中间的8位序列号
     */
    private String getAutoNoKey(Integer orderIdType) {
        // 1. 从数据库取号
        Long autoNo = segmentIDGen.genNewNo(orderIdType.toString());

        // 2. 混淆
        long obfuscateNumber = ObfuscateNumberHelper.genNo(autoNo, 8);
        String orderId = String.valueOf(obfuscateNumber);
        return orderId;
    }

    /**
     * 截取用户ID的后三位
     */
    private String getUserIdKey(String userId) {
        // 1. 如果userId的长度大于或等于3，则直接返回
        if (userId.length() >= 3) {
            return userId.substring(userId.length() - 3);
        } else if (userId.length() == 2) {
            // 2. 如果userId的长度小于3，则直接前面补0
            return "0" + userId;
        } else if (userId.length() == 1) {
            return "00" + userId;
        } else {
            throw OrderExceptionEnum.USER_ID_IS_NULL.msg();
        }
    }
}
