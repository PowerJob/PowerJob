package com.github.kfcfans.powerjob.server.akka.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.request.*;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.server.common.constans.SwitchableStatus;
import com.github.kfcfans.powerjob.server.common.utils.SpringUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.ContainerInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.ContainerInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.powerjob.server.service.InstanceLogService;
import com.github.kfcfans.powerjob.server.service.instance.InstanceManager;
import com.github.kfcfans.powerjob.server.service.ha.WorkerManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Optional;

/**
 * 处理 Worker 请求
 *
 * @author tjq
 * @since 2020/3/30
 */
@Slf4j
public class ServerActor extends AbstractActor {

    private InstanceManager instanceManager;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WorkerHeartbeat.class, this::onReceiveWorkerHeartbeat)
                .match(TaskTrackerReportInstanceStatusReq.class, this::onReceiveTaskTrackerReportInstanceStatusReq)
                .match(WorkerLogReportReq.class, this::onReceiveWorkerLogReportReq)
                .match(WorkerNeedDeployContainerRequest.class, this::onReceiveWorkerNeedDeployContainerRequest)
                .match(WorkerQueryExecutorClusterReq.class, this::onReceiveWorkerQueryExecutorClusterReq)
                .matchAny(obj -> log.warn("[ServerActor] receive unknown request: {}.", obj))
                .build();
    }


    /**
     * 处理 Worker 的心跳请求
     * @param heartbeat 心跳包
     */
    private void onReceiveWorkerHeartbeat(WorkerHeartbeat heartbeat) {
        WorkerManagerService.updateStatus(heartbeat);
    }

    /**
     * 处理 instance 状态
     * @param req 任务实例的状态上报请求
     */
    private void onReceiveTaskTrackerReportInstanceStatusReq(TaskTrackerReportInstanceStatusReq req) {
        try {
            getInstanceManager().updateStatus(req);

            // 结束状态（成功/失败）需要回复消息
            if (InstanceStatus.finishedStatus.contains(req.getInstanceStatus())) {
                getSender().tell(AskResponse.succeed(null), getSelf());
            }
        }catch (Exception e) {
            log.error("[ServerActor] update instance status failed for request: {}.", req, e);
        }
    }

    /**
     * 处理OMS在线日志请求
     * @param req 日志请求
     */
    private void onReceiveWorkerLogReportReq(WorkerLogReportReq req) {
        // 这个效率应该不会拉垮吧...也就是一些判断 + Map#get 吧...
        SpringUtils.getBean(InstanceLogService.class).submitLogs(req.getWorkerAddress(), req.getInstanceLogContents());
    }

    /**
     * 处理 Worker容器部署请求
     * @param req 容器部署请求
     */
    private void onReceiveWorkerNeedDeployContainerRequest(WorkerNeedDeployContainerRequest req) {

        ContainerInfoRepository containerInfoRepository = SpringUtils.getBean(ContainerInfoRepository.class);
        Environment environment = SpringUtils.getBean(Environment.class);
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
        getSender().tell(askResponse, getSelf());
    }

    /**
     * 处理 worker 请求获取当前任务所有处理器节点的请求
     * @param req jobId + appId
     */
    private void onReceiveWorkerQueryExecutorClusterReq(WorkerQueryExecutorClusterReq req) {

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
                List<String> sortedAvailableWorker = WorkerManagerService.getSortedAvailableWorker(appId, jobInfo.getMinCpuCores(), jobInfo.getMinMemorySpace(), jobInfo.getMinDiskSpace());
                askResponse = AskResponse.succeed(sortedAvailableWorker);
            }
        }else {
            askResponse = AskResponse.failed("can't find jobInfo by jobId: " + jobId);
        }
        getSender().tell(askResponse, getSelf());
    }

    // 不需要加锁，从 Spring IOC 中重复取并没什么问题
    private InstanceManager getInstanceManager() {
        if (instanceManager == null) {
            instanceManager = SpringUtils.getBean(InstanceManager.class);
        }
        return instanceManager;
    }
}
