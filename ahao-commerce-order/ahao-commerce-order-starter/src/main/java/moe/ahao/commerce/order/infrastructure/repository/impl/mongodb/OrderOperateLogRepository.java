package moe.ahao.commerce.order.infrastructure.repository.impl.mongodb;

import moe.ahao.commerce.common.enums.OrderOperateTypeEnum;
import moe.ahao.commerce.common.enums.OrderStatusChangeEnum;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mongodb.data.OrderOperateLogDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderOperateLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class OrderOperateLogRepository {
    @Autowired
    private OrderOperateLogMapper orderOperateLogMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据订单id查询订单操作日志
     *
     * @param orderId 订单id
     * @return 订单操作日志列表
     */
    public List<OrderOperateLogDO> getListByOrderId(String orderId) {
        return mongoTemplate.find(this.buildQuery(orderId), OrderOperateLogDO.class);
    }

    /**
     * 构造查询条件
     *
     * @param orderId 订单id
     * @return 查询条件
     */
    private Query buildQuery(String orderId) {
        // 创建时间倒序查询
        Sort orderBy = Sort.by(Sort.Direction.DESC, "createTime");
        Query query = new Query(Criteria.where("orderId").is(orderId));
        query.with(orderBy);
        return query;
    }

    public void save(OrderInfoDO order, OrderStatusChangeEnum statusChange) {
        OrderOperateTypeEnum operateType = statusChange.getOperateType();
        Integer fromStatus = statusChange.getFromStatus().getCode();
        Integer toStatus = statusChange.getToStatus().getCode();
        this.save(order.getOrderId(), operateType, fromStatus, toStatus, operateType.getName());
    }

    public void save(String orderId, OrderOperateTypeEnum operateType, Integer fromStatus, Integer toStatus, String remark) {
        Date now = new Date();
        OrderOperateLogDO log = new OrderOperateLogDO();
        log.setOrderId(orderId);
        log.setOperateType(operateType.getCode());
        log.setPreStatus(fromStatus);
        log.setCurrentStatus(toStatus);
        log.setRemark(remark);
        log.setCreateTime(now);
        log.setUpdateTime(now);

        // orderOperateLogMapper.insert(log);
        mongoTemplate.save(log);
    }

    /**
     * 批量插入订单操作日志
     *
     * @param logList 操作日志集合
     */
    public void saveBatch(List<OrderOperateLogDO> logList) {
        Date now = new Date();
        for (OrderOperateLogDO log : logList) {
            log.setCreateTime(now);
            log.setUpdateTime(now);
        }
        mongoTemplate.insertAll(logList);
    }
}
