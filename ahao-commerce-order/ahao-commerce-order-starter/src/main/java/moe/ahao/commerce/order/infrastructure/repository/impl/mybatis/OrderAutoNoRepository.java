package moe.ahao.commerce.order.infrastructure.repository.impl.mybatis;

import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderAutoNoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderAutoNoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class OrderAutoNoRepository {
    @Autowired
    private OrderAutoNoMapper orderAutoNoMapper;

    public List<String> getBizTagList() {
        List<String> bizTags = orderAutoNoMapper.selectBizTagList();
        return bizTags;
    }

    // 对于一个段的申请，必须包含在一个事务里，多个事务是实现隔离的
    // 我的事务里，累加完毕了以后，我能看到的数据，是我的事务的视图里可以看到的，mvcc的概念，mysql里是有一个多版本隔离机制
    @Transactional(rollbackFor = Exception.class)
    public OrderAutoNoDO updateMaxIdAndGet(String bizTag) {
        // UPDATE order_auto_no SET max_id = max_id + step WHERE biz_tag = #{bizTag}
        int ret = orderAutoNoMapper.updateMaxIdByBizTag(bizTag);
        if (ret != 1) {
            throw OrderExceptionEnum.ORDER_AUTO_NO_GEN_ERROR.msg();
        }
        // SELECT * FROM order_auto_no WHERE biz_tag = #{bizTag}
        return orderAutoNoMapper.selectOneByBizTag(bizTag);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderAutoNoDO updateMaxIdWithStepByAndGet(String bizTag, int nextStep) {
        // maxid=10000,5000，15000,10000~15000,是你的一个新的分段
        int ret = orderAutoNoMapper.updateMaxIdWithStepByBizTag(bizTag, nextStep);
        if (ret != 1) {
            throw OrderExceptionEnum.ORDER_AUTO_NO_GEN_ERROR.msg();
        }
        return orderAutoNoMapper.selectOneByBizTag(bizTag);
    }
}
