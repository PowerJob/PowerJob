package tech.powerjob.worker.core.tracker.task;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.model.JobAdvancedRuntimeConfig;
import tech.powerjob.common.request.ServerQueryInstanceStatusReq;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.utils.TransportUtils;
import tech.powerjob.worker.pojo.model.InstanceInfo;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Echo009
 * @since 2022/9/19
 */
@Slf4j
public abstract class TaskTracker {

    /**
     * TaskTracker创建时间
     */
    protected final long createTime;
    /**
     * 任务实例ID，使用频率过高，从 InstanceInfo 提取出来单独保存一份
     */
    protected final long instanceId;
    /**
     * 任务实例信息
     */
    protected final InstanceInfo instanceInfo;
    protected final ExecuteType executeType;

    protected final JobAdvancedRuntimeConfig advancedRuntimeConfig;
    /**
     * 追加的工作流上下文数据
     *
     * @since 2021/02/05
     */
    protected final Map<String, String> appendedWfContext;
    /**
     * worker 运行时元数据
     */
    protected final WorkerRuntime workerRuntime;
    /**
     * 是否结束
     */
    protected final AtomicBoolean finished;
    /**
     * 连续上报多次失败后放弃上报，视为结果不可达，TaskTracker down
     */
    protected int reportFailedCnt = 0;

    protected static final int MAX_REPORT_FAILED_THRESHOLD = 5;

    protected TaskTracker(ServerScheduleJobReq req, WorkerRuntime workerRuntime) {
        this.createTime = System.currentTimeMillis();
        this.workerRuntime = workerRuntime;
        this.instanceId = req.getInstanceId();

        this.instanceInfo = new InstanceInfo();

        // PowerJob 值拷贝场景不多，引入三方值拷贝类库可能引入类冲突等问题，综合评估手写 ROI 最高
        instanceInfo.setJobId(req.getJobId());
        instanceInfo.setInstanceId(req.getInstanceId());
        instanceInfo.setWfInstanceId(req.getWfInstanceId());
        instanceInfo.setExecuteType(req.getExecuteType());
        instanceInfo.setProcessorType(req.getProcessorType());
        instanceInfo.setProcessorInfo(req.getProcessorInfo());
        instanceInfo.setJobParams(req.getJobParams());
        instanceInfo.setInstanceParams(req.getInstanceParams());
        instanceInfo.setThreadConcurrency(req.getThreadConcurrency());
        instanceInfo.setTaskRetryNum(req.getTaskRetryNum());
        instanceInfo.setLogConfig(req.getLogConfig());
        instanceInfo.setInstanceTimeoutMS(req.getInstanceTimeoutMS());
        instanceInfo.setAdvancedRuntimeConfig(req.getAdvancedRuntimeConfig());

        // 常用变量初始化
        executeType = ExecuteType.valueOf(req.getExecuteType());
        advancedRuntimeConfig = Optional.ofNullable(req.getAdvancedRuntimeConfig()).map(x -> JsonUtils.parseObjectIgnoreException(x, JobAdvancedRuntimeConfig.class)).orElse(new JobAdvancedRuntimeConfig());

        // 特殊处理超时时间
        if (instanceInfo.getInstanceTimeoutMS() <= 0) {
            instanceInfo.setInstanceTimeoutMS(Integer.MAX_VALUE);
        }
        // 只有工作流中的任务允许向工作流中追加上下文数据
        this.appendedWfContext = req.getWfInstanceId() == null ? Collections.emptyMap() : Maps.newConcurrentMap();
        this.finished = new AtomicBoolean(false);
    }

    /**
     * 销毁
     */
    public abstract void destroy();

    /**
     * 停止任务
     */
    public abstract void stopTask();


    /**
     * 查询任务实例的详细运行状态
     *
     * @return 任务实例的详细运行状态
     */
    public abstract InstanceDetail fetchRunningStatus(ServerQueryInstanceStatusReq req);


    public static void reportCreateErrorToServer(ServerScheduleJobReq req, WorkerRuntime workerRuntime, Exception e) {
        log.warn("[TaskTracker-{}] create TaskTracker from request({}) failed.", req.getInstanceId(), req, e);
        // 直接发送失败请求
        TaskTrackerReportInstanceStatusReq response = new TaskTrackerReportInstanceStatusReq();

        response.setAppId(workerRuntime.getAppId());
        response.setJobId(req.getJobId());
        response.setInstanceId(req.getInstanceId());
        response.setWfInstanceId(req.getWfInstanceId());

        response.setInstanceStatus(InstanceStatus.FAILED.getV());
        response.setResult(String.format("init TaskTracker failed, reason: %s", e.toString()));
        response.setReportTime(System.currentTimeMillis());
        response.setStartTime(System.currentTimeMillis());
        response.setSourceAddress(workerRuntime.getWorkerAddress());

        TransportUtils.ttReportInstanceStatus(response, workerRuntime.getServerDiscoveryService().getCurrentServerAddress(), workerRuntime.getTransporter());
    }

    protected void reportFinalStatusThenDestroy(WorkerRuntime workerRuntime, TaskTrackerReportInstanceStatusReq reportInstanceStatusReq) {
        String currentServerAddress = workerRuntime.getServerDiscoveryService().getCurrentServerAddress();
        // 最终状态需要可靠上报
        boolean serverAccepted = false;
        try {
            serverAccepted = TransportUtils.reliableTtReportInstanceStatus(reportInstanceStatusReq, currentServerAddress, workerRuntime.getTransporter());
        } catch (Exception e) {
            log.warn("[TaskTracker-{}] report finished status failed, req={}.", instanceId, reportInstanceStatusReq, e);
        }
        if (!serverAccepted) {
            if (++reportFailedCnt > MAX_REPORT_FAILED_THRESHOLD) {
                log.error("[TaskTracker-{}] try to report finished status(detail={}) lots of times but all failed, it's time to give up, so the process result will be dropped", instanceId, reportInstanceStatusReq);
                destroy();
            }
            return;
        }
        log.info("[TaskTracker-{}] report finished status(detail={}) success", instanceId, reportInstanceStatusReq);
        destroy();
    }
}
