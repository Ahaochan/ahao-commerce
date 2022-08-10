package moe.ahao.commerce.fulfill.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.fulfill.api.command.CancelFulfillCommand;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillOperateTypeEnum;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillStatusEnum;
import moe.ahao.commerce.fulfill.infrastructure.gateway.TmsGateway;
import moe.ahao.commerce.fulfill.infrastructure.gateway.WmsGateway;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillLogDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillItemMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillLogMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.service.OrderFulfillLogMyBatisService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CancelFulfillAppService {
    @Autowired
    private OrderFulfillMapper orderFulfillMapper;
    @Autowired
    private OrderFulfillItemMapper orderFulfillItemMapper;
    @Autowired
    private OrderFulfillLogMyBatisService orderFulfillLogMyBatisService;

    @Autowired
    private WmsGateway wmsGateway;
    @Autowired
    private TmsGateway tmsGateway;

    public boolean cancelFulfillAndWmsAndTms(CancelFulfillCommand command) {
        // 1. 取消履约单
        this.cancelFulfill(command.getOrderId());

        // 2. 取消捡货
        // wmsGateway.cancelPickGoods(command.getOrderId());

        // 3. 取消发货
        // tmsGateway.cancelSendOut(command.getOrderId());

        return true;
    }

    public boolean cancelFulfill(String orderId) {
        // 1. 查询履约单
        List<OrderFulfillDO> orderFulfills = orderFulfillMapper.selectListByOrderId(orderId);
        if(CollectionUtils.isEmpty(orderFulfills)) {
            return false;
        }
        for (OrderFulfillDO orderFulfill : orderFulfills) {
            if (OrderFulfillStatusEnum.notCancelStatus().contains(orderFulfill.getStatus())) {
                log.info("订单无法取消履约, 存在履约单已出库配送了, orderId={}", orderId);
                return false;
            }
        }

        // 2. 取消履约
        orderFulfillMapper.updateFulfillStatusByOrderId(orderId, OrderFulfillStatusEnum.FULFILL.getCode(), OrderFulfillStatusEnum.CANCELLED.getCode());

        // 3. 添加履约单变更记录
        List<OrderFulfillLogDO> logs = new ArrayList<>(orderFulfills.size());
        for (OrderFulfillDO orderFulfill : orderFulfills) {
            OrderFulfillOperateTypeEnum operateTypeEnum = OrderFulfillOperateTypeEnum.CANCEL_ORDER;
            OrderFulfillLogDO log = new OrderFulfillLogDO();
            log.setOrderId(orderFulfill.getOrderId());
            log.setFulfillId(orderFulfill.getFulfillId());
            log.setOperateType(operateTypeEnum.getCode());
            log.setPreStatus(operateTypeEnum.getFromStatus().getCode());
            log.setCurrentStatus(operateTypeEnum.getToStatus().getCode());
            log.setRemark(operateTypeEnum.getMsg());

            logs.add(log);
        }
        orderFulfillLogMyBatisService.saveBatch(logs);

        // TODO 如果取消履约成功, 也需要发送一个消息来更新订单状态为已取消, 避免取消履约成功, 更新订单状态失败
        return true;
    }
}
