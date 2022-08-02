package com.ruyuan.eshop.order.dao;

import cn.hutool.json.JSONUtil;
import com.ruyuan.eshop.order.domain.entity.OrderOperateLogDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单操作日志表 DAO
 *
 * @author zhonghuashishan
 */
@Slf4j
@Component
public class OrderOperateLogDAO {

    /**
     * mongo存储模板
     */
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 插入订单操作日志
     * @param log 操作日志DO
     * @return 结果
     */
    public OrderOperateLogDO save(OrderOperateLogDO log) {
        Date date = new Date();
        log.setGmtCreate(date);
        log.setGmtModified(date);
        return mongoTemplate.save(log);
    }

    /**
     * 批量插入订单操作日志
     *
     * @param logList 操作日志集合
     */
    public void batchSave(List<OrderOperateLogDO> logList) {
        List<OrderOperateLogDO> logDOList = logList.stream().peek((item) -> {
            Date date = new Date();
            item.setGmtCreate(date);
            item.setGmtModified(date);
        }).collect(Collectors.toList());
        mongoTemplate.insertAll(logDOList);
    }

    /**
     * 根据订单id查询订单操作日志
     *
     * @param orderId 订单id
     * @return 订单操作日志列表
     */
    public List<OrderOperateLogDO> listByOrderId(String orderId) {
        return mongoTemplate.find(buildQuery(orderId), OrderOperateLogDO.class);
    }

    /**
     * 构造查询条件
     *
     * @param orderId 订单id
     * @return 查询条件
     */
    private Query buildQuery(String orderId) {
        // 创建时间倒序查询
        Sort orderBy = Sort.by(Sort.Direction.DESC, "gmtCreate");
        Query query = new Query(Criteria.where("orderId").is(orderId));
        query.with(orderBy);
        return query;
    }

}
