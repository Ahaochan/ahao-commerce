package moe.ahao.commerce.fulfill.application.saga;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.infrastructure.gateway.WmsGateway;
import moe.ahao.commerce.wms.api.command.PickGoodsCommand;
import moe.ahao.commerce.wms.api.dto.PickDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("wmsSageService")
@Slf4j
public class WmsSageServiceImpl implements WmsSagaService {
    @Autowired
    private WmsGateway wmsGateway;

    @Override
    public Boolean pickGoods(ReceiveFulfillCommand command) {
        String orderId = command.getOrderId();
        log.info("进行拣货, orderId:{}, command:{}", orderId, command);

        // 调用wms系统进行捡货
        PickDTO result = wmsGateway.pickGoods(this.convert(command));

        log.info("捡货结果, orderId:{}, result={}", orderId, result);
        return true;
    }

    @Override
    public Boolean pickGoodsCompensate(ReceiveFulfillCommand command) {
        String orderId = command.getOrderId();
        log.info("补偿捡货, orderId:{}, command:{}", orderId, command);

        // 调用wms系统进行捡货
        wmsGateway.cancelPickGoods(command.getOrderId());
        log.info("补偿捡货结束, orderId:{}", orderId);
        return true;
    }


    private PickGoodsCommand convert(ReceiveFulfillCommand that) {
        PickGoodsCommand command = new PickGoodsCommand();
        List<PickGoodsCommand.OrderItem> orderItems = new ArrayList<>();
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
        command.setWmsException(that.getWmsException());

        for (ReceiveFulfillCommand.ReceiveOrderItem receiveOrderItem : that.getReceiveOrderItems()) {
            PickGoodsCommand.OrderItem pickOrderItem = new PickGoodsCommand.OrderItem();
            pickOrderItem.setSkuCode(receiveOrderItem.getSkuCode());
            pickOrderItem.setProductName(receiveOrderItem.getProductName());
            pickOrderItem.setSalePrice(receiveOrderItem.getSalePrice());
            pickOrderItem.setSaleQuantity(receiveOrderItem.getSaleQuantity());
            pickOrderItem.setProductUnit(receiveOrderItem.getProductUnit());
            pickOrderItem.setPayAmount(that.getPayAmount());
            pickOrderItem.setOriginAmount(receiveOrderItem.getOriginAmount());

            orderItems.add(pickOrderItem);
        }
        return command;
    }
}
