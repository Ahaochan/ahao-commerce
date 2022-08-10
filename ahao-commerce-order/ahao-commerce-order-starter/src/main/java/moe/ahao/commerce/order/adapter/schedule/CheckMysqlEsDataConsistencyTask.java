package moe.ahao.commerce.order.adapter.schedule;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.adapter.mq.handler.EsOrderSyncHandler;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.OrderListEsRepository;
import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data.EsOrderListQueryDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 检测mysql中订单数据与es中的订单状态数据的一致性
 */
@Slf4j
@Component
public class CheckMysqlEsDataConsistencyTask {
    /**
     * 订单信息的DAO组件
     */
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderListEsRepository orderListEsRepository;
    @Autowired
    private EsOrderSyncHandler esOrderSyncHandler;

    /**
     * 检测mysql中订单数据与es中的订单状态数据的一致性
     */
    @XxlJob("checkMysqlEsDataConsistencyTask")
    public void checkMysqlEsDataConsistencyTask() throws Exception {
        // 查询出当前时间-25分钟 到 当前时间-5分钟 区间内的所有订单
        Date startDate = DateUtils.addMinutes(new Date(), -25);
        Date endDate = DateUtils.addMinutes(new Date(), -5);
        Wrapper<OrderInfoDO> queryWrapper = new LambdaQueryWrapper<OrderInfoDO>()
            .between(OrderInfoDO::getCreateTime, startDate, endDate);
        List<OrderInfoDO> orderInfoList = orderInfoMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(orderInfoList)) {
            return;
        }

        // 检查数据库与es中的订单状态是否一致
        List<OrderInfoDO> diffOrderInfoList = this.getDiffOrderInfoList(orderInfoList);
        // 如果检测结果为true且集合不为空
        if (CollectionUtils.isNotEmpty(diffOrderInfoList)) {
            esOrderSyncHandler.syncFullData(diffOrderInfoList);
            log.info("完成es与数据库数据一致性任务的巡检校验操作");
        }

        XxlJobHelper.handleSuccess();
    }


    public List<OrderInfoDO> getDiffOrderInfoList(List<OrderInfoDO> orderInfoList) {
        // 1. 获取订单id
        Map<String, OrderInfoDO> orderInfoMap = orderInfoList.stream().collect(Collectors.toMap(OrderInfoDO::getOrderId, Function.identity()));
        List<String> orderIds = new ArrayList<>(orderInfoMap.keySet());

        // 2. 从es里查询对应的订单数据
        List<EsOrderListQueryDO> indexList = orderListEsRepository.getListByOrderIds(orderIds);
        Map<String, EsOrderListQueryDO> indexMap = indexList.stream().collect(Collectors.toMap(EsOrderListQueryDO::getOrderId, Function.identity()));

        List<OrderInfoDO> diffList = new ArrayList<>();
        for (Map.Entry<String, OrderInfoDO> entry : orderInfoMap.entrySet()) {
            String orderId = entry.getKey();
            OrderInfoDO dbOrderInfo = entry.getValue();
            EsOrderListQueryDO esOrderInfo = indexMap.get(orderId);

            // 3. 找出数据不一致的订单
            if(esOrderInfo == null || !Objects.equals(dbOrderInfo.getOrderStatus(), esOrderInfo.getOrderStatus())) {
                diffList.add(dbOrderInfo);
            }
        }

        return diffList;
    }
}
