package tech.powerjob.worker.common;

import lombok.Data;
import tech.powerjob.common.model.WorkerAppInfo;
import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.worker.background.OmsLogHandler;
import tech.powerjob.worker.background.discovery.ServerDiscoveryService;
import tech.powerjob.worker.core.executor.ExecutorManager;
import tech.powerjob.worker.persistence.TaskPersistenceService;
import tech.powerjob.worker.processor.ProcessorLoader;

import java.util.Optional;

/**
 * store worker's runtime
 *
 * @author tjq
 * @since 2021/3/7
 */
@Data
public class WorkerRuntime {

    /**
     * App 基础信息
     */
    private WorkerAppInfo appInfo;
    /**
     * 当前执行器地址
     */
    private String workerAddress;
    /**
     * 用户配置
     */
    private PowerJobWorkerConfig workerConfig;
    /**
     * 通讯器
     */
    private Transporter transporter;
    /**
     * 处理器加载器
     */
    private ProcessorLoader processorLoader;

    private ExecutorManager executorManager;

    private OmsLogHandler omsLogHandler;

    private ServerDiscoveryService serverDiscoveryService;

    private TaskPersistenceService taskPersistenceService;

    public Long getAppId() {
        return Optional.ofNullable(appInfo.getAppId()).orElse(-1L);
    }
}
