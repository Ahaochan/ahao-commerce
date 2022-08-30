package com.ruyuan.eshop.order.elasticsearch.handler.aftersale;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.order.dao.AfterSaleInfoDAO;
import com.ruyuan.eshop.order.dao.AfterSaleItemDAO;
import com.ruyuan.eshop.order.dao.AfterSaleRefundDAO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleInfoDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleItemDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleRefundDO;
import com.ruyuan.eshop.order.elasticsearch.EsClientService;
import com.ruyuan.eshop.order.elasticsearch.handler.EsAbstractHandler;
import com.ruyuan.eshop.order.elasticsearch.query.AfterSaleListQueryIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 售后单全量数据新增service：
 * <p>
 * 当after_sale_info被创建时（下单），需要构建AfterSaleListQueryIndex
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class EsAfterSaleInsertHandler extends EsAbstractHandler {

    @Autowired
    private AfterSaleInfoDAO afterSaleInfoDAO;

    @Autowired
    private AfterSaleItemDAO afterSaleItemDAO;

    @Autowired
    private AfterSaleRefundDAO afterSaleRefundDAO;

    @Autowired
    private EsClientService esClientService;

    @Autowired
    private EsAfterSaleListQueryIndexHandler esAfterSaleListQueryIndexHandler;


    /**
     * 构建afterSaleListQueryIndex并同步到es
     */
    public void sync(List<String> afterSaleIds, long timestamp) throws Exception {

        // 查询售后单
        List<AfterSaleInfoDO> afterSales = afterSaleInfoDAO.listByAfterSaleIds(afterSaleIds);
        if (CollectionUtils.isEmpty(afterSales)) {
            return;
        }

        sync(afterSales, afterSaleIds, timestamp);
    }

    /**
     * 构建afterSaleListQueryIndex并同步到es
     */
    public void sync(List<AfterSaleInfoDO> afterSales, List<String> afterSaleIds, long timestamp) throws Exception {

        // 1、查询售后单条目
        List<AfterSaleItemDO> afterSaleItems = afterSaleItemDAO.listByAfterSaleIds(afterSaleIds);

        // 2、查询售后退款信息
        List<AfterSaleRefundDO> afterSaleRefunds = afterSaleRefundDAO.listByAfterSaleIds(afterSaleIds);

        // 3、构建afterSaleListQueryIndex并同步到es
        List<AfterSaleListQueryIndex> afterSaleListQueryIndices =
                esAfterSaleListQueryIndexHandler.buildAfterSaleListQueryIndex(afterSales, afterSaleItems, afterSaleRefunds);
        esAfterSaleListQueryIndexHandler.sycToEs(afterSaleListQueryIndices);
    }
}
