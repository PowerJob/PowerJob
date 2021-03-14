package tech.powerjob.server.core.handler.impl;

import akka.actor.AbstractActor;
import tech.powerjob.common.request.*;
import tech.powerjob.common.response.AskResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static tech.powerjob.server.core.handler.WorkerRequestHandler.getWorkerRequestHandler;

/**
 * 处理 Worker 请求
 *
 * @author tjq
 * @since 2020/3/30
 */
@Slf4j
public class WorkerRequestAkkaHandler extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WorkerHeartbeat.class, hb -> getWorkerRequestHandler().onReceiveWorkerHeartbeat(hb))
                .match(TaskTrackerReportInstanceStatusReq.class, this::onReceiveTaskTrackerReportInstanceStatusReq)
                .match(WorkerLogReportReq.class, req -> getWorkerRequestHandler().onReceiveWorkerLogReportReq(req))
                .match(WorkerNeedDeployContainerRequest.class, this::onReceiveWorkerNeedDeployContainerRequest)
                .match(WorkerQueryExecutorClusterReq.class, this::onReceiveWorkerQueryExecutorClusterReq)
                .matchAny(obj -> log.warn("[WorkerRequestAkkaHandler] receive unknown request: {}.", obj))
                .build();
    }



    /**
     * 处理 instance 状态
     * @param req 任务实例的状态上报请求
     */
    private void onReceiveTaskTrackerReportInstanceStatusReq(TaskTrackerReportInstanceStatusReq req) {

        try {
            Optional<AskResponse> askResponseOpt = getWorkerRequestHandler().onReceiveTaskTrackerReportInstanceStatusReq(req);
            if (askResponseOpt.isPresent()) {
                getSender().tell(AskResponse.succeed(null), getSelf());
            }
        }catch (Exception e) {
            log.error("[WorkerRequestAkkaHandler] update instance status failed for request: {}.", req, e);
        }
    }

    /**
     * 处理 Worker容器部署请求
     * @param req 容器部署请求
     */
    private void onReceiveWorkerNeedDeployContainerRequest(WorkerNeedDeployContainerRequest req) {
        getSender().tell(getWorkerRequestHandler().onReceiveWorkerNeedDeployContainerRequest(req), getSelf());
    }

    /**
     * 处理 worker 请求获取当前任务所有处理器节点的请求
     * @param req jobId + appId
     */
    private void onReceiveWorkerQueryExecutorClusterReq(WorkerQueryExecutorClusterReq req) {

        getSender().tell(getWorkerRequestHandler().onReceiveWorkerQueryExecutorClusterReq(req), getSelf());
    }

}
