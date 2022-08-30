package com.ruyuan.eshop.order.statemachine.action.order.create.node;

import cn.hutool.json.JSONUtil;
import com.ruyuan.eshop.common.utils.LoggerFormat;
import com.ruyuan.eshop.order.builder.FullOrderData;
import com.ruyuan.eshop.order.converter.OrderConverter;
import com.ruyuan.eshop.order.dao.*;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.entity.*;
import com.ruyuan.eshop.order.hbase.entity.OrderInfoExtJsonDTO;
import com.ruyuan.eshop.order.service.impl.NewOrderDataHolder;
import com.ruyuan.process.engine.process.ProcessContext;
import com.ruyuan.process.engine.process.StandardProcessor;
import lombok.extern.slf4j.Slf4j;
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
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
public class OrderCreateDBNode extends StandardProcessor {

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private OrderAmountDAO orderAmountDAO;

    @Autowired
    private OrderAmountDetailDAO orderAmountDetailDAO;

    @Autowired
    private OrderOperateLogDAO orderOperateLogDAO;

    @Autowired
    private OrderSnapshotDAO orderSnapshotDAO;

    @Autowired
    private OrderCancelScheduledTaskDAO orderCancelScheduledTaskDAO;

    @Autowired
    private OrderConverter orderConverter;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    protected void processInternal(ProcessContext processContext) {
        //  @Transactional无法生效，需要用编程式事务
        transactionTemplate.execute(transactionStatus -> {
            FullOrderData fullOrderData = processContext.get("fullMasterOrderData");
            NewOrderDataHolder newOrderDataHolder = processContext.get("newOrderDataHolder");
            FullOrderData fullSubOrderData = processContext.get("fullSubOrderData");

            // 获取主单或者子单
            boolean isSubOrderCreate = fullSubOrderData != null;
            OrderInfoDO order = null;
            if(!isSubOrderCreate) {
                order = fullOrderData.getOrderInfoDO();
            }
            else {
                order = fullSubOrderData.getOrderInfoDO();
            }

            String orderId = fullOrderData.getOrderInfoDO().getOrderId();
            // 订单信息
            List<OrderInfoDO> orderInfoDOList = newOrderDataHolder.getOrderInfoDOList();
            if (!orderInfoDOList.isEmpty()) {
                log.info(LoggerFormat.build()
                        .remark("保存订单信息")
                        .data("orderId", orderId)
                        .finish());
                orderInfoDAO.saveBatch(orderInfoDOList);
            }

            // 订单条目
            List<OrderItemDO> orderItemDOList = newOrderDataHolder.getOrderItemDOList();
            if (!orderItemDOList.isEmpty()) {
                log.info(LoggerFormat.build()
                        .remark("保存订单条目")
                        .data("orderId", orderId)
                        .finish());
                orderItemDAO.saveBatch(orderItemDOList);
            }

            // 订单配送信息
            List<OrderDeliveryDetailDO> orderDeliveryDetailDOList = newOrderDataHolder.getOrderDeliveryDetailDOList();
            if (!orderDeliveryDetailDOList.isEmpty()) {
                log.info(LoggerFormat.build()
                        .remark("保存订单配送信息")
                        .data("orderId", orderId)
                        .finish());
                orderDeliveryDetailDAO.saveBatch(orderDeliveryDetailDOList);
            }

            // 订单支付信息
            List<OrderPaymentDetailDO> orderPaymentDetailDOList = newOrderDataHolder.getOrderPaymentDetailDOList();
            if (!orderPaymentDetailDOList.isEmpty()) {
                log.info(LoggerFormat.build()
                        .remark("保存订单支付信息")
                        .data("orderId", orderId)
                        .finish());
                orderPaymentDetailDAO.saveBatch(orderPaymentDetailDOList);
            }

            // 订单费用信息
            List<OrderAmountDO> orderAmountDOList = newOrderDataHolder.getOrderAmountDOList();
            if (!orderAmountDOList.isEmpty()) {
                log.info(LoggerFormat.build()
                        .remark("保存订单费用信息")
                        .data("orderId", orderId)
                        .finish());
                orderAmountDAO.saveBatch(orderAmountDOList);
            }

            // 订单费用明细
            List<OrderAmountDetailDO> orderAmountDetailDOList = newOrderDataHolder.getOrderAmountDetailDOList();
            if (!orderAmountDetailDOList.isEmpty()) {
                log.info(LoggerFormat.build()
                        .remark("保存订单费用明细")
                        .data("orderId", orderId)
                        .finish());
                orderAmountDetailDAO.saveBatch(orderAmountDetailDOList);
            }

            // 插入用于验证订单是否超时的兜底任务记录 只取主单信息 保存主单order记录
            List<OrderInfoDO> masterOrderInfoDOList = orderInfoDOList.stream()
                    .filter(orderInfoDO -> orderInfoDO.getParentOrderId() == null).collect(Collectors.toList());
            if (!masterOrderInfoDOList.isEmpty()) {
                log.info(LoggerFormat.build()
                        .remark("保存定时执行订单取消兜底任务记录")
                        .data("orderId", orderId)
                        .finish());
                OrderInfoDO orderInfoDO = masterOrderInfoDOList.get(0);
                orderCancelScheduledTaskDAO.saveOne(orderInfoDO.getOrderId(), orderInfoDO.getExpireTime());
            }

            OrderInfoDTO orderInfoDTO = orderConverter.orderInfoDO2DTO(order);
            processContext.set("orderInfoDTO", orderInfoDTO);

            // 事务提交成功后执行
            OrderInfoDO finalOrder = order;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                  @Override
                  public void afterCommit() {
                  // 订单状态变更日志信息
                  List<OrderOperateLogDO> orderOperateLogDOList = newOrderDataHolder.getOrderOperateLogDOList();
                  if (!orderOperateLogDOList.isEmpty()) {
                      log.info(LoggerFormat.build()
                              .remark("保存订单状态变更日志信息")
                              .data("orderId", orderId)
                              .finish());
                      orderOperateLogDAO.batchSave(orderOperateLogDOList);
                  }
                  // 订单快照数据
                  List<OrderSnapshotDO> orderSnapshotDOList = newOrderDataHolder.getOrderSnapshotDOList();
                  if (!orderSnapshotDOList.isEmpty()) {
                      log.info(LoggerFormat.build()
                              .remark("保存订单快照数据")
                              .data("orderId", orderId)
                              .finish());

                      orderSnapshotDAO.batchSave(orderSnapshotDOList, OrderSnapshotDAOUtils.getRowKeyPrefixList(finalOrder));
                  }
                  }
              }
            );

            return true;
        });
    }
}
