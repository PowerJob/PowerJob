package tech.powerjob.server.config;

import tech.powerjob.server.common.RejectedExecutionHandlerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.*;

/**
 * 公用线程池配置
 * omsTimingPool：用于执行定时任务的线程池
 * omsBackgroundPool：用于执行后台任务的线程池，这类任务对时间不敏感，慢慢执行细水长流即可
 * taskScheduler：用于定时调度的线程池
 *
 * @author tjq
 * @since 2020/4/28
 */
@Slf4j
@EnableAsync
@Configuration
public class ThreadPoolConfig {

    @Bean("omsTimingPool")
    public Executor getTimingPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 4);
        // use SynchronousQueue
        executor.setQueueCapacity(0);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("omsTimingPool-");
        executor.setRejectedExecutionHandler(RejectedExecutionHandlerFactory.newThreadRun("PowerJobTiming"));
        return executor;
    }

    @Bean("omsBackgroundPool")
    public Executor initBackgroundPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 8);
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 16);
        executor.setQueueCapacity(8192);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("omsBackgroundPool-");
        executor.setRejectedExecutionHandler(RejectedExecutionHandlerFactory.newReject("PowerJobBackgroundPool"));
        return executor;
    }

    // 引入 WebSocket 支持后需要手动初始化调度线程池
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
        scheduler.setThreadNamePrefix("PowerJobSchedulePool-");
        scheduler.setDaemon(true);
        return scheduler;
    }

}
