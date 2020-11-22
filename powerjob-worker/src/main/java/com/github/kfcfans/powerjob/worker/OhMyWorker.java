package com.github.kfcfans.powerjob.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.routing.RoundRobinPool;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.common.utils.CommonUtils;
import com.github.kfcfans.powerjob.common.utils.HttpUtils;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.worker.actors.ProcessorTrackerActor;
import com.github.kfcfans.powerjob.worker.actors.TaskTrackerActor;
import com.github.kfcfans.powerjob.worker.actors.TroubleshootingActor;
import com.github.kfcfans.powerjob.worker.actors.WorkerActor;
import com.github.kfcfans.powerjob.worker.background.OmsLogHandler;
import com.github.kfcfans.powerjob.worker.background.ServerDiscoveryService;
import com.github.kfcfans.powerjob.worker.background.WorkerHealthReporter;
import com.github.kfcfans.powerjob.worker.common.OhMyConfig;
import com.github.kfcfans.powerjob.worker.common.PowerBannerPrinter;
import com.github.kfcfans.powerjob.worker.common.utils.SpringUtils;
import com.github.kfcfans.powerjob.worker.persistence.TaskPersistenceService;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
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
public class OhMyWorker implements ApplicationContextAware, InitializingBean, DisposableBean {

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
            PowerBannerPrinter.print();
            // 校验 appName
            if (!config.isEnableTestMode()) {
                appId = assertAppName();
            }else {
                log.warn("[OhMyWorker] using TestMode now, it's dangerous if this is production env.");
            }

            // 初始化 ActorSystem（macOS上 new ServerSocket 检测端口占用的方法并不生效，可能是AKKA是Scala写的缘故？没办法...只能靠异常重试了）
            Map<String, Object> overrideConfig = Maps.newHashMap();
            overrideConfig.put("akka.remote.artery.canonical.hostname", NetUtils.getLocalHost());
            overrideConfig.put("akka.remote.artery.canonical.port", config.getPort());
            workerAddress = NetUtils.getLocalHost() + ":" + config.getPort();

            Config akkaBasicConfig = ConfigFactory.load(RemoteConstant.WORKER_AKKA_CONFIG_NAME);
            Config akkaFinalConfig = ConfigFactory.parseMap(overrideConfig).withFallback(akkaBasicConfig);

            int cores = Runtime.getRuntime().availableProcessors();
            actorSystem = ActorSystem.create(RemoteConstant.WORKER_ACTOR_SYSTEM_NAME, akkaFinalConfig);
            actorSystem.actorOf(Props.create(TaskTrackerActor.class)
                    .withDispatcher("akka.task-tracker-dispatcher")
                    .withRouter(new RoundRobinPool(cores * 2)), RemoteConstant.Task_TRACKER_ACTOR_NAME);
            actorSystem.actorOf(Props.create(ProcessorTrackerActor.class)
                    .withDispatcher("akka.processor-tracker-dispatcher")
                    .withRouter(new RoundRobinPool(cores)), RemoteConstant.PROCESSOR_TRACKER_ACTOR_NAME);
            actorSystem.actorOf(Props.create(WorkerActor.class)
                    .withDispatcher("akka.worker-common-dispatcher")
                    .withRouter(new RoundRobinPool(cores)), RemoteConstant.WORKER_ACTOR_NAME);

            // 处理系统中产生的异常情况
            ActorRef troubleshootingActor = actorSystem.actorOf(Props.create(TroubleshootingActor.class), RemoteConstant.TROUBLESHOOTING_ACTOR_NAME);
            actorSystem.eventStream().subscribe(troubleshootingActor, DeadLetter.class);

            log.info("[OhMyWorker] akka-remote listening address: {}", workerAddress);
            log.info("[OhMyWorker] akka ActorSystem({}) initialized successfully.", actorSystem);

            // 初始化存储
            TaskPersistenceService.INSTANCE.init();
            log.info("[OhMyWorker] local storage initialized successfully.");

            // 服务发现
            currentServer = ServerDiscoveryService.discovery();
            if (StringUtils.isEmpty(currentServer) && !config.isEnableTestMode()) {
                throw new RuntimeException("can't find any available server, this worker has been quarantined.");
            }
            log.info("[OhMyWorker] discovery server succeed, current server is {}.", currentServer);

            // 初始化定时任务
            ThreadFactory timingPoolFactory = new ThreadFactoryBuilder().setNameFormat("oms-worker-timing-pool-%d").build();
            timingPool = Executors.newScheduledThreadPool(3, timingPoolFactory);
            timingPool.scheduleAtFixedRate(new WorkerHealthReporter(), 0, 15, TimeUnit.SECONDS);
            timingPool.scheduleAtFixedRate(() -> currentServer = ServerDiscoveryService.discovery(), 10, 10, TimeUnit.SECONDS);
            timingPool.scheduleWithFixedDelay(OmsLogHandler.INSTANCE.logSubmitter, 0, 5, TimeUnit.SECONDS);

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
                ResultDTO resultDTO = JsonUtils.parseObject(resultDTOStr, ResultDTO.class);
                if (resultDTO.isSuccess()) {
                    Long appId = Long.valueOf(resultDTO.getData().toString());
                    log.info("[OhMyWorker] assert appName({}) succeed, the appId for this application is {}.", appName, appId);
                    return appId;
                }else {
                    log.error("[OhMyWorker] assert appName failed, this appName is invalid, please register the appName {} first.", appName);
                    throw new PowerJobException(resultDTO.getMessage());
                }
            }catch (PowerJobException oe) {
                throw oe;
            }catch (Exception ignore) {
                log.warn("[OhMyWorker] assert appName by url({}) failed, please check the server address.", realUrl);
            }
        }
        log.error("[OhMyWorker] no available server in {}.", config.getServerAddress());
        throw new PowerJobException("no server available!");
    }

    @Override
    public void destroy() throws Exception {
        timingPool.shutdownNow();
    }
}
