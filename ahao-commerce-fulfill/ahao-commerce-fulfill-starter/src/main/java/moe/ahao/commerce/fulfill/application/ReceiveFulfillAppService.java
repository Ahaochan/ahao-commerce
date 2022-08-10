package moe.ahao.commerce.fulfill.application;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.infrastructure.utils.SnowFlake;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.application.saga.TmsSagaService;
import moe.ahao.commerce.fulfill.application.saga.WmsSagaService;
import moe.ahao.commerce.fulfill.infrastructure.component.FulfillDataBuilder;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillTypeEnum;
import moe.ahao.commerce.fulfill.infrastructure.exception.FulfillExceptionEnum;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillItemDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillLogDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.service.OrderFulfillItemMyBatisService;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.service.OrderFulfillLogMyBatisService;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.service.OrderFulfillMyBatisService;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReceiveFulfillAppService {
    @Setter
    private ReceiveFulfillAppService _this;

    @Autowired
    private OrderFulfillMapper orderFulfillMapper;
    @Autowired
    private OrderFulfillMyBatisService orderFulfillMyBatisService;
    @Autowired
    private OrderFulfillItemMyBatisService orderFulfillItemMyBatisService;
    @Autowired
    private OrderFulfillLogMyBatisService orderFulfillLogMyBatisService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WmsSagaService wmsSagaService;
    @Autowired
    private TmsSagaService tmsSagaService;

    public Boolean fulfill(ReceiveFulfillCommand command) {
        String orderId = command.getOrderId();
        // 1. 加分布式锁（防止重复触发履约）
        String lockKey = RedisLockKeyConstants.FULFILL_KEY + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw FulfillExceptionEnum.ORDER_FULFILL_ERROR.msg();
        }

        try {
            return _this.doFulfillV2(command);
        } finally {
            lock.unlock();
        }
    }

    @Deprecated
    public boolean doFulfillV1(ReceiveFulfillCommand command) {
        String orderId = command.getOrderId();
        // 1. 幂等校验, 校验orderId是否已经履约过
        if (this.orderFulfilled(orderId)) {
            log.info("该订单已履约, orderId={}", orderId);
            return true;
        }

        // 2. saga状态机，触发wms捡货和tms发货
        // StateMachineEngine stateMachineEngine = (StateMachineEngine) applicationContext
        //     .getBean("stateMachineEngine");
        // Map<String, Object> startParams = new HashMap<>(3);
        // startParams.put("receiveFulfillRequest", command);
        //
        // // 配置的saga状态机 json的name
        // // 位于/resources/statelang/order_fulfull.json
        // String stateMachineName = "order_fulfill";
        // log.info("开始触发saga流程，stateMachineName={}", stateMachineName);
        // StateMachineInstance inst = stateMachineEngine.startWithBusinessKey(stateMachineName, null, null, startParams);
        // if (ExecutionStatus.SU.equals(inst.getStatus())) {
        //     log.info("订单履约流程执行完毕. xid={}", inst.getId());
        // } else {
        //     log.error("订单履约流程执行异常. xid={}", inst.getId());
        //     throw FulfillExceptionEnum.ORDER_FULFILL_IS_ERROR.msg();
        // }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean doFulfillV2(ReceiveFulfillCommand command) {
        String orderId = command.getOrderId();
        // 1. 幂等校验, 校验orderId是否已经履约过
        if (this.orderFulfilled(orderId)) {
            log.info("该订单已履约, orderId={}", orderId);
            return true;
        }

        // 2. 创建履约单, 拆分履约单
        String fulfillId = SnowFlake.generateIdStr();
        FulfillDataBuilder.FulfillData fulfillData = new FulfillDataBuilder(fulfillId, command).build();
        List<FulfillDataBuilder.FulfillData> splitFulfillData = fulfillData.split();

        // 3. 保存履约单、履约条目、履约单状态变更
        List<OrderFulfillDO> orderFulfillList = splitFulfillData.stream().map(FulfillDataBuilder.FulfillData::getOrderFulfill).collect(Collectors.toList());
        List<OrderFulfillItemDO> orderFulFillItemList = splitFulfillData.stream().map(FulfillDataBuilder.FulfillData::getOrderFulfillItems).flatMap(List::stream).collect(Collectors.toList());
        List<OrderFulfillLogDO> orderFulfillLogList = splitFulfillData.stream().map(FulfillDataBuilder.FulfillData::getOrderFulfillLog).collect(Collectors.toList());
        orderFulfillMyBatisService.saveBatch(orderFulfillList);
        orderFulfillItemMyBatisService.saveBatch(orderFulFillItemList);
        orderFulfillLogMyBatisService.saveBatch(orderFulfillLogList);

        // 4. 对普通履约单进行履约调度
        Map<String, List<OrderFulfillItemDO>> orderFulfillItemMap = orderFulFillItemList.stream()
            .collect(Collectors.groupingBy(OrderFulfillItemDO::getFulfillId));
        for (OrderFulfillDO orderFulfill : orderFulfillList) {
            if (OrderFulfillTypeEnum.NORMAL.equals(OrderFulfillTypeEnum.getByCode(orderFulfill.getOrderFulfillType()))) {
                // 4.1. 调用wms的接口进行捡货出库
                wmsSagaService.pickGoods(command);
                // 4.2. 调用tms的接口进行发货
                tmsSagaService.sendOut(command);
            }
        }
        return true;
    }

    /**
     * 校验订单是否履约过
     *
     * @param orderId 订单id
     * @return true为是, false为否
     */
    private boolean orderFulfilled(String orderId) {
        List<OrderFulfillDO> list = orderFulfillMapper.selectListByOrderId(orderId);
        return CollectionUtils.isNotEmpty(list);
    }
}
