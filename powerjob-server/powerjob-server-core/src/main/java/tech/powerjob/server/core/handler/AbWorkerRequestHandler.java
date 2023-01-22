package tech.powerjob.server.core.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.request.*;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.remote.framework.actor.Handler;
import tech.powerjob.remote.framework.actor.ProcessType;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.common.module.WorkerInfo;
import tech.powerjob.server.common.utils.SpringUtils;
import tech.powerjob.server.monitor.MonitorService;
import tech.powerjob.server.monitor.events.w2s.TtReportInstanceStatusEvent;
import tech.powerjob.server.monitor.events.w2s.WorkerHeartbeatEvent;
import tech.powerjob.server.monitor.events.w2s.WorkerLogReportEvent;
import tech.powerjob.server.persistence.remote.model.ContainerInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.repository.ContainerInfoRepository;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import static tech.powerjob.common.RemoteConstant.*;

/**
 * wrapper monitor for IWorkerRequestHandler
 *
 * @author tjq
 * @since 2022/9/11
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AbWorkerRequestHandler implements IWorkerRequestHandler {


    protected final MonitorService monitorService;

    protected final Environment environment;

    protected final ContainerInfoRepository containerInfoRepository;

    private final WorkerClusterQueryService workerClusterQueryService;

    protected abstract void processWorkerHeartbeat0(WorkerHeartbeat heartbeat, WorkerHeartbeatEvent event);

    protected abstract AskResponse processTaskTrackerReportInstanceStatus0(TaskTrackerReportInstanceStatusReq req, TtReportInstanceStatusEvent event) throws Exception;

    protected abstract void processWorkerLogReport0(WorkerLogReportReq req, WorkerLogReportEvent event);


    @Override
    @Handler(path = S4W_HANDLER_WORKER_HEARTBEAT, processType = ProcessType.NO_BLOCKING)
    public void processWorkerHeartbeat(WorkerHeartbeat heartbeat) {
        long startMs = System.currentTimeMillis();
        WorkerHeartbeatEvent event = new WorkerHeartbeatEvent()
                .setAppName(heartbeat.getAppName())
                .setAppId(heartbeat.getAppId())
                .setVersion(heartbeat.getVersion())
                .setProtocol(heartbeat.getProtocol())
                .setTag(heartbeat.getTag())
                .setWorkerAddress(heartbeat.getWorkerAddress())
                .setDelayMs(startMs - heartbeat.getHeartbeatTime())
                .setScore(heartbeat.getSystemMetrics().getScore());
        processWorkerHeartbeat0(heartbeat, event);
        monitorService.monitor(event);
    }

    @Override
    @Handler(path = S4W_HANDLER_REPORT_INSTANCE_STATUS, processType = ProcessType.BLOCKING)
    public AskResponse processTaskTrackerReportInstanceStatus(TaskTrackerReportInstanceStatusReq req) {
        long startMs = System.currentTimeMillis();
        TtReportInstanceStatusEvent event = new TtReportInstanceStatusEvent()
                .setAppId(req.getAppId())
                .setJobId(req.getJobId())
                .setInstanceId(req.getInstanceId())
                .setWfInstanceId(req.getWfInstanceId())
                .setInstanceStatus(InstanceStatus.of(req.getInstanceStatus()))
                .setDelayMs(startMs - req.getReportTime())
                .setServerProcessStatus(TtReportInstanceStatusEvent.Status.SUCCESS);
        try {
            return processTaskTrackerReportInstanceStatus0(req, event);
        } catch (Exception e) {
            event.setServerProcessStatus(TtReportInstanceStatusEvent.Status.FAILED);
            log.error("[WorkerRequestHandler] processTaskTrackerReportInstanceStatus failed for request: {}", req, e);
            return AskResponse.failed(ExceptionUtils.getMessage(e));
        } finally {
            event.setServerProcessCost(System.currentTimeMillis() - startMs);
            monitorService.monitor(event);
        }
    }

    @Override
    @Handler(path = S4W_HANDLER_REPORT_LOG, processType = ProcessType.NO_BLOCKING)
    public void processWorkerLogReport(WorkerLogReportReq req) {

        WorkerLogReportEvent event = new WorkerLogReportEvent()
                .setWorkerAddress(req.getWorkerAddress())
                .setLogNum(req.getInstanceLogContents().size());
        try {
            processWorkerLogReport0(req, event);
            event.setStatus(WorkerLogReportEvent.Status.SUCCESS);
        } catch (RejectedExecutionException re) {
            event.setStatus(WorkerLogReportEvent.Status.REJECTED);
        } catch (Throwable t) {
            event.setStatus(WorkerLogReportEvent.Status.EXCEPTION);
            log.warn("[WorkerRequestHandler] process worker report failed!", t);
        } finally {
            monitorService.monitor(event);
        }
    }

    @Override
    @Handler(path = S4W_HANDLER_QUERY_JOB_CLUSTER, processType = ProcessType.BLOCKING)
    public AskResponse processWorkerQueryExecutorCluster(WorkerQueryExecutorClusterReq req) {
        AskResponse askResponse;

        Long jobId = req.getJobId();
        Long appId = req.getAppId();

        JobInfoRepository jobInfoRepository = SpringUtils.getBean(JobInfoRepository.class);
        Optional<JobInfoDO> jobInfoOpt = jobInfoRepository.findById(jobId);
        if (jobInfoOpt.isPresent()) {
            JobInfoDO jobInfo = jobInfoOpt.get();
            if (!jobInfo.getAppId().equals(appId)) {
                askResponse = AskResponse.failed("Permission Denied!");
            }else {
                List<String> sortedAvailableWorker = workerClusterQueryService.getSuitableWorkers(jobInfo)
                        .stream().map(WorkerInfo::getAddress).collect(Collectors.toList());
                askResponse = AskResponse.succeed(sortedAvailableWorker);
            }
        }else {
            askResponse = AskResponse.failed("can't find jobInfo by jobId: " + jobId);
        }
        return askResponse;
    }

    @Override
    @Handler(path = S4W_HANDLER_WORKER_NEED_DEPLOY_CONTAINER, processType = ProcessType.BLOCKING)
    public AskResponse processWorkerNeedDeployContainer(WorkerNeedDeployContainerRequest req) {
        String port = environment.getProperty("local.server.port");

        Optional<ContainerInfoDO> containerInfoOpt = containerInfoRepository.findById(req.getContainerId());
        AskResponse askResponse = new AskResponse();
        if (!containerInfoOpt.isPresent() || containerInfoOpt.get().getStatus() != SwitchableStatus.ENABLE.getV()) {
            askResponse.setSuccess(false);
            askResponse.setMessage("can't find container by id: " + req.getContainerId());
        }else {
            ContainerInfoDO containerInfo = containerInfoOpt.get();
            askResponse.setSuccess(true);

            ServerDeployContainerRequest dpReq = new ServerDeployContainerRequest();
            BeanUtils.copyProperties(containerInfo, dpReq);
            dpReq.setContainerId(containerInfo.getId());
            String downloadURL = String.format("http://%s:%s/container/downloadJar?version=%s", NetUtils.getLocalHost(), port, containerInfo.getVersion());
            dpReq.setDownloadURL(downloadURL);

            askResponse.setData(JsonUtils.toBytes(dpReq));
        }
        return askResponse;
    }
}
