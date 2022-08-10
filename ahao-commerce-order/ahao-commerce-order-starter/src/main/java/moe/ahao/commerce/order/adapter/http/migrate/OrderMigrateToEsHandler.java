package moe.ahao.commerce.order.adapter.http.migrate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.adapter.mq.handler.EsOrderSyncHandler;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.data.OrderInfoDO;
import moe.ahao.commerce.order.infrastructure.repository.impl.mybatis.mapper.OrderInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单es数据迁移
 */
@Component
@Slf4j
public class OrderMigrateToEsHandler extends AbstractMigrateToEsHandler<OrderInfoDO> {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private EsOrderSyncHandler esOrderSyncHandler;

    @Override
    protected void syncData(List<OrderInfoDO> list) throws Exception {
        esOrderSyncHandler.syncFullData(list);
    }

    @Override
    protected BaseMapper<OrderInfoDO> baseMapper() {
        return orderInfoMapper;
    }
}
