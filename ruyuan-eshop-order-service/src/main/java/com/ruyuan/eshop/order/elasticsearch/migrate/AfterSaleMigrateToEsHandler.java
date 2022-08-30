package com.ruyuan.eshop.order.elasticsearch.migrate;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.ruyuan.eshop.common.utils.LoggerFormat;
import com.ruyuan.eshop.order.dao.AfterSaleInfoDAO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleInfoDO;
import com.ruyuan.eshop.order.elasticsearch.handler.aftersale.EsAfterSaleInsertHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 售后单es数据迁移
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
@Slf4j
public class AfterSaleMigrateToEsHandler extends AbstractMigrateToEsHandler{

    @Autowired
    private AfterSaleInfoDAO afterSaleInfoDAO;

    @Autowired
    @Qualifier("orderThreadPool")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private EsAfterSaleInsertHandler esAfterSaleInsertHandler;

    private static long limit = 2000L;

    private static int partitionSize = 1000;

    @Override
    protected void doExecute(String content) throws Exception {

        log.info(LoggerFormat
                .build()
                .remark("AfterSaleMigrateToEsHandler->start~")
                .data("content", content)
                .finish());

        //默认增量刷
        Content contentConfig = JSONObject.parseObject(content, Content.class);
        long offset = contentConfig.getOffset();
        long endOffset = afterSaleInfoDAO.count();

        // 分页查询订单
        PageAfterSaleInfoIterator iterator = new PageAfterSaleInfoIterator(afterSaleInfoDAO,offset,limit,endOffset);
        int total = 0;
        while(iterator.hasNext()) {
            List<AfterSaleInfoDO> afterSales = iterator.next();
            List<List<AfterSaleInfoDO>> lists = Lists.partition(afterSales, partitionSize);
            for (List<AfterSaleInfoDO> list : lists) {
                    List<String> afterSaleIds = list.stream().map(AfterSaleInfoDO::getAfterSaleId).collect(Collectors.toList());
                    taskExecutor.execute(()->{
                        try {
                            esAfterSaleInsertHandler.sync(list,afterSaleIds,-1);
                        }catch (Exception e) {
                            log.error(LoggerFormat
                                    .build()
                                    .remark("esAfterSaleFullDataAddHandler error!!")
                                    .data("offset",iterator.offset)
                                    .data("endOffset",iterator.endOffset)
                                    .finish(),e);
                        }
                    });
                    total+=afterSaleIds.size();
            }
        }
        log.info("esAfterSaleFullDataAddHandler sync finished!,total={}",total);
    }


    @Data
    public static class Content {

        /**
         * 起始offset
         */
        private long offset = 0;

    }

    /**
     * 分页查询跑批数据
     */
    public static class PageAfterSaleInfoIterator implements Iterator<List<AfterSaleInfoDO>> {

        private final AfterSaleInfoDAO afterSaleInfoDAO;
        /**
         * 默从0开始
         */
        private long offset = 0;
        /**
         * 每次查询大小
         */
        private final long limit;

        private final long endOffset;

        public PageAfterSaleInfoIterator(AfterSaleInfoDAO afterSaleInfoDAO, long offset, long limit, long endOffset) {
            this.afterSaleInfoDAO = afterSaleInfoDAO;
            this.offset = offset;
            this.limit = limit;
            this.endOffset = endOffset;
        }

        private List<AfterSaleInfoDO> result;

        @Override
        public boolean hasNext() {
            // 分页查询
            result = afterSaleInfoDAO
                    .getPageBy(offset,limit);

            if(result.size()>0) {
                offset+=limit;
                return true;
            }
            else {
                if(offset<=endOffset) {
                    offset+=limit;
                    return true;
                }
                return false;
            }
        }

        @Override
        public List<AfterSaleInfoDO> next() {
            return result;
        }
    }
}
