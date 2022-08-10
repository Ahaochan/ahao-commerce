package moe.ahao.commerce.inventory.adapter.http;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.inventory.api.InventoryFeignApi;
import moe.ahao.commerce.inventory.api.command.AddProductStockCommand;
import moe.ahao.commerce.inventory.api.command.DeductProductStockCommand;
import moe.ahao.commerce.inventory.api.command.ModifyProductStockCommand;
import moe.ahao.commerce.inventory.api.command.ReleaseProductStockCommand;
import moe.ahao.commerce.inventory.application.AddProductStockAppService;
import moe.ahao.commerce.inventory.application.DeductProductStockAppService;
import moe.ahao.commerce.inventory.application.ModifyProductStockAppService;
import moe.ahao.commerce.inventory.application.ReleaseProductStockAppService;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 正向下单流程接口冒烟测试
 */
@Slf4j
@RestController
@RequestMapping(InventoryFeignApi.PATH)
public class InventoryController implements InventoryFeignApi {
    @Autowired
    private AddProductStockAppService addProductStockAppService;
    @Autowired
    private DeductProductStockAppService deductProductStockAppService;
    @Autowired
    private ReleaseProductStockAppService releaseProductStockAppService;
    @Autowired
    private ModifyProductStockAppService modifyProductStockAppService;

    /**
     * 扣减商品库存
     */
    @Override
    public Result<Boolean> deductProductStock(@RequestBody DeductProductStockCommand command) {
        boolean result = deductProductStockAppService.deduct(command);
        return Result.success(result);
    }

    /**
     * 取消订单 释放商品库存
     */
    @Override
    public Result<Boolean> releaseProductStock(ReleaseProductStockCommand command) {
        boolean result = releaseProductStockAppService.releaseProductStock(command);
        return Result.success(result);
    }

    /**
     * 新增商品库存
     */
    @PostMapping("/addProductStock")
    public Result<Boolean> addProductStock(@RequestBody AddProductStockCommand request) {
        addProductStockAppService.add(request);
        return Result.success(true);
    }

    /**
     * 调整商品库存
     */
    @PostMapping("/modifyProductStock")
    public Result<Boolean> modifyProductStock(@RequestBody ModifyProductStockCommand request) {
        modifyProductStockAppService.doModify(request);
        return Result.success(true);
    }
}
