package moe.ahao.commerce.order.infrastructure.gateway;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import moe.ahao.commerce.inventory.api.command.DeductProductStockCommand;
import moe.ahao.commerce.inventory.api.command.ReleaseProductStockCommand;
import moe.ahao.commerce.order.infrastructure.exception.OrderException;
import moe.ahao.commerce.order.infrastructure.gateway.feign.InventoryFeignClient;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 库存服务远程接口
 */
@Component
public class InventoryGateway {
    /**
     * 库存服务
     */
    @Autowired
    private InventoryFeignClient inventoryFeignClient;

    /**
     * 扣减订单条目库存
     */
    @SentinelResource(value = "InventoryGateway:deductProductStock")
    public void deductProductStock(DeductProductStockCommand lockProductStockRequest) {
        Result<Boolean> result = inventoryFeignClient.deductProductStock(lockProductStockRequest);
        if (result.getCode() != moe.ahao.domain.entity.Result.SUCCESS) {
            throw new OrderException(result.getCode(), result.getMsg());
        }
    }

    /**
     * 释放订单条目库存
     */
    @SentinelResource(value = "InventoryGateway:releaseProductStock")
    public void releaseProductStock(ReleaseProductStockCommand command) {
        Result<Boolean> result = inventoryFeignClient.releaseProductStock(command);
        if (result.getCode() != moe.ahao.domain.entity.Result.SUCCESS) {
            throw new OrderException(result.getCode(), result.getMsg());
        }
    }
}
