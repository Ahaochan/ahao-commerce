package com.ruyuan.eshop.order.elasticsearch.migrate;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.ruyuan.eshop.common.utils.LoggerFormat;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.elasticsearch.handler.order.EsOrderInfoInsertHandler;
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
 * 订单es数据迁移
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
@Slf4j
public class OrderMigrateToEsHandler extends AbstractMigrateToEsHandler{

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    @Qualifier("orderThreadPool")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private EsOrderInfoInsertHandler esOrderInfoInsertHandler;

    private static long limit = 2000L;

    private static int partitionSize = 1000;

    @Override
    protected void doExecute(String content) throws Exception {

        log.info(LoggerFormat
                .build()
                .remark("OrderMigrateToEsHandler->start~")
                .data("content", content)
                .finish());

        //默认增量刷
        Content contentConfig = JSONObject.parseObject(content, Content.class);
        long offset = contentConfig.getOffset();
        long endOffset = orderInfoDAO.count();

        // 分页查询订单
        PageOrderInfoIterator iterator = new PageOrderInfoIterator(orderInfoDAO,offset,limit,endOffset);
        int total = 0;
        while(iterator.hasNext()) {
            List<OrderInfoDO> orders = iterator.next();
            List<List<OrderInfoDO>> lists = Lists.partition(orders, partitionSize);
            for (List<OrderInfoDO> list : lists) {
                    List<String> orderIds = list.stream().map(OrderInfoDO::getOrderId).collect(Collectors.toList());
                    taskExecutor.execute(()->{
                        try {
                            esOrderInfoInsertHandler.sync(list,orderIds,-1);
                        }catch (Exception e) {
                            log.error(LoggerFormat
                                    .build()
                                    .remark("esOrderFullDataAddHandler error!!")
                                    .data("offset",iterator.offset)
                                    .data("endOffset",iterator.endOffset)
                                    .finish(),e);
                        }
                    });
                    total+=orderIds.size();
            }
        }
        log.info("esOrderFullDataAddHandler sync finished!,total={}",total);
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
    public static class PageOrderInfoIterator implements Iterator<List<OrderInfoDO>> {

        private final OrderInfoDAO orderInfoDAO;
        /**
         * 默从0开始
         */
        private long offset = 0;
        /**
         * 每次查询大小
         */
        private final long limit;

        private final long endOffset;

        public PageOrderInfoIterator(OrderInfoDAO orderInfoDAO, long offset, long limit, long endOffset) {
            this.orderInfoDAO = orderInfoDAO;
            this.offset = offset;
            this.limit = limit;
            this.endOffset = endOffset;
        }

        private List<OrderInfoDO> result;

        @Override
        public boolean hasNext() {
            // 分页查询
            result = orderInfoDAO
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
        public List<OrderInfoDO> next() {
            return result;
        }
    }
}
