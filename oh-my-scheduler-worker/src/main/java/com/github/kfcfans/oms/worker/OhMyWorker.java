package com.github.kfcfans.oms.worker;

import akka.actor.ActorSystem;
import akka.actor.Props;
import com.github.kfcfans.oms.worker.actors.ProcessorTrackerActor;
import com.github.kfcfans.oms.worker.actors.TaskTrackerActor;
import com.github.kfcfans.oms.worker.background.WorkerHealthReportRunnable;
import com.github.kfcfans.oms.worker.common.OhMyConfig;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.oms.worker.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.common.utils.SpringUtils;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 客户端启动类
 *
 * @author KFCFans
 * @since 2020/3/16
 */
@Slf4j
public class OhMyWorker implements ApplicationContextAware, InitializingBean {

    @Getter
    private static OhMyConfig config;
    @Getter
    private static String currentServer;
    @Getter
    private static String workerAddress;

    public static ActorSystem actorSystem;
    private static ScheduledExecutorService timingPool;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.inject(applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void init() {

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("[OhMyWorker] start to initialize OhMyWorker...");

        try {

            // 初始化 ActorSystem
            Map<String, Object> overrideConfig = Maps.newHashMap();
            String localIP = StringUtils.isEmpty(config.getListeningIP()) ? NetUtils.getLocalHost() : config.getListeningIP();
            int port = config.getListeningPort() == null ? RemoteConstant.DEFAULT_CLIENT_PORT : config.getListeningPort();
            overrideConfig.put("akka.remote.artery.canonical.hostname", localIP);
            overrideConfig.put("akka.remote.artery.canonical.port", port);
            workerAddress = localIP + ":" + port;
            log.info("[OhMyWorker] akka-remote listening address: {}", workerAddress);

            Config akkaBasicConfig = ConfigFactory.load(RemoteConstant.AKKA_CONFIG_NAME);
            Config akkaFinalConfig = ConfigFactory.parseMap(overrideConfig).withFallback(akkaBasicConfig);

            actorSystem = ActorSystem.create(RemoteConstant.ACTOR_SYSTEM_NAME, akkaFinalConfig);
            actorSystem.actorOf(Props.create(TaskTrackerActor.class), RemoteConstant.Task_TRACKER_ACTOR_NAME);
            actorSystem.actorOf(Props.create(ProcessorTrackerActor.class), RemoteConstant.PROCESSOR_TRACKER_ACTOR_NAME);
            log.info("[OhMyWorker] akka ActorSystem({}) initialized successfully.", actorSystem);

            // 初始化存储
            TaskPersistenceService.INSTANCE.init();
            log.info("[OhMyWorker] local storage initialized successfully.");

            // 初始化定时任务
            ThreadFactory timingPoolFactory = new ThreadFactoryBuilder().setNameFormat("oms-worker-timing-pool-%d").build();
            timingPool = Executors.newScheduledThreadPool(2, timingPoolFactory);
            timingPool.scheduleAtFixedRate(new WorkerHealthReportRunnable(), 0, 30, TimeUnit.SECONDS);


            log.info("[OhMyWorker] OhMyWorker initialized successfully, using time: {}, congratulations!", stopwatch);
        }catch (Exception e) {
            log.error("[OhMyWorker] initialize OhMyWorker failed, using {}.", stopwatch, e);
        }
    }

    public void setConfig(OhMyConfig config) {
        OhMyWorker.config = config;
    }
}
