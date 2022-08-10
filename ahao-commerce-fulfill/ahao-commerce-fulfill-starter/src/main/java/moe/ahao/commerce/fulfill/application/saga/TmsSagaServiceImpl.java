package moe.ahao.commerce.fulfill.application.saga;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.infrastructure.gateway.TmsGateway;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillMapper;
import moe.ahao.commerce.tms.api.command.SendOutCommand;
import moe.ahao.commerce.tms.api.dto.SendOutDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("tmsSagaService")
@Slf4j
public class TmsSagaServiceImpl implements TmsSagaService {
    @Autowired
    private OrderFulfillMapper orderFulfillMapper;

    @Autowired
    private TmsGateway tmsGateway;

    @Override
    public Boolean sendOut(ReceiveFulfillCommand command) {
        String orderId = command.getOrderId();
        log.info("进行发货, orderId:{}, command:{}", orderId, command);
        // 1. 调用tms进行发货
        SendOutDTO result = tmsGateway.sendOut(convert(command));
        log.info("发货结果, orderId:{}, result:{}", orderId, result);

        // 2. 查询履约单
        OrderFulfillDO orderFulfill = orderFulfillMapper.selectOneByOrderId(command.getOrderId());

        // 3. 存储物流单号
        String logisticsCode = result.getLogisticsCode();
        orderFulfillMapper.updateLogisticsCodeByFulfillId(orderFulfill.getFulfillId(), logisticsCode);

        return true;
    }

    @Override
    public Boolean sendOutCompensate(ReceiveFulfillCommand command) {
        String orderId = command.getOrderId();
        log.info("补偿发货, orderId:{}, command:{}", orderId, command);

        // 1. 调用tms进行补偿发货
        tmsGateway.cancelSendOut(command.getOrderId());

        log.info("补偿发货结束, orderId:{}", orderId);
        return true;
    }

    private SendOutCommand convert(ReceiveFulfillCommand that) {
        SendOutCommand command = new SendOutCommand();
        List<SendOutCommand.OrderItem> orderItems = new ArrayList<>();
        command.setBusinessIdentifier(that.getBusinessIdentifier());
        command.setOrderId(that.getOrderId());
        command.setSellerId(that.getSellerId());
        command.setUserId(that.getUserId());
        command.setDeliveryType(that.getDeliveryType());
        command.setReceiverName(that.getReceiverName());
        command.setReceiverPhone(that.getReceiverPhone());
        command.setReceiverProvince(that.getReceiverProvince());
        command.setReceiverCity(that.getReceiverCity());
        command.setReceiverArea(that.getReceiverArea());
        command.setReceiverStreet(that.getReceiverStreet());
        command.setReceiverDetailAddress(that.getReceiverDetailAddress());
        command.setReceiverLon(that.getReceiverLon());
        command.setReceiverLat(that.getReceiverLat());
        command.setUserRemark(that.getUserRemark());
        command.setPayType(that.getPayType());
        command.setPayAmount(that.getPayAmount());
        command.setTotalAmount(that.getTotalAmount());
        command.setDeliveryAmount(that.getDeliveryAmount());
        command.setOrderItems(orderItems);
        command.setTmsException(that.getTmsException());

        for (ReceiveFulfillCommand.ReceiveOrderItem receiveOrderItem : that.getReceiveOrderItems()) {
            SendOutCommand.OrderItem sendOutOrderItem = new SendOutCommand.OrderItem();
            sendOutOrderItem.setSkuCode(receiveOrderItem.getSkuCode());
            sendOutOrderItem.setProductName(receiveOrderItem.getProductName());
            sendOutOrderItem.setSalePrice(receiveOrderItem.getSalePrice());
            sendOutOrderItem.setSaleQuantity(receiveOrderItem.getSaleQuantity());
            sendOutOrderItem.setProductUnit(receiveOrderItem.getProductUnit());
            sendOutOrderItem.setPayAmount(receiveOrderItem.getPayAmount());
            sendOutOrderItem.setOriginAmount(receiveOrderItem.getOriginAmount());


            orderItems.add(sendOutOrderItem);
        }
        return command;
    }
}
