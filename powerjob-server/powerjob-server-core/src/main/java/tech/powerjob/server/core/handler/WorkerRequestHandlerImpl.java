package tech.powerjob.server.core.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.common.request.WorkerHeartbeat;
import tech.powerjob.common.request.WorkerLogReportReq;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.server.core.instance.InstanceLogService;
import tech.powerjob.server.core.instance.InstanceManager;
import tech.powerjob.server.core.workflow.WorkflowInstanceManager;
import tech.powerjob.server.monitor.MonitorService;
import tech.powerjob.server.monitor.events.w2s.TtReportInstanceStatusEvent;
import tech.powerjob.server.monitor.events.w2s.WorkerHeartbeatEvent;
import tech.powerjob.server.monitor.events.w2s.WorkerLogReportEvent;
import tech.powerjob.server.persistence.remote.repository.ContainerInfoRepository;
import tech.powerjob.server.remote.worker.WorkerClusterManagerService;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;

/**
 * receive and process worker's request
 *
 * @author tjq
 * @since 2022/9/11
 */
@Slf4j
@Component
@Actor(path = RemoteConstant.S4W_PATH)
public class WorkerRequestHandlerImpl extends AbWorkerRequestHandler {

    private final InstanceManager instanceManager;

    private final WorkflowInstanceManager workflowInstanceManager;

    private final InstanceLogService instanceLogService;

    public WorkerRequestHandlerImpl(InstanceManager instanceManager, WorkflowInstanceManager workflowInstanceManager, InstanceLogService instanceLogService,
                                    MonitorService monitorService, Environment environment, ContainerInfoRepository containerInfoRepository, WorkerClusterQueryService workerClusterQueryService) {
        super(monitorService, environment, containerInfoRepository, workerClusterQueryService);
        this.instanceManager = instanceManager;
        this.workflowInstanceManager = workflowInstanceManager;
        this.instanceLogService = instanceLogService;
    }

    @Override
    protected void processWorkerHeartbeat0(WorkerHeartbeat heartbeat, WorkerHeartbeatEvent event) {
        WorkerClusterManagerService.updateStatus(heartbeat);
    }

    @Override
    protected AskResponse processTaskTrackerReportInstanceStatus0(TaskTrackerReportInstanceStatusReq req, TtReportInstanceStatusEvent event) throws Exception {
        // 2021/02/05 如果是工作流中的实例先尝试更新上下文信息，再更新实例状态，这里一定不会有异常
        if (req.getWfInstanceId() != null && !CollectionUtils.isEmpty(req.getAppendedWfContext())) {
            // 更新工作流上下文信息
            workflowInstanceManager.updateWorkflowContext(req.getWfInstanceId(),req.getAppendedWfContext());
        }

        instanceManager.updateStatus(req);

        // 结束状态（成功/失败）需要回复消息
        if (InstanceStatus.FINISHED_STATUS.contains(req.getInstanceStatus())) {
            return AskResponse.succeed(null);
        }

        return null;
    }

    @Override
    protected void processWorkerLogReport0(WorkerLogReportReq req, WorkerLogReportEvent event) {
        // 这个效率应该不会拉垮吧...也就是一些判断 + Map#get 吧...
        instanceLogService.submitLogs(req.getWorkerAddress(), req.getInstanceLogContents());
    }
}
