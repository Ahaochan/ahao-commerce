package com.ruyuan.eshop.inventory.service.impl;

import com.ruyuan.eshop.inventory.dao.ProductStockDAO;
import com.ruyuan.eshop.inventory.dao.ProductStockLogDAO;
import com.ruyuan.eshop.inventory.domain.dto.DeductStockDTO;
import com.ruyuan.eshop.inventory.domain.entity.ProductStockDO;
import com.ruyuan.eshop.inventory.domain.entity.ProductStockLogDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 扣减商品库存处理器
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
public class DeductProductStockProcessor {

    @Autowired
    private ProductStockLogDAO productStockLogDAO;

    @Autowired
    private ProductStockDAO productStockDAO;

    @Autowired
    private SyncStockToCacheProcessor syncStockToCacheProcessor;

    /**
     * 执行扣减商品库存逻辑
     */
    @Transactional(rollbackFor = Exception.class)
    public void doDeduct(DeductStockDTO deductStock) {

        // 1、扣减mysql商品库存
        productStockDAO.deductProductStock(deductStock.getSkuCode(), deductStock.getSaleQuantity());

        // 2、增加库存扣减日志表
        productStockLogDAO.save(buildStockLog(deductStock));
    }

    private ProductStockLogDO buildStockLog(DeductStockDTO deductStock) {

        ProductStockDO productStockDO = deductStock.getProductStockDO();
        Long saleQuantity = deductStock.getSaleQuantity().longValue();
        Long originSaleStock = productStockDO.getSaleStockQuantity();
        // 通过扣减log获取原始已销售库存
        Long originSaledStock = getOriginSaledStock(productStockDO);

        ProductStockLogDO logDO = new ProductStockLogDO();
        logDO.setOrderId(deductStock.getOrderId());
        logDO.setSkuCode(deductStock.getSkuCode());
        logDO.setOriginSaleStockQuantity(originSaleStock);
        logDO.setOriginSaledStockQuantity(originSaledStock);
        logDO.setDeductedSaleStockQuantity(originSaleStock - saleQuantity);
        logDO.setIncreasedSaledStockQuantity(originSaledStock + saleQuantity);
        return logDO;
    }


    /**
     * 获取sku的原始已销售库存
     *
     * @param productStockDO
     * @return
     */
    private Long getOriginSaledStock(ProductStockDO productStockDO) {
        //1、查询sku库存最近一笔扣减日志
        ProductStockLogDO latestLog = productStockLogDAO.getLatestOne(productStockDO.getSkuCode());

        //2、获取原始的已销售库存
        Long originSaledStock = null;
        if (null == latestLog) {
            //第一次扣，直接取productStockDO的saledStockQuantity
            originSaledStock = productStockDO.getSaledStockQuantity();
        } else {
            //取最近一笔扣减日志的increasedSaledStockQuantity
            originSaledStock = latestLog.getIncreasedSaledStockQuantity();
        }
        return originSaledStock;
    }

}
