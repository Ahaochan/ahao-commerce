package moe.ahao.commerce.fulfill.adapter.schedule;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.application.PreSaleOrderFulfillAppService;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillStatusEnum;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillTypeEnum;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillItemDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillItemMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillMapper;
import moe.ahao.commerce.product.api.dto.ProductSkuDTO;
import moe.ahao.util.commons.io.JSONHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 触发预售商品履约的定时任务
 */
@Slf4j
@Component
public class PreSaleOrderFulfillScheduleTriggerTask {

    @Autowired
    private OrderFulfillMapper orderFulfillMapper;

    @Autowired
    private OrderFulfillItemMapper orderFulfillItemMapper;

    @Autowired
    private PreSaleOrderFulfillAppService preSaleOrderFulfillAppService;

    /**
     * 执行任务逻辑
     */
    @XxlJob("preSaleOrderFulfillScheduleTriggerTask")
    public void execute() {
        // 1. 查询未履约预售单的候选集
        // TODO 真实的生产环境，还需要加上截止时间进行过滤，具体可以参考订单系统取消订单定时任务
        List<OrderFulfillDO> list = orderFulfillMapper.selectListByFulfillTypeAndStatus(OrderFulfillTypeEnum.PRE_SALE.getCode(), OrderFulfillStatusEnum.FULFILL.getCode());
        log.info("查询未履约预售单的候选集: list={}", JSONHelper.toString(list));
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (OrderFulfillDO orderFulfill : list) {
            try {
                ProductSkuDTO.PreSaleInfoDTO preSaleInfoDTO = JSONHelper.parse(orderFulfill.getExtJson(), ProductSkuDTO.PreSaleInfoDTO.class);
                if (new Date().getTime() >= preSaleInfoDTO.getPreSaleTime().getTime()) {
                    // 2. 触发预售商品履约调度
                    ReceiveFulfillCommand command = new ReceiveFulfillCommand();
                    // TODO 填充Command
                    preSaleOrderFulfillAppService.fulfill(command);
                }
            } catch (Exception e) {
                log.error("触发预售商品履约调度失败，err={}", e.getMessage(), e);
            }
        }
        XxlJobHelper.handleSuccess();
    }
}
