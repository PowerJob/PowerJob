package tech.powerjob.server.core.scheduler;

import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.SystemInstanceResult;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.enums.WorkflowInstanceStatus;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.server.remote.transport.starter.AkkaStarter;
import tech.powerjob.server.persistence.remote.model.*;
import tech.powerjob.server.persistence.remote.repository.*;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.instance.InstanceManager;
import tech.powerjob.server.core.workflow.WorkflowInstanceManager;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 定时状态检查
 *
 * @author tjq
 * @since 2020/4/7
 */
@Slf4j
@Service
public class InstanceStatusCheckService {

    private static final int MAX_BATCH_NUM = 10;
    private static final long DISPATCH_TIMEOUT_MS = 30000;
    private static final long RECEIVE_TIMEOUT_MS = 60000;
    private static final long RUNNING_TIMEOUT_MS = 60000;
    private static final long WORKFLOW_WAITING_TIMEOUT_MS = 60000;

    @Resource
    private DispatchService dispatchService;
    @Resource
    private InstanceManager instanceManager;
    @Resource
    private WorkflowInstanceManager workflowInstanceManager;

    @Resource
    private AppInfoRepository appInfoRepository;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;
    @Resource
    private WorkflowInfoRepository workflowInfoRepository;
    @Resource
    private WorkflowInstanceInfoRepository workflowInstanceInfoRepository;

    @Async("omsTimingPool")
    @Scheduled(fixedDelay = 10000)
    public void timingStatusCheck() {
        Stopwatch stopwatch = Stopwatch.createStarted();

        // 查询DB获取该Server需要负责的AppGroup
        List<AppInfoDO> appInfoList = appInfoRepository.findAllByCurrentServer(AkkaStarter.getActorSystemAddress());
        if (CollectionUtils.isEmpty(appInfoList)) {
            log.info("[InstanceStatusChecker] current server has no app's job to check");
            return;
        }
        List<Long> allAppIds = appInfoList.stream().map(AppInfoDO::getId).collect(Collectors.toList());

        try {
            checkInstance(allAppIds);
            checkWorkflowInstance(allAppIds);
        } catch (Exception e) {
            log.error("[InstanceStatusChecker] status check failed.", e);
        }
        log.info("[InstanceStatusChecker] status check using {}.", stopwatch.stop());
    }

    /**
     * 检查任务实例的状态，发现异常及时重试，包括
     * WAITING_DISPATCH 超时：写入时间轮但未调度前 server down
     * WAITING_WORKER_RECEIVE 超时：由于网络错误导致 worker 未接受成功
     * RUNNING 超时：TaskTracker down，断开与 server 的心跳连接
     *
     * @param allAppIds 本系统所承担的所有 appIds
     */
    private void checkInstance(List<Long> allAppIds) {

        Lists.partition(allAppIds, MAX_BATCH_NUM).forEach(partAppIds -> {
            // 1. 检查等待 WAITING_DISPATCH 状态的任务
            handleWaitingDispatchInstance(partAppIds);
            // 2. 检查 WAITING_WORKER_RECEIVE 状态的任务
            handleWaitingWorkerReceiveInstance(partAppIds);
            // 3. 检查 RUNNING 状态的任务（一定时间内没收到 TaskTracker 的状态报告，视为失败）
            handleRunningInstance(partAppIds);
        });
    }


    private void handleWaitingDispatchInstance(List<Long> partAppIds) {
        // 1. 检查等待 WAITING_DISPATCH 状态的任务
        long threshold = System.currentTimeMillis() - DISPATCH_TIMEOUT_MS;
        List<InstanceInfoDO> waitingDispatchInstances = instanceInfoRepository.findByAppIdInAndStatusAndExpectedTriggerTimeLessThan(partAppIds, InstanceStatus.WAITING_DISPATCH.getV(), threshold);
        if (!CollectionUtils.isEmpty(waitingDispatchInstances)) {
            log.warn("[InstanceStatusChecker] find some instance which is not triggered as expected: {}", waitingDispatchInstances);
            waitingDispatchInstances.forEach(instance -> {

                Optional<JobInfoDO> jobInfoOpt = jobInfoRepository.findById(instance.getJobId());
                if (jobInfoOpt.isPresent()) {
                    dispatchService.redispatch(jobInfoOpt.get(), instance.getInstanceId());
                } else {
                    log.warn("[InstanceStatusChecker] can't find job by jobId[{}], so redispatch failed, failed instance: {}", instance.getJobId(), instance);
                    updateFailedInstance(instance, SystemInstanceResult.CAN_NOT_FIND_JOB_INFO);
                }
            });
        }
    }

