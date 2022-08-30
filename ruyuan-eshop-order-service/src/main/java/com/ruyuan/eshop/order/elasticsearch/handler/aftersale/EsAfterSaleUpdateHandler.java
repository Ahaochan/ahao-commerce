package com.ruyuan.eshop.order.elasticsearch.handler.aftersale;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.order.dao.AfterSaleInfoDAO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleInfoDO;
import com.ruyuan.eshop.order.elasticsearch.EsClientService;
import com.ruyuan.eshop.order.elasticsearch.handler.EsAbstractHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 售后单更新service：
 * <p>
 * 当after_sale_info被更新的时候，需要更新AfterSaleListQueryIndex
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class EsAfterSaleUpdateHandler extends EsAbstractHandler {

    @Autowired
    private AfterSaleInfoDAO afterSaleInfoDAO;

    @Autowired
    private EsClientService esClientService;

    @Autowired
    private EsAfterSaleListQueryIndexHandler esAfterSaleListQueryIndexHandler;

    /**
     * 构建AfterSaleListQueryListIndex并同步到es
     */
    public void sync(List<String> afterSaleIds, long timestamp) throws Exception {
        // 1、查询售后单
        List<AfterSaleInfoDO> afterSales = afterSaleInfoDAO.listByAfterSaleIds(afterSaleIds);
        if (CollectionUtils.isEmpty(afterSales)) {
            return;
        }

        // 3、异步构建OrderQueryListIndex，并同步到es里面去
        esAfterSaleListQueryIndexHandler.asyncBuildAndSynToEs(afterSales, afterSaleIds, timestamp);
    }
}
