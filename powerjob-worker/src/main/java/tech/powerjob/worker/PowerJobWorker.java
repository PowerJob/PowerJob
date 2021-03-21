package tech.powerjob.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.routing.RoundRobinPool;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.HttpUtils;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.worker.actors.ProcessorTrackerActor;
import tech.powerjob.worker.actors.TaskTrackerActor;
import tech.powerjob.worker.actors.TroubleshootingActor;
import tech.powerjob.worker.actors.WorkerActor;
import tech.powerjob.worker.background.OmsLogHandler;
import tech.powerjob.worker.background.ServerDiscoveryService;
import tech.powerjob.worker.background.WorkerHealthReporter;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.PowerBannerPrinter;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.utils.SpringUtils;
import tech.powerjob.worker.persistence.TaskPersistenceService;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 客户端启动类
 *
 * @author KFCFans
 * @since 2020/3/16
 */
@Slf4j
public class PowerJobWorker implements ApplicationContextAware, InitializingBean, DisposableBean {

    private ScheduledExecutorService timingPool;
    private final WorkerRuntime workerRuntime = new WorkerRuntime();
    private final AtomicBoolean initialized = new AtomicBoolean();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.inject(applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void init() throws Exception {

        if (!initialized.compareAndSet(false, true)) {
            log.warn("[PowerJobWorker] please do not repeat the initialization");
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("[PowerJobWorker] start to initialize PowerJobWorker...");

        PowerJobWorkerConfig config = workerRuntime.getWorkerConfig();
        CommonUtils.requireNonNull(config, "can't find OhMyConfig, please set OhMyConfig first");

        try {
            PowerBannerPrinter.print();
            // 校验 appName
            if (!config.isEnableTestMode()) {
                assertAppName();
            }else {
                log.warn("[PowerJobWorker] using TestMode now, it's dangerous if this is production env.");
            }

            // 初始化元数据
            String workerAddress = NetUtils.getLocalHost() + ":" + config.getPort();
            workerRuntime.setWorkerAddress(workerAddress);

            // 初始化定时线程池
            ThreadFactory timingPoolFactory = new ThreadFactoryBuilder().setNameFormat("oms-worker-timing-pool-%d").build();
            timingPool = Executors.newScheduledThreadPool(3, timingPoolFactory);

            // 连接 server
            ServerDiscoveryService serverDiscoveryService = new ServerDiscoveryService(workerRuntime.getAppId(), workerRuntime.getWorkerConfig());
            serverDiscoveryService.start(timingPool);
            workerRuntime.setServerDiscoveryService(serverDiscoveryService);

            // 初始化 ActorSystem（macOS上 new ServerSocket 检测端口占用的方法并不生效，可能是AKKA是Scala写的缘故？没办法...只能靠异常重试了）
            Map<String, Object> overrideConfig = Maps.newHashMap();
            overrideConfig.put("akka.remote.artery.canonical.hostname", NetUtils.getLocalHost());
            overrideConfig.put("akka.remote.artery.canonical.port", config.getPort());

            Config akkaBasicConfig = ConfigFactory.load(RemoteConstant.WORKER_AKKA_CONFIG_NAME);
            Config akkaFinalConfig = ConfigFactory.parseMap(overrideConfig).withFallback(akkaBasicConfig);

            int cores = Runtime.getRuntime().availableProcessors();
            ActorSystem actorSystem = ActorSystem.create(RemoteConstant.WORKER_ACTOR_SYSTEM_NAME, akkaFinalConfig);
            workerRuntime.setActorSystem(actorSystem);

            ActorRef taskTrackerActorRef = actorSystem.actorOf(TaskTrackerActor.props(workerRuntime)
                    .withDispatcher("akka.task-tracker-dispatcher")
                    .withRouter(new RoundRobinPool(cores * 2)), RemoteConstant.TASK_TRACKER_ACTOR_NAME);
            actorSystem.actorOf(ProcessorTrackerActor.props(workerRuntime)
                    .withDispatcher("akka.processor-tracker-dispatcher")
                    .withRouter(new RoundRobinPool(cores)), RemoteConstant.PROCESSOR_TRACKER_ACTOR_NAME);
            actorSystem.actorOf(WorkerActor.props(taskTrackerActorRef)
                    .withDispatcher("akka.worker-common-dispatcher")
                    .withRouter(new RoundRobinPool(cores)), RemoteConstant.WORKER_ACTOR_NAME);

            // 处理系统中产生的异常情况
            ActorRef troubleshootingActor = actorSystem.actorOf(Props.create(TroubleshootingActor.class), RemoteConstant.TROUBLESHOOTING_ACTOR_NAME);
            actorSystem.eventStream().subscribe(troubleshootingActor, DeadLetter.class);

            log.info("[PowerJobWorker] akka-remote listening address: {}", workerAddress);
            log.info("[PowerJobWorker] akka ActorSystem({}) initialized successfully.", actorSystem);

            // 初始化日志系统
            OmsLogHandler omsLogHandler = new OmsLogHandler(workerAddress, actorSystem, serverDiscoveryService);
            workerRuntime.setOmsLogHandler(omsLogHandler);

            // 初始化存储
            TaskPersistenceService taskPersistenceService = new TaskPersistenceService(workerRuntime.getWorkerConfig().getStoreStrategy());
            taskPersistenceService.init();
            workerRuntime.setTaskPersistenceService(taskPersistenceService);
            log.info("[PowerJobWorker] local storage initialized successfully.");

            // 初始化定时任务
            timingPool.scheduleAtFixedRate(new WorkerHealthReporter(workerRuntime), 0, 15, TimeUnit.SECONDS);
            timingPool.scheduleWithFixedDelay(omsLogHandler.logSubmitter, 0, 5, TimeUnit.SECONDS);

            log.info("[PowerJobWorker] PowerJobWorker initialized successfully, using time: {}, congratulations!", stopwatch);
        }catch (Exception e) {
            log.error("[PowerJobWorker] initialize PowerJobWorker failed, using {}.", stopwatch, e);
            throw e;
        }
    }

    public void setConfig(PowerJobWorkerConfig config) {
        workerRuntime.setWorkerConfig(config);
    }

    @SuppressWarnings("rawtypes")
    private void assertAppName() {

        PowerJobWorkerConfig config = workerRuntime.getWorkerConfig();
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
                    log.info("[PowerJobWorker] assert appName({}) succeed, the appId for this application is {}.", appName, appId);
                    workerRuntime.setAppId(appId);
                    return;
                }else {
                    log.error("[PowerJobWorker] assert appName failed, this appName is invalid, please register the appName {} first.", appName);
                    throw new PowerJobException(resultDTO.getMessage());
                }
            }catch (PowerJobException oe) {
                throw oe;
            }catch (Exception ignore) {
                log.warn("[PowerJobWorker] assert appName by url({}) failed, please check the server address.", realUrl);
            }
        }
        log.error("[PowerJobWorker] no available server in {}.", config.getServerAddress());
        throw new PowerJobException("no server available!");
    }

    @Override
    public void destroy() throws Exception {
        timingPool.shutdownNow();
        workerRuntime.getActorSystem().terminate();
    }
}
