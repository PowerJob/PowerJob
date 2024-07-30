package tech.powerjob.worker;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.PowerJobDKey;
import tech.powerjob.common.model.WorkerAppInfo;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.PropertyUtils;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.ServerType;
import tech.powerjob.remote.framework.engine.EngineConfig;
import tech.powerjob.remote.framework.engine.EngineOutput;
import tech.powerjob.remote.framework.engine.RemoteEngine;
import tech.powerjob.remote.framework.engine.impl.PowerJobRemoteEngine;
import tech.powerjob.worker.actors.ProcessorTrackerActor;
import tech.powerjob.worker.actors.TaskTrackerActor;
import tech.powerjob.worker.actors.WorkerActor;
import tech.powerjob.worker.background.OmsLogHandler;
import tech.powerjob.worker.background.WorkerHealthReporter;
import tech.powerjob.worker.background.discovery.PowerJobServerDiscoveryService;
import tech.powerjob.worker.background.discovery.ServerDiscoveryService;
import tech.powerjob.worker.common.PowerBannerPrinter;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.utils.WorkerNetUtils;
import tech.powerjob.worker.core.executor.ExecutorManager;
import tech.powerjob.worker.extension.processor.ProcessorFactory;
import tech.powerjob.worker.persistence.DbTaskPersistenceService;
import tech.powerjob.worker.persistence.TaskPersistenceService;
import tech.powerjob.worker.processor.PowerJobProcessorLoader;
import tech.powerjob.worker.processor.ProcessorLoader;
import tech.powerjob.worker.processor.impl.BuiltInDefaultProcessorFactory;
import tech.powerjob.worker.processor.impl.JarContainerProcessorFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 客户端启动类
 *
 * @author KFCFans
 * @since 2020/3/16
 */
@Slf4j
public class PowerJobWorker {
    private final RemoteEngine remoteEngine;
    protected final WorkerRuntime workerRuntime;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public PowerJobWorker(PowerJobWorkerConfig config) {
        this.workerRuntime = new WorkerRuntime();
        this.remoteEngine = new PowerJobRemoteEngine();
        workerRuntime.setWorkerConfig(config);
    }

    public void init() throws Exception {

        if (!initialized.compareAndSet(false, true)) {
            log.warn("[PowerJobWorker] please do not repeat the initialization");
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("[PowerJobWorker] start to initialize PowerJobWorker...");

        PowerJobWorkerConfig config = workerRuntime.getWorkerConfig();
        CommonUtils.requireNonNull(config, "can't find PowerJobWorkerConfig, please set PowerJobWorkerConfig first");

        ServerDiscoveryService serverDiscoveryService = new PowerJobServerDiscoveryService(config);
        workerRuntime.setServerDiscoveryService(serverDiscoveryService);

        try {
            PowerBannerPrinter.print();

            // 在发第一个请求之前，完成真正 IP 的解析
            int localBindPort = config.getPort();
            String localBindIp = WorkerNetUtils.parseLocalBindIp(localBindPort, config.getServerAddress());

            // 校验 appName
            WorkerAppInfo appInfo = serverDiscoveryService.assertApp();
            workerRuntime.setAppInfo(appInfo);

            // 初始化网络数据，区别对待上报地址和本机绑定地址（对外统一使用上报地址）
            String externalIp = PropertyUtils.readProperty(PowerJobDKey.NT_EXTERNAL_ADDRESS, localBindIp);
            String externalPort = PropertyUtils.readProperty(PowerJobDKey.NT_EXTERNAL_PORT, String.valueOf(localBindPort));
            log.info("[PowerJobWorker] [ADDRESS_INFO] localBindIp: {}, localBindPort: {}; externalIp: {}, externalPort: {}", localBindIp, localBindPort, externalIp, externalPort);
            workerRuntime.setWorkerAddress(Address.toFullAddress(externalIp, Integer.parseInt(externalPort)));

            // 初始化 线程池
            final ExecutorManager executorManager = new ExecutorManager(workerRuntime.getWorkerConfig());
            workerRuntime.setExecutorManager(executorManager);

            // 初始化 ProcessorLoader
            ProcessorLoader processorLoader = buildProcessorLoader(workerRuntime);
            workerRuntime.setProcessorLoader(processorLoader);

            // 初始化 actor
            TaskTrackerActor taskTrackerActor = new TaskTrackerActor(workerRuntime);
            ProcessorTrackerActor processorTrackerActor = new ProcessorTrackerActor(workerRuntime);
            WorkerActor workerActor = new WorkerActor(workerRuntime, taskTrackerActor);

            // 初始化通讯引擎
            EngineConfig engineConfig = new EngineConfig()
                    .setType(config.getProtocol().name())
                    .setServerType(ServerType.WORKER)
                    .setBindAddress(new Address().setHost(localBindIp).setPort(localBindPort))
                    .setActorList(Lists.newArrayList(taskTrackerActor, processorTrackerActor, workerActor));

            EngineOutput engineOutput = remoteEngine.start(engineConfig);
            workerRuntime.setTransporter(engineOutput.getTransporter());

            // 连接 server
            serverDiscoveryService.timingCheck(workerRuntime.getExecutorManager().getCoreExecutor());

            log.info("[PowerJobWorker] PowerJobRemoteEngine initialized successfully.");

            // 初始化日志系统
            OmsLogHandler omsLogHandler = new OmsLogHandler(workerRuntime.getWorkerAddress(), workerRuntime.getTransporter(), serverDiscoveryService);
            workerRuntime.setOmsLogHandler(omsLogHandler);

            // 初始化存储
            TaskPersistenceService taskPersistenceService = new DbTaskPersistenceService(workerRuntime.getWorkerConfig().getStoreStrategy());
            taskPersistenceService.init();
            workerRuntime.setTaskPersistenceService(taskPersistenceService);
            log.info("[PowerJobWorker] local storage initialized successfully.");


            // 初始化定时任务
            workerRuntime.getExecutorManager().getCoreExecutor().scheduleAtFixedRate(new WorkerHealthReporter(workerRuntime), 0, config.getHealthReportInterval(), TimeUnit.SECONDS);
            workerRuntime.getExecutorManager().getCoreExecutor().scheduleWithFixedDelay(omsLogHandler.logSubmitter, 0, 5, TimeUnit.SECONDS);

            log.info("[PowerJobWorker] PowerJobWorker initialized successfully, using time: {}, congratulations!", stopwatch);
        }catch (Exception e) {
            log.error("[PowerJobWorker] initialize PowerJobWorker failed, using {}.", stopwatch, e);
            throw e;
        }
    }

    private ProcessorLoader buildProcessorLoader(WorkerRuntime runtime) {
        List<ProcessorFactory> customPF = Optional.ofNullable(runtime.getWorkerConfig().getProcessorFactoryList()).orElse(Collections.emptyList());
        List<ProcessorFactory> finalPF = Lists.newArrayList(customPF);

        // 后置添加2个系统 ProcessorLoader
        finalPF.add(new BuiltInDefaultProcessorFactory());
        finalPF.add(new JarContainerProcessorFactory(runtime));

        return new PowerJobProcessorLoader(finalPF);
    }

    public void destroy() throws Exception {
        workerRuntime.getExecutorManager().shutdown();
        remoteEngine.close();
    }
}
