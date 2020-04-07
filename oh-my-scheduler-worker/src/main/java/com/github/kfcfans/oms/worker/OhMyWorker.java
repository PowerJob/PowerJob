package com.github.kfcfans.oms.worker;

import akka.actor.ActorSystem;
import akka.actor.Props;
import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.oms.worker.actors.ProcessorTrackerActor;
import com.github.kfcfans.oms.worker.actors.TaskTrackerActor;
import com.github.kfcfans.oms.worker.background.ServerDiscoveryService;
import com.github.kfcfans.oms.worker.background.WorkerHealthReportRunnable;
import com.github.kfcfans.oms.worker.common.OhMyConfig;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.common.utils.HttpUtils;
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
import java.util.Objects;
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
    @Getter
    private static Long appId;
    private static ScheduledExecutorService timingPool;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.inject(applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void init() throws Exception {

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("[OhMyWorker] start to initialize OhMyWorker...");

        try {

            // 校验 appName
            appId = assertAppName();

            // 初始化 ActorSystem
            Map<String, Object> overrideConfig = Maps.newHashMap();
            int port = NetUtils.getAvailablePort(RemoteConstant.DEFAULT_WORKER_PORT);
            overrideConfig.put("akka.remote.artery.canonical.hostname", NetUtils.getLocalHost());
            overrideConfig.put("akka.remote.artery.canonical.port", port);
            workerAddress = NetUtils.getLocalHost() + ":" + port;
            log.info("[OhMyWorker] akka-remote listening address: {}", workerAddress);

            Config akkaBasicConfig = ConfigFactory.load(RemoteConstant.WORKER_AKKA_CONFIG_NAME);
            Config akkaFinalConfig = ConfigFactory.parseMap(overrideConfig).withFallback(akkaBasicConfig);

            actorSystem = ActorSystem.create(RemoteConstant.WORKER_ACTOR_SYSTEM_NAME, akkaFinalConfig);
            actorSystem.actorOf(Props.create(TaskTrackerActor.class), RemoteConstant.Task_TRACKER_ACTOR_NAME);
            actorSystem.actorOf(Props.create(ProcessorTrackerActor.class), RemoteConstant.PROCESSOR_TRACKER_ACTOR_NAME);
            log.info("[OhMyWorker] akka ActorSystem({}) initialized successfully.", actorSystem);

            // 初始化存储
            TaskPersistenceService.INSTANCE.init();
            log.info("[OhMyWorker] local storage initialized successfully.");

            // 服务发现
            currentServer = ServerDiscoveryService.discovery();
            if (StringUtils.isEmpty(currentServer)) {
                throw new RuntimeException("can't find any available server, this worker has been quarantined.");
            }
            log.info("[OhMyWorker] discovery server succeed, current server is {}.", currentServer);

            // 初始化定时任务
            ThreadFactory timingPoolFactory = new ThreadFactoryBuilder().setNameFormat("oms-worker-timing-pool-%d").build();
            timingPool = Executors.newScheduledThreadPool(2, timingPoolFactory);
            timingPool.scheduleAtFixedRate(new WorkerHealthReportRunnable(), 0, 15, TimeUnit.SECONDS);
            timingPool.scheduleAtFixedRate(() -> currentServer = ServerDiscoveryService.discovery(), 10, 10, TimeUnit.SECONDS);

            log.info("[OhMyWorker] OhMyWorker initialized successfully, using time: {}, congratulations!", stopwatch);
        }catch (Exception e) {
            log.error("[OhMyWorker] initialize OhMyWorker failed, using {}.", stopwatch, e);
            throw e;
        }
    }

    public void setConfig(OhMyConfig config) {
        OhMyWorker.config = config;
    }

    @SuppressWarnings("rawtypes")
    private Long assertAppName() {

        String appName = config.getAppName();
        Objects.requireNonNull(appName, "appName can't be empty!");

        String url = "http://%s/server/assert?appName=%s";
        for (String server : config.getServerAddress()) {
            String realUrl = String.format(url, server, appName);
            try {
                String resultDTOStr = CommonUtils.executeWithRetry0(() -> HttpUtils.get(realUrl));
                ResultDTO resultDTO = JSONObject.parseObject(resultDTOStr, ResultDTO.class);
                if (resultDTO.isSuccess()) {
                    Long appId = Long.valueOf(resultDTO.getData().toString());
                    log.info("[OhMyWorker] assert appName({}) succeed, the appId for this application is {}.", appName, appId);
                    return appId;
                }else {
                    log.error("[OhMyWorker] assert appName failed, this appName is invalid, please register the appName {} first.", appName);
                    throw new IllegalArgumentException("appName invalid!");
                }
            }catch (IllegalArgumentException ie) {
                throw ie;
            }catch (Exception ignore) {
            }
        }
        log.error("[OhMyWorker] no available server in {}.", config.getServerAddress());
        throw new RuntimeException("no server available!");
    }
}
