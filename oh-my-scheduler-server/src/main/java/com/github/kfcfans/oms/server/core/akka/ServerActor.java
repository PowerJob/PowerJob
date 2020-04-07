package com.github.kfcfans.oms.server.core.akka;

import akka.actor.AbstractActor;
import com.github.kfcfans.common.request.TaskTrackerReportInstanceStatusReq;
import com.github.kfcfans.common.request.WorkerHeartbeat;
import com.github.kfcfans.common.response.AskResponse;
import com.github.kfcfans.oms.server.core.InstanceManager;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import lombok.extern.slf4j.Slf4j;

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
                .match(Ping.class, this::onReceivePing)
                .matchAny(obj -> log.warn("[ServerActor] receive unknown request: {}.", obj))
                .build();
    }

    /**
     * 处理存活检测的请求
     * @param ping 存活检测请求
     */
    private void onReceivePing(Ping ping) {
        AskResponse askResponse = new AskResponse();
        askResponse.setSuccess(true);
        askResponse.setExtra(System.currentTimeMillis() - ping.getCurrentTime());
        getSender().tell(askResponse, getSelf());
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

            // 回复接收成功
            AskResponse askResponse = new AskResponse();
            askResponse.setSuccess(true);
            getSender().tell(askResponse, getSelf());
        }catch (Exception e) {
            log.error("[ServerActor] update instance status failed for request: {}.", req, e);
        }
    }
}
