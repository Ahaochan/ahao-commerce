package moe.ahao.commerce.order.infrastructure.component.statemachine.action.order.create.node;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.infrastructure.component.OrderDataBuilder;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.OrderOperateLogRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.hbase.OrderSnapshotRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.hbase.data.OrderSnapshotDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.data.OrderOperateLogDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.*;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderCancelScheduledTaskMapper;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.service.*;
import moe.ahao.process.engine.core.process.ProcessContext;
import moe.ahao.process.engine.core.process.StandardProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 创建订单落库节点
 */
@Slf4j
@Component
public class OrderCreateDBNode extends StandardProcessor {

    @Autowired
    private OrderInfoMybatisService orderInfoMybatisService;

    @Autowired
    private OrderItemMybatisService orderItemMybatisService;

    @Autowired
    private OrderDeliveryDetailMybatisService orderDeliveryDetailMybatisService;

    @Autowired
    private OrderPaymentDetailMybatisService orderPaymentDetailMybatisService;

    @Autowired
    private OrderAmountMybatisService orderAmountMybatisService;

    @Autowired
    private OrderAmountDetailMybatisService orderAmountDetailMybatisService;

    @Autowired
    private OrderOperateLogRepository orderOperateLogRepository;

    @Autowired
    private OrderSnapshotRepository orderSnapshotRepository;

    @Autowired
    private OrderCancelScheduledTaskMapper orderCancelScheduledTaskMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    protected void processInternal(ProcessContext processContext) {
        // @Transactional无法生效，需要用编程式事务
        transactionTemplate.execute(transactionStatus -> {
            OrderDataBuilder.OrderData orderData = processContext.get("orderData");
            List<OrderDataBuilder.OrderData> allOrderDataList = processContext.get("allOrderDataList");

            // 订单信息
            List<OrderInfoDO> orderInfoList = allOrderDataList.stream().map(OrderDataBuilder.OrderData::getOrderInfo).collect(Collectors.toList());
            if (!orderInfoList.isEmpty()) {
                orderInfoMybatisService.saveBatch(orderInfoList);
            }

            // 订单条目
            List<OrderItemDO> orderItemDOList = allOrderDataList.stream().map(OrderDataBuilder.OrderData::getOrderItemList).flatMap(List::stream).collect(Collectors.toList());
            if (!orderItemDOList.isEmpty()) {
                orderItemMybatisService.saveBatch(orderItemDOList);
            }

            // 订单配送信息
            List<OrderDeliveryDetailDO> orderDeliveryDetailDOList = allOrderDataList.stream().map(OrderDataBuilder.OrderData::getOrderDeliveryDetail).collect(Collectors.toList());
            if (!orderDeliveryDetailDOList.isEmpty()) {
                orderDeliveryDetailMybatisService.saveBatch(orderDeliveryDetailDOList);
            }

            // 订单支付信息
            List<OrderPaymentDetailDO> orderPaymentDetailDOList = allOrderDataList.stream().map(OrderDataBuilder.OrderData::getOrderPaymentDetailList).flatMap(List::stream).collect(Collectors.toList());
            if (!orderPaymentDetailDOList.isEmpty()) {
                orderPaymentDetailMybatisService.saveBatch(orderPaymentDetailDOList);
            }

            // 订单费用信息
            List<OrderAmountDO> orderAmountDOList = allOrderDataList.stream().map(OrderDataBuilder.OrderData::getOrderAmountList).flatMap(List::stream).collect(Collectors.toList());
            if (!orderAmountDOList.isEmpty()) {
                orderAmountMybatisService.saveBatch(orderAmountDOList);
            }

            // 订单费用明细
            List<OrderAmountDetailDO> orderAmountDetailDOList = allOrderDataList.stream().map(OrderDataBuilder.OrderData::getOrderAmountDetailList).flatMap(List::stream).collect(Collectors.toList());
            if (!orderAmountDetailDOList.isEmpty()) {
                orderAmountDetailMybatisService.saveBatch(orderAmountDetailDOList);
            }

            // 插入用于验证订单是否超时的兜底任务记录 只取主单信息 保存主单order记录
            if (orderData != null) {
                OrderInfoDO orderInfoDO = orderData.getOrderInfo();

                OrderCancelScheduledTaskDO orderCancelScheduledTaskDO = new OrderCancelScheduledTaskDO();
                orderCancelScheduledTaskDO.setOrderId(orderInfoDO.getOrderId());
                orderCancelScheduledTaskDO.setExpireTime(orderInfoDO.getExpireTime());
                orderCancelScheduledTaskMapper.insert(orderCancelScheduledTaskDO);
            }

            // 事务提交成功后执行
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    // 订单状态变更日志信息
                    List<OrderOperateLogDO> orderOperateLogDOList = allOrderDataList.stream().map(OrderDataBuilder.OrderData::getOrderOperateLog).collect(Collectors.toList());
                    if (!orderOperateLogDOList.isEmpty()) {
                        orderOperateLogRepository.saveBatch(orderOperateLogDOList);
                    }

                    // 订单快照数据
                    List<OrderSnapshotDO> orderSnapshotDOList = allOrderDataList.stream().map(OrderDataBuilder.OrderData::getOrderSnapshotList).flatMap(List::stream).collect(Collectors.toList());
                    if (!orderSnapshotDOList.isEmpty()) {
                        orderSnapshotRepository.saveBatch(orderSnapshotDOList);
                    }
                }
            });

            return true;
        });
    }
}
