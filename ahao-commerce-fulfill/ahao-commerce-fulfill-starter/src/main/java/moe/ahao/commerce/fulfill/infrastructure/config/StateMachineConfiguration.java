// package moe.ahao.commerce.fulfill.infrastructure.config;
//
// import io.seata.saga.engine.config.DbStateMachineConfig;
// import io.seata.saga.engine.impl.ProcessCtrlStateMachineEngine;
// import io.seata.saga.rm.StateMachineEngineHolder;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
// import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
//
// import javax.sql.DataSource;
// import java.io.IOException;
// import java.util.concurrent.ThreadPoolExecutor;
//
// @Configuration(proxyBeanMethods = false)
// public class StateMachineConfiguration {
//     @Bean("sagaAsyncThreadPool")
//     public ThreadPoolExecutorFactoryBean threadExecutor() {
//         ThreadPoolExecutorFactoryBean threadExecutor = new ThreadPoolExecutorFactoryBean();
//         threadExecutor.setThreadNamePrefix("SAGA_ASYNC_EXE_");
//         threadExecutor.setCorePoolSize(1);
//         threadExecutor.setMaxPoolSize(20);
//         return threadExecutor;
//     }
//
//     @Bean
//     public DbStateMachineConfig dbStateMachineConfig(@Qualifier("sagaAsyncThreadPool") ThreadPoolExecutorFactoryBean threadExecutor,
//                                                      DataSource dataSource) throws IOException {
//         DbStateMachineConfig dbStateMachineConfig = new DbStateMachineConfig();
//         dbStateMachineConfig.setDataSource(dataSource);
//         // 事件驱动执行时使用的线程池, 如果所有状态机都同步执行且不存在循环任务可以不需要
//         dbStateMachineConfig.setThreadPoolExecutor((ThreadPoolExecutor) threadExecutor.getObject());
//         dbStateMachineConfig.setResources(new PathMatchingResourcePatternResolver()
//             .getResources("classpath*:statelang/*.json"));
//         dbStateMachineConfig.setEnableAsync(true);
//         dbStateMachineConfig.setTxServiceGroup("default_tx_group");
//         return dbStateMachineConfig;
//     }
//
//     /**
//      * saga状态机实例
//      */
//     @Bean
//     public ProcessCtrlStateMachineEngine stateMachineEngine(DbStateMachineConfig dbStateMachineConfig) {
//         ProcessCtrlStateMachineEngine stateMachineEngine = new ProcessCtrlStateMachineEngine();
//         stateMachineEngine.setStateMachineConfig(dbStateMachineConfig);
//         return stateMachineEngine;
//     }
//
//     @Bean
//     public StateMachineEngineHolder stateMachineEngineHolder(ProcessCtrlStateMachineEngine stateMachineEngine) {
//         StateMachineEngineHolder stateMachineEngineHolder = new StateMachineEngineHolder();
//         stateMachineEngineHolder.setStateMachineEngine(stateMachineEngine);
//         return stateMachineEngineHolder;
//     }
// }
