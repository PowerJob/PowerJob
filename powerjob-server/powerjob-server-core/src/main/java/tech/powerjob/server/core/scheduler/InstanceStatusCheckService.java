package tech.powerjob.server.core.scheduler;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tech.powerjob.common.SystemInstanceResult;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.enums.WorkflowInstanceStatus;
import tech.powerjob.server.common.Holder;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.instance.InstanceManager;
import tech.powerjob.server.core.workflow.WorkflowInstanceManager;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowInstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.brief.BriefInstanceInfo;
import tech.powerjob.server.persistence.remote.repository.*;
import tech.powerjob.server.remote.transporter.TransportService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 定时状态检查
 *
 * @author tjq
 * @since 2020/4/7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceStatusCheckService {

    private static final int MAX_BATCH_NUM_APP = 10;
    private static final int MAX_BATCH_NUM_INSTANCE = 3000;
    private static final int MAX_BATCH_UPDATE_NUM = 500;
    private static final long DISPATCH_TIMEOUT_MS = 30000;
    private static final long RECEIVE_TIMEOUT_MS = 60000;
    private static final long RUNNING_TIMEOUT_MS = 60000;
    private static final long WORKFLOW_WAITING_TIMEOUT_MS = 60000;

    public static final long CHECK_INTERVAL = 10000;

    private final TransportService transportService;

    private final DispatchService dispatchService;

    private final InstanceManager instanceManager;

    private final WorkflowInstanceManager workflowInstanceManager;

    private final AppInfoRepository appInfoRepository;

    private final JobInfoRepository jobInfoRepository;

    private final InstanceInfoRepository instanceInfoRepository;

    private final WorkflowInfoRepository workflowInfoRepository;

    private final WorkflowInstanceInfoRepository workflowInstanceInfoRepository;

    public void checkWorkflowInstance() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // 查询 DB 获取该 Server 需要负责的 AppGroup
        List<Long> allAppIds = appInfoRepository.listAppIdByCurrentServer(transportService.defaultProtocol().getAddress());
        if (CollectionUtils.isEmpty(allAppIds)) {
            log.info("[InstanceStatusChecker] current server has no app's job to check");
            return;
        }
        try {
            checkWorkflowInstance(allAppIds);
        } catch (Exception e) {
            log.error("[InstanceStatusChecker] WorkflowInstance status check failed.", e);
        }
        log.info("[InstanceStatusChecker] WorkflowInstance status check using {}.", stopwatch.stop());
    }

    /**
     * 检查等待派发的实例
     * WAITING_DISPATCH 超时：写入时间轮但未调度前 server down
     */
    public void checkWaitingDispatchInstance() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // 查询 DB 获取该 Server 需要负责的 AppGroup
        List<Long> allAppIds = appInfoRepository.listAppIdByCurrentServer(transportService.defaultProtocol().getAddress());
        if (CollectionUtils.isEmpty(allAppIds)) {
            log.info("[InstanceStatusChecker] current server has no app's job to check");
            return;
        }
        try {
            // 检查等待 WAITING_DISPATCH 状态的任务
            Lists.partition(allAppIds, MAX_BATCH_NUM_APP).forEach(this::handleWaitingDispatchInstance);
        } catch (Exception e) {
            log.error("[InstanceStatusChecker] WaitingDispatchInstance status check failed.", e);
        }
        log.info("[InstanceStatusChecker] WaitingDispatchInstance status check using {}.", stopwatch.stop());
    }

    /**
     * 检查等待 worker 接收的实例
     * WAITING_WORKER_RECEIVE 超时：由于网络错误导致 worker 未接受成功
     */
    public void checkWaitingWorkerReceiveInstance() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // 查询 DB 获取该 Server 需要负责的 AppGroup
        List<Long> allAppIds = appInfoRepository.listAppIdByCurrentServer(transportService.defaultProtocol().getAddress());
        if (CollectionUtils.isEmpty(allAppIds)) {
            log.info("[InstanceStatusChecker] current server has no app's job to check");
            return;
        }
        try {
            // 检查 WAITING_WORKER_RECEIVE 状态的任务
            Lists.partition(allAppIds, MAX_BATCH_NUM_APP).forEach(this::handleWaitingWorkerReceiveInstance);
        } catch (Exception e) {
            log.error("[InstanceStatusChecker] WaitingWorkerReceiveInstance status check failed.", e);
        }
        log.info("[InstanceStatusChecker] WaitingWorkerReceiveInstance status check using {}.", stopwatch.stop());
    }

    /**
     * 检查运行中的实例
     * RUNNING 超时：TaskTracker down，断开与 server 的心跳连接
     */
    public void checkRunningInstance() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // 查询 DB 获取该 Server 需要负责的 AppGroup
        List<Long> allAppIds = appInfoRepository.listAppIdByCurrentServer(transportService.defaultProtocol().getAddress());
        if (CollectionUtils.isEmpty(allAppIds)) {
            log.info("[InstanceStatusChecker] current server has no app's job to check");
            return;
        }
        try {
            // 检查 RUNNING 状态的任务（一定时间没收到 TaskTracker 的状态报告，视为失败）
            Lists.partition(allAppIds, MAX_BATCH_NUM_APP).forEach(this::handleRunningInstance);
        } catch (Exception e) {
            log.error("[InstanceStatusChecker] RunningInstance status check failed.", e);
        }
        log.info("[InstanceStatusChecker] RunningInstance status check using {}.", stopwatch.stop());
    }

    private void handleWaitingDispatchInstance(List<Long> partAppIds) {
        // 1. 检查等待 WAITING_DISPATCH 状态的任务
        long threshold = System.currentTimeMillis() - DISPATCH_TIMEOUT_MS;
        List<InstanceInfoDO> waitingDispatchInstances = instanceInfoRepository.findAllByAppIdInAndStatusAndExpectedTriggerTimeLessThan(partAppIds, InstanceStatus.WAITING_DISPATCH.getV(), threshold, PageRequest.of(0, MAX_BATCH_NUM_INSTANCE));
        while (!waitingDispatchInstances.isEmpty()) {
            List<Long> overloadAppIdList = new ArrayList<>();
            long startTime = System.currentTimeMillis();
            // 按照 appId 分组处理，方便处理超载的逻辑
            Map<Long, List<InstanceInfoDO>> waitingDispatchInstancesMap = waitingDispatchInstances.stream().collect(Collectors.groupingBy(InstanceInfoDO::getAppId));
            for (Map.Entry<Long, List<InstanceInfoDO>> entry : waitingDispatchInstancesMap.entrySet()) {
                final Long currentAppId = entry.getKey();
                final List<InstanceInfoDO> currentAppWaitingDispatchInstances = entry.getValue();
                // collect job id
                Set<Long> jobIds = currentAppWaitingDispatchInstances.stream().map(InstanceInfoDO::getJobId).collect(Collectors.toSet());
                // query job info and map
                Map<Long, JobInfoDO> jobInfoMap = jobInfoRepository.findByIdIn(jobIds).stream().collect(Collectors.toMap(JobInfoDO::getId, e -> e));
                log.warn("[InstanceStatusChecker] find some instance in app({}) which is not triggered as expected: {}", currentAppId, currentAppWaitingDispatchInstances.stream().map(InstanceInfoDO::getInstanceId).collect(Collectors.toList()));
                final Holder<Boolean> overloadFlag = new Holder<>(false);
                // 先这么简单处理没问题，毕竟只有这一个地方用了 parallelStream
                currentAppWaitingDispatchInstances.parallelStream().forEach(instance -> {
                    if (overloadFlag.get()) {
                        // 直接忽略
                        return;
                    }
                    Optional<JobInfoDO> jobInfoOpt = Optional.ofNullable(jobInfoMap.get(instance.getJobId()));
                    if (jobInfoOpt.isPresent()) {
                        // 处理等待派发的任务没有必要再重置一次状态，减少 io 次数
                        dispatchService.dispatch(jobInfoOpt.get(), instance.getInstanceId(), Optional.of(instance), Optional.of(overloadFlag));
                    } else {
                        log.warn("[InstanceStatusChecker] can't find job by jobId[{}], so redispatch failed, failed instance: {}", instance.getJobId(), instance);
                        final Optional<InstanceInfoDO> opt = instanceInfoRepository.findById(instance.getId());
                        opt.ifPresent(instanceInfoDO -> updateFailedInstance(instanceInfoDO, SystemInstanceResult.CAN_NOT_FIND_JOB_INFO));
                    }
                });
                threshold = System.currentTimeMillis() - DISPATCH_TIMEOUT_MS;
                if (overloadFlag.get()) {
                    overloadAppIdList.add(currentAppId);
                }
            }
            log.info("[InstanceStatusChecker] process {} task,use {} ms", waitingDispatchInstances.size(), System.currentTimeMillis() - startTime);
            if (!overloadAppIdList.isEmpty()) {
                log.warn("[InstanceStatusChecker] app[{}] is overload, so skip check waiting dispatch instance", overloadAppIdList);
                partAppIds.removeAll(overloadAppIdList);
            }
            if (partAppIds.isEmpty()) {
                break;
            }
            waitingDispatchInstances = instanceInfoRepository.findAllByAppIdInAndStatusAndExpectedTriggerTimeLessThan(partAppIds, InstanceStatus.WAITING_DISPATCH.getV(), threshold, PageRequest.of(0, MAX_BATCH_NUM_INSTANCE));
        }

    }

    private void handleWaitingWorkerReceiveInstance(List<Long> partAppIds) {
        // 2. 检查 WAITING_WORKER_RECEIVE 状态的任务
        long threshold = System.currentTimeMillis() - RECEIVE_TIMEOUT_MS;
        List<BriefInstanceInfo> waitingWorkerReceiveInstances = instanceInfoRepository.selectBriefInfoByAppIdInAndStatusAndActualTriggerTimeLessThan(partAppIds, InstanceStatus.WAITING_WORKER_RECEIVE.getV(), threshold, PageRequest.of(0, MAX_BATCH_NUM_INSTANCE));
        while (!waitingWorkerReceiveInstances.isEmpty()) {
            log.warn("[InstanceStatusChecker] find some instance didn't receive any reply from worker, try to redispatch: {}", waitingWorkerReceiveInstances.stream().map(BriefInstanceInfo::getInstanceId).collect(Collectors.toList()));
            final List<List<BriefInstanceInfo>> partitions = Lists.partition(waitingWorkerReceiveInstances, MAX_BATCH_UPDATE_NUM);
            for (List<BriefInstanceInfo> partition : partitions) {
                dispatchService.redispatchBatchAsyncLockFree(partition.stream().map(BriefInstanceInfo::getInstanceId).collect(Collectors.toList()), InstanceStatus.WAITING_WORKER_RECEIVE.getV());
            }
            // 重新查询
            threshold = System.currentTimeMillis() - RECEIVE_TIMEOUT_MS;
            waitingWorkerReceiveInstances = instanceInfoRepository.selectBriefInfoByAppIdInAndStatusAndActualTriggerTimeLessThan(partAppIds, InstanceStatus.WAITING_WORKER_RECEIVE.getV(), threshold, PageRequest.of(0, MAX_BATCH_NUM_INSTANCE));
        }
    }

    private void handleRunningInstance(List<Long> partAppIds) {
        // 3. 检查 RUNNING 状态的任务（一定时间没收到 TaskTracker 的状态报告，视为失败）
        long threshold = System.currentTimeMillis() - RUNNING_TIMEOUT_MS;
        List<BriefInstanceInfo> failedInstances = instanceInfoRepository.selectBriefInfoByAppIdInAndStatusAndGmtModifiedBefore(partAppIds, InstanceStatus.RUNNING.getV(), new Date(threshold), PageRequest.of(0, MAX_BATCH_NUM_INSTANCE));
        while (!failedInstances.isEmpty()) {
            // collect job id
            Set<Long> jobIds = failedInstances.stream().map(BriefInstanceInfo::getJobId).collect(Collectors.toSet());
            // query job info and map
            Map<Long, JobInfoDO> jobInfoMap = jobInfoRepository.findByIdIn(jobIds).stream().collect(Collectors.toMap(JobInfoDO::getId, e -> e));
            log.warn("[InstanceStatusCheckService] find some instances have not received status report for a long time : {}", failedInstances.stream().map(BriefInstanceInfo::getInstanceId).collect(Collectors.toList()));
            failedInstances.forEach(instance -> {
                Optional<JobInfoDO> jobInfoOpt = Optional.ofNullable(jobInfoMap.get(instance.getJobId()));
                if (!jobInfoOpt.isPresent()) {
                    final Optional<InstanceInfoDO> opt = instanceInfoRepository.findById(instance.getId());
                    opt.ifPresent(e -> updateFailedInstance(e, SystemInstanceResult.REPORT_TIMEOUT));
                    return;
                }
                TimeExpressionType timeExpressionType = TimeExpressionType.of(jobInfoOpt.get().getTimeExpressionType());
                SwitchableStatus switchableStatus = SwitchableStatus.of(jobInfoOpt.get().getStatus());
                // 如果任务已关闭，则不进行重试，将任务置为失败即可；秒级任务也直接置为失败，由派发器重新调度
                if (switchableStatus != SwitchableStatus.ENABLE || TimeExpressionType.FREQUENT_TYPES.contains(timeExpressionType.getV())) {
                    final Optional<InstanceInfoDO> opt = instanceInfoRepository.findById(instance.getId());
                    opt.ifPresent(e -> updateFailedInstance(e, SystemInstanceResult.REPORT_TIMEOUT));
                    return;
                }
                // CRON 和 API一样，失败次数 + 1，根据重试配置进行重试
                if (instance.getRunningTimes() < jobInfoOpt.get().getInstanceRetryNum()) {
                    dispatchService.redispatchAsync(instance.getInstanceId(), InstanceStatus.RUNNING.getV());
                } else {
                    final Optional<InstanceInfoDO> opt = instanceInfoRepository.findById(instance.getId());
                    opt.ifPresent(e -> updateFailedInstance(e, SystemInstanceResult.REPORT_TIMEOUT));
                }
            });
            threshold = System.currentTimeMillis() - RUNNING_TIMEOUT_MS;
            failedInstances = instanceInfoRepository.selectBriefInfoByAppIdInAndStatusAndGmtModifiedBefore(partAppIds, InstanceStatus.RUNNING.getV(), new Date(threshold), PageRequest.of(0, MAX_BATCH_NUM_INSTANCE));
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
        Lists.partition(allAppIds, MAX_BATCH_NUM_APP).forEach(partAppIds -> {
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
