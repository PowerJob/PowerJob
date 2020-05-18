package com.github.kfcfans.oms.server.akka.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.oms.common.InstanceStatus;
import com.github.kfcfans.oms.common.request.ServerDeployContainerRequest;
import com.github.kfcfans.oms.common.request.TaskTrackerReportInstanceStatusReq;
import com.github.kfcfans.oms.common.request.WorkerHeartbeat;
import com.github.kfcfans.oms.common.request.WorkerLogReportReq;
import com.github.kfcfans.oms.common.request.http.WorkerNeedDeployContainerRequest;
import com.github.kfcfans.oms.common.response.AskResponse;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.common.utils.NetUtils;
import com.github.kfcfans.oms.server.common.utils.SpringUtils;
import com.github.kfcfans.oms.server.persistence.core.model.ContainerInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.ContainerInfoRepository;
import com.github.kfcfans.oms.server.service.InstanceLogService;
import com.github.kfcfans.oms.server.service.instance.InstanceManager;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;

import java.util.Optional;

/**
 * 处理 Worker 请求
 *
 * @author tjq
 * @since 2020/3/30
 */
@Slf4j
public class ServerActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WorkerHeartbeat.class, this::onReceiveWorkerHeartbeat)
                .match(TaskTrackerReportInstanceStatusReq.class, this::onReceiveTaskTrackerReportInstanceStatusReq)
                .match(WorkerLogReportReq.class, this::onReceiveWorkerLogReportReq)
                .match(WorkerNeedDeployContainerRequest.class, this::onReceiveWorkerNeedDeployContainerRequest)
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
            InstanceManager.updateStatus(req);

            // 结束状态（成功/失败）需要回复消息
            if (!InstanceStatus.generalizedRunningStatus.contains(req.getInstanceStatus())) {
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

        Optional<ContainerInfoDO> containerInfoOpt = containerInfoRepository.findByContainerName(req.getContainerName());
        AskResponse askResponse = new AskResponse();
        askResponse.setSuccess(false);
        if (containerInfoOpt.isPresent()) {
            ContainerInfoDO containerInfo = containerInfoOpt.get();
            askResponse.setSuccess(true);

            ServerDeployContainerRequest dpReq = new ServerDeployContainerRequest();
            BeanUtils.copyProperties(containerInfo, dpReq);
            String downloadURL = String.format("http://%s:%s/container/downloadJar?version=%s", NetUtils.getLocalHost(), port, containerInfo.getVersion());
            dpReq.setDownloadURL(downloadURL);

            askResponse.setData(JsonUtils.toBytes(dpReq));
        }

        getSender().tell(askResponse, getSelf());
    }
}
