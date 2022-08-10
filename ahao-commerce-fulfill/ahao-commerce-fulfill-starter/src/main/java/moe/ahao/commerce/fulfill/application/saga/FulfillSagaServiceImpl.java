package moe.ahao.commerce.fulfill.application.saga;


import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.application.CancelFulfillAppService;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillStatusEnum;
import moe.ahao.commerce.fulfill.infrastructure.enums.OrderFulfillTypeEnum;
import moe.ahao.commerce.fulfill.infrastructure.exception.FulfillExceptionEnum;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillItemDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillItemMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.service.OrderFulfillItemMyBatisService;
import moe.ahao.util.commons.lang.RandomHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * fulfull saga service
 */
@Service("fulfillSagaService")
@Slf4j
public class FulfillSagaServiceImpl implements FulfillSagaService {
    @Autowired
    private OrderFulfillMapper orderFulfillMapper;
    @Autowired
    private OrderFulfillItemMapper orderFulfillItemMapper;
    @Autowired
    private OrderFulfillItemMyBatisService orderFulfillItemMyBatisService;

    @Autowired
    private CancelFulfillAppService cancelFulfillAppService;

    /**
     * 创建履约单
     */
    @Override
    public Boolean createFulfillOrder(ReceiveFulfillCommand command) {
        String orderId = command.getOrderId();
        log.info("创建履约单, orderId:{}, command:{}", orderId, command);
        String fulfillException = command.getFulfillException();
        if (StringUtils.isNotBlank(fulfillException) && fulfillException.equals("true")) {
            throw FulfillExceptionEnum.ORDER_FULFILL_IS_ERROR.msg();
        }

        // 创建履约单
        // 1. 生成履约单ID
        String fulfillId = this.genFulfillId();

        // 2. 保存履约单和履约条目
        OrderFulfillDO orderFulFill = this.convert(fulfillId, command);
        List<OrderFulfillItemDO> orderFulFillItems = this.convertItems(fulfillId, command);
        orderFulfillMapper.insert(orderFulFill);
        orderFulfillItemMyBatisService.saveBatch(orderFulFillItems);

        log.info("履约单创建成功, orderId:{}", orderId);
        return true;
    }

    /**
     * 补偿创建履约单
     */
    @Override
    public Boolean createFulfillOrderCompensate(ReceiveFulfillCommand command) {
        String orderId = command.getOrderId();
        log.info("补偿创建履约单, orderId:{}, command:{}", orderId, command);
        // 取消履约单
        cancelFulfillAppService.cancelFulfill(orderId);

        log.info("补偿创建履约单结束, orderId:{}", orderId);
        return true;
    }

    /**
     * 生成履约单id
     * TODO 接入发号器
     */
    private String genFulfillId() {
        return RandomHelper.getString(10, RandomHelper.DIST_NUMBER);
    }

    private List<OrderFulfillItemDO> convertItems(String fulfillId, ReceiveFulfillCommand command) {
        List<ReceiveFulfillCommand.ReceiveOrderItem> list1 = command.getReceiveOrderItems();
        if (CollectionUtils.isEmpty(list1)) {
            return Collections.emptyList();
        }
        List<OrderFulfillItemDO> list2 = new ArrayList<>(list1.size());
        for (ReceiveFulfillCommand.ReceiveOrderItem item1 : list1) {
            OrderFulfillItemDO item2 = new OrderFulfillItemDO();
            item2.setFulfillId(fulfillId);
            item2.setSkuCode(item1.getSkuCode());
            item2.setProductType(item1.getProductType());
            item2.setProductName(item1.getProductName());
            item2.setSalePrice(item1.getSalePrice());
            item2.setSaleQuantity(item1.getSaleQuantity());
            item2.setProductUnit(item1.getProductUnit());
            item2.setPayAmount(item1.getPayAmount());
            item2.setOriginAmount(item1.getOriginAmount());

            list2.add(item2);
        }

        return list2;
    }

    private OrderFulfillDO convert(String fulfillId, ReceiveFulfillCommand command) {
        OrderFulfillDO data = new OrderFulfillDO();
        // data.setId();
        data.setBusinessIdentifier(command.getBusinessIdentifier());
        data.setFulfillId(fulfillId);
        data.setOrderId(command.getOrderId());
        data.setSellerId(command.getSellerId());
        data.setUserId(command.getUserId());
        data.setStatus(OrderFulfillStatusEnum.FULFILL.getCode());
        data.setOrderFulfillType(OrderFulfillTypeEnum.getByOrderType(command.getOrderType()).getCode());
        data.setDeliveryType(command.getDeliveryType());
        data.setReceiverName(command.getReceiverName());
        data.setReceiverPhone(command.getReceiverPhone());
        data.setReceiverProvince(command.getReceiverProvince());
        data.setReceiverCity(command.getReceiverCity());
        data.setReceiverArea(command.getReceiverArea());
        data.setReceiverStreet(command.getReceiverStreet());
        data.setReceiverDetailAddress(command.getReceiverDetailAddress());
        data.setReceiverLon(command.getReceiverLon());
        data.setReceiverLat(command.getReceiverLat());
        // data.setDelivererNo();
        // data.setDelivererName();
        // data.setDelivererPhone();
        // data.setLogisticsCode();
        data.setUserRemark(command.getUserRemark());
        data.setPayType(command.getPayType());
        data.setPayAmount(command.getPayAmount());
        data.setTotalAmount(command.getTotalAmount());
        data.setDeliveryAmount(command.getDeliveryAmount());

        return data;
    }
}
