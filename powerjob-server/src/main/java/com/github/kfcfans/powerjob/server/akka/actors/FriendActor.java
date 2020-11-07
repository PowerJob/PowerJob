package com.github.kfcfans.powerjob.server.akka.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.github.kfcfans.powerjob.server.akka.requests.FriendQueryWorkerClusterStatusReq;
import com.github.kfcfans.powerjob.server.akka.requests.Ping;
import com.github.kfcfans.powerjob.server.akka.requests.RunJobOrWorkflowReq;
import com.github.kfcfans.powerjob.server.common.utils.SpringUtils;
import com.github.kfcfans.powerjob.server.service.JobService;
import com.github.kfcfans.powerjob.server.service.ha.WorkerManagerService;
import com.github.kfcfans.powerjob.server.service.workflow.WorkflowService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 处理朋友们的信息（处理服务器与服务器之间的通讯）
 *
 * @author tjq
 * @since 2020/4/9
 */
@Slf4j
public class FriendActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Ping.class, this::onReceivePing)
                .match(RunJobOrWorkflowReq.class, this::onReceiveFriendResendRunRequest)
                .match(FriendQueryWorkerClusterStatusReq.class, this::onReceiveFriendQueryWorkerClusterStatusReq)
                .matchAny(obj -> log.warn("[FriendActor] receive unknown request: {}.", obj))
                .build();
    }

    /**
     * 处理存活检测的请求
     */
    private void onReceivePing(Ping ping) {
        getSender().tell(AskResponse.succeed(System.currentTimeMillis() - ping.getCurrentTime()), getSelf());
    }

    /**
     * 处理查询Worker节点的请求
     */
    private void onReceiveFriendQueryWorkerClusterStatusReq(FriendQueryWorkerClusterStatusReq req) {
        Map<String, SystemMetrics> workerInfo = WorkerManagerService.getActiveWorkerInfo(req.getAppId());
        AskResponse askResponse = AskResponse.succeed(workerInfo);
        getSender().tell(askResponse, getSelf());
    }

    /**
     * 处理 run 转发
     */
    private void onReceiveFriendResendRunRequest(RunJobOrWorkflowReq req) {
        try {
            Long resultId;
            switch (req.getType()) {
                case RunJobOrWorkflowReq.WORKFLOW:
                    resultId = SpringUtils.getBean(WorkflowService.class).runWorkflow(req.getId(), req.getAppId(), req.getParams(), req.getDelay());
                    break;
                case RunJobOrWorkflowReq.JOB:
                    resultId = SpringUtils.getBean(JobService.class).runJob(req.getId(), req.getParams(), req.getDelay());
                    break;
                default:
                    throw new PowerJobException("unknown type: " + req.getType());
            }
            getSender().tell(AskResponse.succeed(String.valueOf(resultId)), getSelf());
        } catch (Exception e) {
            log.error("[FriendActor] process run request [{}] failed!", req, e);
            getSender().tell(AskResponse.failed(e.getMessage()), getSelf());
        }
    }
}