    private void handleWaitingWorkerReceiveInstance(List<Long> partAppIds) {
        // 2. 检查 WAITING_WORKER_RECEIVE 状态的任务
        long threshold = System.currentTimeMillis() - RECEIVE_TIMEOUT_MS;
        List<InstanceInfoDO> waitingWorkerReceiveInstances = instanceInfoRepository.findByAppIdInAndStatusAndActualTriggerTimeLessThan(partAppIds, InstanceStatus.WAITING_WORKER_RECEIVE.getV(), threshold);
        if (!CollectionUtils.isEmpty(waitingWorkerReceiveInstances)) {
            log.warn("[InstanceStatusChecker] find one instance didn't receive any reply from worker, try to redispatch: {}", waitingWorkerReceiveInstances);
            waitingWorkerReceiveInstances.forEach(instance -> {
                // 重新派发
                JobInfoDO jobInfoDO = jobInfoRepository.findById(instance.getJobId()).orElseGet(JobInfoDO::new);
                dispatchService.redispatch(jobInfoDO, instance.getInstanceId());
            });
        }
    }

    private void handleRunningInstance(List<Long> partAppIds) {
        // 3. 检查 RUNNING 状态的任务（一定时间没收到 TaskTracker 的状态报告，视为失败）
        long threshold = System.currentTimeMillis() - RUNNING_TIMEOUT_MS;
        List<InstanceInfoDO> failedInstances = instanceInfoRepository.findByAppIdInAndStatusAndGmtModifiedBefore(partAppIds, InstanceStatus.RUNNING.getV(), new Date(threshold));
        if (!CollectionUtils.isEmpty(failedInstances)) {
            log.warn("[InstanceStatusCheckService] instances({}) has not received status report for a long time.", failedInstances);
            failedInstances.forEach(instance -> {

                JobInfoDO jobInfoDO = jobInfoRepository.findById(instance.getJobId()).orElseGet(JobInfoDO::new);
                TimeExpressionType timeExpressionType = TimeExpressionType.of(jobInfoDO.getTimeExpressionType());
                SwitchableStatus switchableStatus = SwitchableStatus.of(jobInfoDO.getStatus());

                // 如果任务已关闭，则不进行重试，将任务置为失败即可；秒级任务也直接置为失败，由派发器重新调度
                if (switchableStatus != SwitchableStatus.ENABLE || TimeExpressionType.frequentTypes.contains(timeExpressionType.getV())) {
                    updateFailedInstance(instance, SystemInstanceResult.REPORT_TIMEOUT);
                    return;
                }

                // CRON 和 API一样，失败次数 + 1，根据重试配置进行重试
                if (instance.getRunningTimes() < jobInfoDO.getInstanceRetryNum()) {
                    dispatchService.redispatch(jobInfoDO, instance.getInstanceId());
                } else {
                    updateFailedInstance(instance, SystemInstanceResult.REPORT_TIMEOUT);
                }

            });
        }
    }

    /**
     * 定期检查工作流实例状态
     * 此处仅检查并重试长时间处于 WAITING 状态的工作流实例，工作流的其他可靠性由 Instance 支撑，即子任务失败会反馈会 WorkflowInstance
     *
     * @param allAppIds 本系统所承担的所有 appIds
     */
    private void checkWorkflowInstance(List<Long> allAppIds) {

        // 重试长时间处于 WAITING 状态的工作流实例
        long threshold = System.currentTimeMillis() - WORKFLOW_WAITING_TIMEOUT_MS;
        Lists.partition(allAppIds, MAX_BATCH_NUM).forEach(partAppIds -> {
            List<WorkflowInstanceInfoDO> waitingWfInstanceList = workflowInstanceInfoRepository.findByAppIdInAndStatusAndExpectedTriggerTimeLessThan(partAppIds, WorkflowInstanceStatus.WAITING.getV(), threshold);
            if (!CollectionUtils.isEmpty(waitingWfInstanceList)) {

                List<Long> wfInstanceIds = waitingWfInstanceList.stream().map(WorkflowInstanceInfoDO::getWfInstanceId).collect(Collectors.toList());
                log.warn("[WorkflowInstanceChecker] wfInstance({}) is not started as expected, oms try to restart these workflowInstance.", wfInstanceIds);

                waitingWfInstanceList.forEach(wfInstance -> {
                    Optional<WorkflowInfoDO> workflowOpt = workflowInfoRepository.findById(wfInstance.getWorkflowId());
                    workflowOpt.ifPresent(workflowInfo -> {
                        workflowInstanceManager.start(workflowInfo, wfInstance.getWfInstanceId());
                        log.info("[Workflow-{}|{}] restart workflowInstance successfully~", workflowInfo.getId(), wfInstance.getWfInstanceId());
                    });
                });
            }
        });
    }

    /**
     * 处理失败的任务实例
     */
    private void updateFailedInstance(InstanceInfoDO instance, String result) {

        log.warn("[InstanceStatusChecker] instance[{}] failed due to {}, instanceInfo: {}", instance.getInstanceId(), result, instance);

        instance.setStatus(InstanceStatus.FAILED.getV());
        instance.setFinishedTime(System.currentTimeMillis());
        instance.setGmtModified(new Date());
        instance.setResult(result);
        instanceInfoRepository.saveAndFlush(instance);

        instanceManager.processFinishedInstance(instance.getInstanceId(), instance.getWfInstanceId(), InstanceStatus.FAILED, result);
    }
}
