package moe.ahao.commerce.product.adapter;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.product.api.ProductFeignApi;
import moe.ahao.commerce.product.api.dto.ProductSkuDTO;
import moe.ahao.commerce.product.api.query.GetProductSkuQuery;
import moe.ahao.commerce.product.api.query.ListProductSkuQuery;
import moe.ahao.commerce.product.application.GetProductSkuQueryService;
import moe.ahao.commerce.product.application.ListProductSkuQueryService;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ProductFeignApi.PATH)
public class ProductController implements ProductFeignApi {
    @Autowired
    private GetProductSkuQueryService getProductSkuQueryService;
    @Autowired
    private ListProductSkuQueryService listProductSkuQueryService;

    @Override
    public Result<ProductSkuDTO> getBySkuCode(@RequestBody GetProductSkuQuery query) {
        ProductSkuDTO dto = getProductSkuQueryService.query(query);
        return Result.success(dto);
    }

    @Override
    public Result<List<ProductSkuDTO>> listBySkuCodes(@RequestBody ListProductSkuQuery query) {
        List<ProductSkuDTO> list = listProductSkuQueryService.query(query);
        return Result.success(list);
    }
}
