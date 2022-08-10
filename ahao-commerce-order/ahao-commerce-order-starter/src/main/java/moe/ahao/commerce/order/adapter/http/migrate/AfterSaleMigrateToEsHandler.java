package moe.ahao.commerce.order.adapter.http.migrate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.order.adapter.mq.handler.EsAfterSaleSyncHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 售后单es数据迁移
 */
@Component
@Slf4j
public class AfterSaleMigrateToEsHandler extends AbstractMigrateToEsHandler<AfterSaleInfoDO>{
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;
    @Autowired
    private EsAfterSaleSyncHandler esAfterSaleSyncHandler;

    @Override
    protected void syncData(List<AfterSaleInfoDO> list) throws Exception {
        esAfterSaleSyncHandler.syncFullData(list);
    }

    @Override
    protected BaseMapper<AfterSaleInfoDO> baseMapper() {
        return afterSaleInfoMapper;
    }
}
