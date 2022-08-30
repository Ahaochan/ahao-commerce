package com.ruyuan.eshop.order.elasticsearch.handler.aftersale;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.order.dao.AfterSaleItemDAO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleItemDO;
import com.ruyuan.eshop.order.elasticsearch.EsClientService;
import com.ruyuan.eshop.order.elasticsearch.handler.EsAbstractHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 售后单条目信息更新service：
 * <p>
 * 当after_sale_item被更新的时候，需要更新AfterSaleListQueryIndex
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class EsAfterSaleItemUpdateHandler extends EsAbstractHandler {

    @Autowired
    private AfterSaleItemDAO afterSaleItemDAO;

    @Autowired
    private EsClientService esClientService;

    @Autowired
    private EsAfterSaleListQueryIndexHandler esAfterSaleListQueryIndexHandler;

    /**
     * 构建AfterSaleListQueryListIndex并同步到es
     */
    public void sync(List<String> afterSaleIds, long timestamp) throws Exception {

        // 1、查询售后单条目
        List<AfterSaleItemDO> afterSaleItems = afterSaleItemDAO.listByAfterSaleIds(afterSaleIds);
        if (CollectionUtils.isEmpty(afterSaleItems)) {
            return;
        }

        // 2、异步构建AfterSaleListQueryListIndex并同步到es
        esAfterSaleListQueryIndexHandler.asyncBuildAndSynToEs(afterSaleIds, timestamp);
    }
}
