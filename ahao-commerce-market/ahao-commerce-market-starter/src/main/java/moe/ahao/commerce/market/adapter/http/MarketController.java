package moe.ahao.commerce.market.adapter.http;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.market.api.MarketFeignApi;
import moe.ahao.commerce.market.api.dto.CalculateOrderAmountDTO;
import moe.ahao.commerce.market.api.query.CalculateOrderAmountQuery;
import moe.ahao.commerce.market.application.MarketCalculateAppService;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(MarketFeignApi.PATH)
public class MarketController implements MarketFeignApi {
    @Autowired
    private MarketCalculateAppService marketCalculateService;
    @Override
    public Result<CalculateOrderAmountDTO> calculateOrderAmount(@RequestBody CalculateOrderAmountQuery query) {
        CalculateOrderAmountDTO calculateOrderAmountDTO = marketCalculateService.calculateOrderAmount(query);
        return Result.success(calculateOrderAmountDTO);
    }
}
