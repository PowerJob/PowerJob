package com.github.kfcfans.powerjob.server.service.timing;

import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.SystemInstanceResult;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.WorkflowInstanceStatus;
import com.github.kfcfans.powerjob.server.common.constans.SwitchableStatus;
import com.github.kfcfans.powerjob.server.akka.OhMyServer;
import com.github.kfcfans.powerjob.server.persistence.core.model.*;
import com.github.kfcfans.powerjob.server.persistence.core.repository.*;
import com.github.kfcfans.powerjob.server.service.DispatchService;
import com.github.kfcfans.powerjob.server.service.instance.InstanceManager;
import com.github.kfcfans.powerjob.server.service.workflow.WorkflowInstanceManager;
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
        List<AppInfoDO> appInfoList = appInfoRepository.findAllByCurrentServer(OhMyServer.getActorSystemAddress());
        if (CollectionUtils.isEmpty(appInfoList)) {
            log.info("[InstanceStatusChecker] current server has no app's job to check");
            return;
        }
        List<Long> allAppIds = appInfoList.stream().map(AppInfoDO::getId).collect(Collectors.toList());

        try {
            checkInstance(allAppIds);
            checkWorkflowInstance(allAppIds);
        }catch (Exception e) {
            log.error("[InstanceStatusChecker] status check failed.", e);
        }
        log.info("[InstanceStatusChecker] status check using {}.", stopwatch.stop());
    }

    /**
     * 检查任务实例的状态，发现异常及时重试，包括
     * WAITING_DISPATCH 超时：写入时间伦但为调度前 server down
     * WAITING_WORKER_RECEIVE 超时：由于网络错误导致 worker 未接受成功
     * RUNNING 超时：TaskTracker down，断开与 server 的心跳连接
     * @param allAppIds 本系统所承担的所有 appIds
     */
    private void checkInstance(List<Long> allAppIds) {

        Lists.partition(allAppIds, MAX_BATCH_NUM).forEach(partAppIds -> {

            // 1. 检查等待 WAITING_DISPATCH 状态的任务
            long threshold = System.currentTimeMillis() - DISPATCH_TIMEOUT_MS;
            List<InstanceInfoDO> waitingDispatchInstances = instanceInfoRepository.findByAppIdInAndStatusAndExpectedTriggerTimeLessThan(partAppIds, InstanceStatus.WAITING_DISPATCH.getV(), threshold);
            if (!CollectionUtils.isEmpty(waitingDispatchInstances)) {
                log.warn("[InstanceStatusChecker] instances({}) is not triggered as expected.", waitingDispatchInstances);
                waitingDispatchInstances.forEach(instance -> {

                    // 过滤因为失败重试而改成 WAITING_DISPATCH 状态的任务实例
                    long t = System.currentTimeMillis() - instance.getGmtModified().getTime();
                    if (t < DISPATCH_TIMEOUT_MS) {
                        return;
                    }

                    // 重新派发(orElseGet用于消除编译器警告...)
                    JobInfoDO jobInfoDO = jobInfoRepository.findById(instance.getJobId()).orElseGet(JobInfoDO::new);
                    dispatchService.redispatch(jobInfoDO, instance.getInstanceId(), 0);
                });
            }

            // 2. 检查 WAITING_WORKER_RECEIVE 状态的任务
            threshold = System.currentTimeMillis() - RECEIVE_TIMEOUT_MS;
            List<InstanceInfoDO> waitingWorkerReceiveInstances = instanceInfoRepository.findByAppIdInAndStatusAndActualTriggerTimeLessThan(partAppIds, InstanceStatus.WAITING_WORKER_RECEIVE.getV(), threshold);
            if (!CollectionUtils.isEmpty(waitingWorkerReceiveInstances)) {
                log.warn("[InstanceStatusChecker] find one instance didn't receive any reply from worker, try to redispatch: {}", waitingWorkerReceiveInstances);
                waitingWorkerReceiveInstances.forEach(instance -> {
                    // 重新派发
                    JobInfoDO jobInfoDO = jobInfoRepository.findById(instance.getJobId()).orElseGet(JobInfoDO::new);
                    dispatchService.redispatch(jobInfoDO, instance.getInstanceId(), 0);
                });
            }

            // 3. 检查 RUNNING 状态的任务（一定时间没收到 TaskTracker 的状态报告，视为失败）
            threshold = System.currentTimeMillis() - RUNNING_TIMEOUT_MS;
            List<InstanceInfoDO> failedInstances = instanceInfoRepository.findByAppIdInAndStatusAndGmtModifiedBefore(partAppIds, InstanceStatus.RUNNING.getV(), new Date(threshold));
            if (!CollectionUtils.isEmpty(failedInstances)) {
                log.warn("[InstanceStatusCheckService] instances({}) has not received status report for a long time.", failedInstances);
                failedInstances.forEach(instance -> {

                    JobInfoDO jobInfoDO = jobInfoRepository.findById(instance.getJobId()).orElseGet(JobInfoDO::new);
                    TimeExpressionType timeExpressionType = TimeExpressionType.of(jobInfoDO.getTimeExpressionType());
                    SwitchableStatus switchableStatus = SwitchableStatus.of(jobInfoDO.getStatus());

                    // 如果任务已关闭，则不进行重试，将任务置为失败即可；秒级任务也直接置为失败，由派发器重新调度
                    if (switchableStatus != SwitchableStatus.ENABLE || TimeExpressionType.frequentTypes.contains(timeExpressionType.getV())) {
                        updateFailedInstance(instance);
                        return;
                    }

                    // CRON 和 API一样，失败次数 + 1，根据重试配置进行重试
                    if (instance.getRunningTimes() < jobInfoDO.getInstanceRetryNum()) {
                        dispatchService.redispatch(jobInfoDO, instance.getInstanceId(), instance.getRunningTimes());
                    }else {
                        updateFailedInstance(instance);
                    }

                });
            }
        });
    }

    /**
     * 定期检查工作流实例状态
     * 此处仅检查并重试长时间处于 WAITING 状态的工作流实例，工作流的其他可靠性由 Instance 支撑，即子任务失败会反馈会 WorkflowInstance
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
                        workflowInstanceManager.start(workflowInfo, wfInstance.getWfInstanceId(), wfInstance.getWfInitParams());
                        log.info("[Workflow-{}|{}] restart workflowInstance successfully~", workflowInfo.getId(), wfInstance.getWfInstanceId());
                    });
                });
            }
        });
    }

    /**
     * 处理上报超时而失败的任务实例
     */
    private void updateFailedInstance(InstanceInfoDO instance) {

        log.warn("[InstanceStatusCheckService] detected instance(instanceId={},jobId={})'s TaskTracker report timeout,this instance is considered a failure.", instance.getInstanceId(), instance.getJobId());

        instance.setStatus(InstanceStatus.FAILED.getV());
        instance.setFinishedTime(System.currentTimeMillis());
        instance.setGmtModified(new Date());
        instance.setResult(SystemInstanceResult.REPORT_TIMEOUT);
        instanceInfoRepository.saveAndFlush(instance);

        instanceManager.processFinishedInstance(instance.getInstanceId(), instance.getWfInstanceId(), InstanceStatus.FAILED, SystemInstanceResult.REPORT_TIMEOUT);
    }
}
