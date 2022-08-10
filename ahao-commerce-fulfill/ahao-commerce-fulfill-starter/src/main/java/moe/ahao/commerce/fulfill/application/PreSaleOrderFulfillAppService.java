package moe.ahao.commerce.fulfill.application;

import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.application.saga.TmsSagaService;
import moe.ahao.commerce.fulfill.application.saga.WmsSagaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PreSaleOrderFulfillAppService {
    @Autowired
    private WmsSagaService wmsSagaService;
    @Autowired
    private TmsSagaService tmsSagaService;

    public void fulfill(ReceiveFulfillCommand command) {
        // TODO 扭转订单状态
        // 4.1. 调用wms的接口进行捡货出库
        wmsSagaService.pickGoods(command);
        // 4.2. 调用tms的接口进行发货
        tmsSagaService.sendOut(command);
    }
}
