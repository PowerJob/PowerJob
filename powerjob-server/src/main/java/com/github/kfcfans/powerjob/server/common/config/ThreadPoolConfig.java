package com.github.kfcfans.powerjob.server.common.config;

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
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        // use SynchronousQueue
        executor.setQueueCapacity(0);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("omsTimingPool-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("[OmsTimingService] timing pool can't schedule job immediately, maybe some job using too much cpu times.");
            // 定时任务优先级较高，不惜一些代价都需要继续执行，开线程继续干～
            new Thread(r).start();
        });
        return executor;
    }

    @Bean("omsBackgroundPool")
    public Executor initBackgroundPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(8192);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("omsBackgroundPool-");
        executor.setRejectedExecutionHandler(new LogOnRejected());
        return executor;
    }

    // 引入 WebSocket 支持后需要手动初始化调度线程池
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
        scheduler.setThreadNamePrefix("omsSchedulerPool-");
        scheduler.setDaemon(true);
        return scheduler;
    }

    private static final class LogOnRejected implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor p) {
            log.error("[OmsThreadPool] Task({}) rejected from pool({}).", r, p);
        }
    }
}
