package moe.ahao.commerce.order.infrastructure.component;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.tend.consistency.core.custom.alerter.ConsistencyFrameworkAlerter;
import moe.ahao.tend.consistency.core.infrastructure.repository.impl.mybatis.data.ConsistencyTaskInstance;
import org.springframework.stereotype.Component;

/**
 * 一致性框架告警器
 */
@Slf4j
@Component
public class TendConsistencyAlerter implements ConsistencyFrameworkAlerter {
    @Override
    public void sendAlertNotice(ConsistencyTaskInstance consistencyTaskInstance) {
        log.error("一致性任务执行失败，name={}, param={}", consistencyTaskInstance.getId(), consistencyTaskInstance.getTaskParameter());
    }
}
