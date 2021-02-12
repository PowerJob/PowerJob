package com.github.kfcfans.powerjob.server.remote.worker.handler;

import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.server.remote.worker.cluster.WorkerInfo;
import com.github.kfcfans.powerjob.common.request.*;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.server.common.constans.SwitchableStatus;
import com.github.kfcfans.powerjob.server.common.utils.SpringUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.ContainerInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.ContainerInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.powerjob.server.service.InstanceLogService;
import com.github.kfcfans.powerjob.server.remote.worker.cluster.WorkerClusterManagerService;
import com.github.kfcfans.powerjob.server.service.instance.InstanceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * receive and process worker's request
 *
 * @author tjq
 * @since 2021/2/8
 */
@Slf4j
@Component
public class WorkerRequestHandler {

    @Resource
    private Environment environment;
    @Resource
    private InstanceManager instanceManager;
    @Resource
    private InstanceLogService instanceLogService;
    @Resource
    private ContainerInfoRepository containerInfoRepository;

    private static WorkerRequestHandler workerRequestHandler;

    /**
     * 处理 Worker 的心跳请求
     * @param heartbeat 心跳包
     */
    public void onReceiveWorkerHeartbeat(WorkerHeartbeat heartbeat) {
        WorkerClusterManagerService.updateStatus(heartbeat);
    }

    /**
     * 处理 instance 状态
     * @param req 任务实例的状态上报请求
     */
    public Optional<AskResponse> onReceiveTaskTrackerReportInstanceStatusReq(TaskTrackerReportInstanceStatusReq req) throws Exception {
        instanceManager.updateStatus(req);

        // 结束状态（成功/失败）需要回复消息
        if (InstanceStatus.finishedStatus.contains(req.getInstanceStatus())) {
            return Optional.of(AskResponse.succeed(null));
        }
        return Optional.empty();
    }

    /**
     * 处理OMS在线日志请求
     * @param req 日志请求
     */
    public void onReceiveWorkerLogReportReq(WorkerLogReportReq req) {
        // 这个效率应该不会拉垮吧...也就是一些判断 + Map#get 吧...
        instanceLogService.submitLogs(req.getWorkerAddress(), req.getInstanceLogContents());
    }

    /**
     * 处理 Worker容器部署请求
     * @param req 容器部署请求
     */
    public AskResponse onReceiveWorkerNeedDeployContainerRequest(WorkerNeedDeployContainerRequest req) {

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

    /**
     * 处理 worker 请求获取当前任务所有处理器节点的请求
     * @param req jobId + appId
     */
    public AskResponse onReceiveWorkerQueryExecutorClusterReq(WorkerQueryExecutorClusterReq req) {

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
                List<String> sortedAvailableWorker = WorkerClusterManagerService.getAvailableWorkers(appId, jobInfo.getMinCpuCores(), jobInfo.getMinMemorySpace(), jobInfo.getMinDiskSpace())
                        .stream().map(WorkerInfo::getAddress).collect(Collectors.toList());
                askResponse = AskResponse.succeed(sortedAvailableWorker);
            }
        }else {
            askResponse = AskResponse.failed("can't find jobInfo by jobId: " + jobId);
        }
        return askResponse;
    }

    public static WorkerRequestHandler getWorkerRequestHandler() {
        if (workerRequestHandler == null) {
            workerRequestHandler = SpringUtils.getBean(WorkerRequestHandler.class);
        }
        return workerRequestHandler;
    }
}
