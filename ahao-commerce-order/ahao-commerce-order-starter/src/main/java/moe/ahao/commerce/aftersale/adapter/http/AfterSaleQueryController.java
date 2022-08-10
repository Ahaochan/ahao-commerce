package moe.ahao.commerce.aftersale.adapter.http;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.AfterSaleQueryFeignApi;
import moe.ahao.commerce.aftersale.api.dto.AfterSaleOrderDetailDTO;
import moe.ahao.commerce.aftersale.api.dto.AfterSaleOrderListDTO;
import moe.ahao.commerce.aftersale.api.query.AfterSaleDetailQuery;
import moe.ahao.commerce.aftersale.api.query.AfterSalePageQuery;
import moe.ahao.commerce.aftersale.application.AfterSaleQueryService;
import moe.ahao.domain.entity.PagingInfo;
import moe.ahao.domain.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单中心-售后查询业务接口
 */
@Slf4j
@RestController
@RequestMapping(AfterSaleQueryFeignApi.PATH)
public class AfterSaleQueryController implements AfterSaleQueryFeignApi {
    @Autowired
    private AfterSaleQueryService afterSaleQueryService;

    @Override
    public Result<PagingInfo<AfterSaleOrderDetailDTO>> listAfterSales(AfterSalePageQuery query) {
        PagingInfo<AfterSaleOrderDetailDTO> page = afterSaleQueryService.queryV2(query);
        return Result.success(page);
    }

    @Override
    public Result<AfterSaleOrderDetailDTO> afterSaleDetail(AfterSaleDetailQuery query) {
        AfterSaleOrderDetailDTO dto = afterSaleQueryService.afterSaleDetail(query.getAfterSaleId());
        return Result.success(dto);
    }
}
