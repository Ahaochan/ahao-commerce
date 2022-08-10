package moe.ahao.commerce.inventory.api;

import moe.ahao.commerce.inventory.api.command.DeductProductStockCommand;
import moe.ahao.commerce.inventory.api.command.ReleaseProductStockCommand;
import moe.ahao.domain.entity.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface InventoryFeignApi {
    String PATH = "/api/inventory/";

    /**
     * 扣减商品库存
     */
    @PostMapping("/deductProductStock")
    Result<Boolean> deductProductStock(@RequestBody DeductProductStockCommand command);

    /**
     * 取消订单 释放商品库存
     */
    @PostMapping("/releaseProductStock")
    Result<Boolean> releaseProductStock(@RequestBody ReleaseProductStockCommand command);
}
