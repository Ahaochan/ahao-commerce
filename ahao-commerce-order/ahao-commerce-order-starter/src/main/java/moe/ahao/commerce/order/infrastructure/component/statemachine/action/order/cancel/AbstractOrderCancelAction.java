package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.cancel;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.enums.OrderOperateTypeEnum;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.common.enums.OrderStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.OrderStateAction;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.OrderOperateLogRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.data.OrderOperateLogDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * 订单取消抽象Action
 */
@Slf4j
public abstract class AbstractOrderCancelAction extends OrderStateAction<OrderInfoDO> {
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderOperateLogRepository orderOperateLogRepository;

    @Override
    protected String onStateChangeInternal(OrderStatusChangeEnum event, OrderInfoDO masterOrderInfo) {
        // 1. 查询全部订单
        String masterOrderId = masterOrderInfo.getOrderId();
        List<OrderInfoDO> subOrderInfoList = orderInfoMapper.selectListByParentOrderId(masterOrderId);
        List<OrderInfoDO> allOrderInfoList = ListUtils.union(Collections.singletonList(masterOrderInfo), subOrderInfoList);

        // 2. 封装操作数据
        List<String> orderIdList = new ArrayList<>();
        List<OrderOperateLogDO> orderOperateLogList = new ArrayList<>();
        for (OrderInfoDO orderInfo : allOrderInfoList) {
            Integer fromStatus = orderInfo.getOrderStatus();
            Integer toStatus = OrderStatusEnum.CANCELLED.getCode();

            // 更新订单数据
            orderInfo.setCancelType(orderInfo.getCancelType());
            orderInfo.setOrderStatus(toStatus);
            orderInfo.setCancelTime(new Date());
            orderIdList.add(orderInfo.getOrderId());

            // 订单操作日志数据
            OrderOperateLogDO orderOperateLog = new OrderOperateLogDO();
            orderOperateLog.setOrderId(orderInfo.getOrderId());
            orderOperateLog.setPreStatus(fromStatus);
            orderOperateLog.setCurrentStatus(toStatus);
            OrderOperateTypeEnum orderOperateTypeEnum = event().getOperateType();
            orderOperateLog.setOperateType(orderOperateTypeEnum.getCode());
            orderOperateLog.setRemark(orderOperateTypeEnum.getName() + fromStatus + "-" + toStatus);
            orderOperateLogList.add(orderOperateLog);
        }

        // 3. 更新订单状态为已取消
        orderInfoMapper.updateCancelInfoByOrderIds(orderIdList, masterOrderInfo.getCancelType(), OrderStatusEnum.CANCELLED.getCode(), new Date());
        log.info("更新订单信息OrderInfo状态: masterOrderId:{}, status:{}", masterOrderId, masterOrderInfo.getOrderStatus());

        // 4. 新增订单操作操作日志表
        orderOperateLogRepository.saveBatch(orderOperateLogList);
        log.info("新增订单操作日志OrderOperateLog状态, masterOrderId:{}", masterOrderId);

        return masterOrderId;
    }
}
