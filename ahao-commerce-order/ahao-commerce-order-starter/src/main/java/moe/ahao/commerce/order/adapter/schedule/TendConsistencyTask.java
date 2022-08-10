package moe.ahao.commerce.order.adapter.schedule;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.tend.consistency.core.manager.TaskScheduleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 一致性框架执行逻辑
 */
@Slf4j
@Component
public class TendConsistencyTask {

    /**
     * 一致性任务调度器
     */
    @Autowired
    private TaskScheduleManager taskScheduleManager;

    @XxlJob("consistencyRetryTask")
    public void execute() {
        try {
            taskScheduleManager.performanceTask();
        } catch (Exception e) {
            log.error("一致性任务调度时，发送异常", e);
        }
        XxlJobHelper.handleSuccess();
    }
}
