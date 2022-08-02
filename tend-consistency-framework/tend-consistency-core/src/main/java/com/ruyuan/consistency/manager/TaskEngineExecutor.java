package com.ruyuan.consistency.manager;

import com.ruyuan.consistency.model.ConsistencyTaskInstance;

/**
 * 任务执行引擎接口
 *
 * @author zhonghuashishan
 **/
public interface TaskEngineExecutor {

    /**
     * 执行指定的任务实例
     *
     * @param taskInstance 任务实例信息
     */
    void executeTaskInstance(ConsistencyTaskInstance taskInstance);

    /**
     * 当执行任务失败的时候，执行该逻辑
     *
     * @param taskInstance 任务实例
     */
    void fallbackExecuteTask(ConsistencyTaskInstance taskInstance);

}
