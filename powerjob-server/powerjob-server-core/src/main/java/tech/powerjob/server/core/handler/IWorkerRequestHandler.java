package tech.powerjob.server.core.handler;

import tech.powerjob.common.request.*;
import tech.powerjob.common.response.AskResponse;

/**
 * 定义 server 与 worker 之间需要处理的协议
 *
 * @author tjq
 * @since 2022/9/10
 */
public interface IWorkerRequestHandler {

    /**
     * 处理 worker 上报的心跳信息
     * @param heartbeat 心跳信息
     */
    void processWorkerHeartbeat(WorkerHeartbeat heartbeat);

    /**
     * 处理 TaskTracker 的任务实例上报
     * @param req 上报请求
     * @return 响应信息
     */
    AskResponse processTaskTrackerReportInstanceStatus(TaskTrackerReportInstanceStatusReq req);

    /**
     * 处理 worker 查询执行器集群
     * @param req 请求
     * @return cluster info
     */
    AskResponse processWorkerQueryExecutorCluster(WorkerQueryExecutorClusterReq req);

    /**
     * 处理 worker 日志推送请求（内部使用线程池异步处理，非阻塞）
     * @param req 请求
     */
    void processWorkerLogReport(WorkerLogReportReq req);

    /**
     * 处理 worker 的容器部署请求
     * @param request 请求
     * @return 容器部署信息
     */
    AskResponse processWorkerNeedDeployContainer(WorkerNeedDeployContainerRequest request);
}
