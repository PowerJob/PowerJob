package com.github.kfcfans.oms.server.service.instance;

import com.github.kfcfans.oms.common.InstanceStatus;
import com.github.kfcfans.oms.common.request.TaskTrackerReportInstanceStatusReq;
import com.github.kfcfans.oms.common.TimeExpressionType;
import com.github.kfcfans.oms.server.common.utils.SpringUtils;
import com.github.kfcfans.oms.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.UserInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.UserInfoRepository;
import com.github.kfcfans.oms.server.service.DispatchService;
import com.github.kfcfans.oms.server.service.InstanceLogService;
import com.github.kfcfans.oms.server.service.alarm.AlarmContent;
import com.github.kfcfans.oms.server.service.alarm.Alarmable;
import com.github.kfcfans.oms.server.service.timing.schedule.HashedWheelTimerHolder;
import com.github.kfcfans.oms.server.service.workflow.WorkflowInstanceManager;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 管理被调度的服务
 *
 * @author tjq
 * @since 2020/4/7
 */
@Slf4j
public class InstanceManager {

    // 存储 instanceId 对应的 Job 信息，便于重试
    private static Map<Long, JobInfoDO> instanceId2JobInfo = Maps.newConcurrentMap();
    // 存储 instance 的状态（暂时只用到了 lastReportTime）
    private static Map<Long, InstanceStatusHolder> instanceId2StatusHolder = Maps.newConcurrentMap();

    // Spring Bean
    private static DispatchService dispatchService;
    private static InstanceLogService instanceLogService;
    private static InstanceInfoRepository instanceInfoRepository;
    private static JobInfoRepository jobInfoRepository;
    private static Alarmable omsCenterAlarmService;
    private static WorkflowInstanceManager workflowInstanceManager;

    /**
     * 注册到任务实例管理器
     * @param instanceId 即将运行的任务实例ID
     * @param jobInfoDO 即将运行的任务实例对应的任务元数据
     */
    public static void register(Long instanceId, JobInfoDO jobInfoDO) {

        InstanceStatusHolder statusHolder = new InstanceStatusHolder();
        statusHolder.setInstanceId(instanceId);
        statusHolder.setInstanceStatus(InstanceStatus.WAITING_DISPATCH.getV());

        instanceId2JobInfo.put(instanceId, jobInfoDO);
        instanceId2StatusHolder.put(instanceId, statusHolder);
    }

    /**
     * 更新任务状态
     * @param req TaskTracker上报任务实例状态的请求
     */
    public static void updateStatus(TaskTrackerReportInstanceStatusReq req) {

        Long jobId = req.getJobId();
        Long instanceId = req.getInstanceId();

        // 不存在，可能该任务实例刚经历Server变更，需要重新构建基础信息
        if (!instanceId2JobInfo.containsKey(instanceId)) {
            log.warn("[InstanceManager] can't find any register info for instance(jobId={},instanceId={}), maybe change the server.", jobId, instanceId);

            Optional<JobInfoDO> jobInfoDOOptional = getJobInfoRepository().findById(jobId);
            if (jobInfoDOOptional.isPresent()) {
                JobInfoDO JobInfoDo = jobInfoDOOptional.get();
                instanceId2JobInfo.put(instanceId, JobInfoDo);
            }else {
                throw new IllegalArgumentException("can't find JobIno by jobId:" + jobId);
            }
        }

        // 更新本地保存的任务实例状态（用于未完成任务前的详细信息查询和缓存加速）
        InstanceStatusHolder statusHolder = instanceId2StatusHolder.computeIfAbsent(instanceId, ignore -> new InstanceStatusHolder());
        if (req.getReportTime() > statusHolder.getLastReportTime()) {
            BeanUtils.copyProperties(req, statusHolder);
            statusHolder.setLastReportTime(req.getReportTime());
        }else {
            log.warn("[InstanceManager] receive the expired status report request: {}.", req);
            return;
        }

        InstanceStatus newStatus = InstanceStatus.of(req.getInstanceStatus());
        Integer timeExpressionType = instanceId2JobInfo.get(instanceId).getTimeExpressionType();

        // FREQUENT 任务没有失败重试机制，TaskTracker一直运行即可，只需要将存活信息同步到DB即可
        // FREQUENT 任务的 newStatus 只有2中情况，一种是 RUNNING，一种是 FAILED（表示该机器 overload，需要重新选一台机器执行）
        // 综上，直接把 status 和 runningNum 同步到DB即可
        if (TimeExpressionType.frequentTypes.contains(timeExpressionType)) {
            getInstanceInfoRepository().update4FrequentJob(instanceId, newStatus.getV(), req.getTotalTaskNum());
            return;
        }

        InstanceInfoDO updateEntity = getInstanceInfoRepository().findByInstanceId(instanceId);
        updateEntity.setStatus(newStatus.getV());
        updateEntity.setGmtModified(new Date());

        boolean finished = false;
        if (newStatus == InstanceStatus.SUCCEED) {
            updateEntity.setResult(req.getResult());
            updateEntity.setFinishedTime(System.currentTimeMillis());

            finished = true;
        }else if (newStatus == InstanceStatus.FAILED) {

            // 当前重试次数 <= 最大重试次数，进行重试 （第一次运行，runningTimes为1，重试一次，instanceRetryNum也为1，故需要 =）
            if (updateEntity.getRunningTimes() <= instanceId2JobInfo.get(instanceId).getInstanceRetryNum()) {

                log.info("[InstanceManager] instance(instanceId={}) execute failed but will take the {}th retry.", instanceId, updateEntity.getRunningTimes());

                // 延迟10S重试（由于重试不改变 instanceId，如果派发到同一台机器，上一个 TaskTracker 还处于资源释放阶段，无法创建新的TaskTracker，任务失败）
                HashedWheelTimerHolder.TIMER.schedule(() -> {
                    getDispatchService().redispatch(instanceId2JobInfo.get(instanceId), instanceId, updateEntity.getRunningTimes());
                }, 10, TimeUnit.SECONDS);

                // 修改状态为 等待派发，正式开始重试
                // 问题：会丢失以往的调度记录（actualTriggerTime什么的都会被覆盖）
                updateEntity.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
            }else {
                updateEntity.setResult(req.getResult());
                updateEntity.setFinishedTime(System.currentTimeMillis());
                finished = true;
                log.info("[InstanceManager] instance(instanceId={}) execute failed and have no chance to retry.", instanceId);
            }
        }

        // 同步状态变更信息到数据库
        getInstanceInfoRepository().saveAndFlush(updateEntity);

        if (finished) {
            // 这里的 InstanceStatus 只有 成功/失败 两种，手动停止不会由 TaskTracker 上报
            processFinishedInstance(instanceId, req.getWfInstanceId(), newStatus, req.getResult());
        }
    }

    /**
     * 收尾完成的任务实例
     * @param instanceId 任务实例ID
     * @param wfInstanceId 工作流实例ID，非必须
     * @param status 任务状态，有 成功/失败/手动停止
     * @param result 执行结果
     */
    public static void processFinishedInstance(Long instanceId, Long wfInstanceId, InstanceStatus status, String result) {

        log.info("[Instance-{}] process finished, final status is {}.", instanceId, status.name());

        // 清除已完成的实例信息
        instanceId2StatusHolder.remove(instanceId);
        // 这一步也可能导致后面取不到 JobInfoDO
        JobInfoDO jobInfo = instanceId2JobInfo.remove(instanceId);

        // 上报日志数据
        getInstanceLogService().sync(instanceId);

        // workflow 特殊处理
        if (wfInstanceId != null) {
            // 手动停止在工作流中也认为是失败（理论上不应该发生）
            getWorkflowInstanceManager().move(wfInstanceId, instanceId, status == InstanceStatus.SUCCEED, result);
        }

        // 告警
        if (status == InstanceStatus.FAILED) {

            if (jobInfo == null) {
                jobInfo = fetchJobInfo(instanceId);
            }
            if (jobInfo == null) {
                log.warn("[InstanceManager] can't find jobInfo by instanceId({}), alarm failed.", instanceId);
                return;
            }

            InstanceInfoDO instanceInfo = getInstanceInfoRepository().findByInstanceId(instanceId);
            AlarmContent content = new AlarmContent();
            BeanUtils.copyProperties(jobInfo, content);
            BeanUtils.copyProperties(instanceInfo, content);

            List<Long> userIds = jobInfo.fetchNotifyUserIds();
            List<UserInfoDO> userList = SpringUtils.getBean(UserInfoRepository.class).findByIdIn(userIds);
            userList.forEach(x -> x.setPassword(null));

            getAlarmService().alarm(content, userList);
        }
    }

    public static JobInfoDO fetchJobInfo(Long instanceId) {
        JobInfoDO jobInfo = instanceId2JobInfo.get(instanceId);
        if (jobInfo != null) {
            return jobInfo;
        }
        InstanceInfoDO instanceInfo = getInstanceInfoRepository().findByInstanceId(instanceId);
        if (instanceInfo != null) {
            return getJobInfoRepository().findById(instanceInfo.getJobId()).orElse(null);
        }
        return null;
    }

    /**
     * 释放本地缓存，防止内存泄漏
     */
    public static void releaseInstanceInfos() {
        instanceId2JobInfo = Maps.newConcurrentMap();
        instanceId2StatusHolder = Maps.newConcurrentMap();
    }

    private static InstanceInfoRepository getInstanceInfoRepository() {
        while (instanceInfoRepository == null) {
            try {
                Thread.sleep(100);
            }catch (Exception ignore) {
            }
            instanceInfoRepository = SpringUtils.getBean(InstanceInfoRepository.class);
        }
        return instanceInfoRepository;
    }

    private static JobInfoRepository getJobInfoRepository() {
        while (jobInfoRepository == null) {
            try {
                Thread.sleep(100);
            }catch (Exception ignore) {
            }
            jobInfoRepository = SpringUtils.getBean(JobInfoRepository.class);
        }
        return jobInfoRepository;
    }

    private static DispatchService getDispatchService() {
        while (dispatchService == null) {
            try {
                Thread.sleep(100);
            }catch (Exception ignore) {
            }
            dispatchService = SpringUtils.getBean(DispatchService.class);
        }
        return dispatchService;
    }

    private static InstanceLogService getInstanceLogService() {
        while (instanceLogService == null) {
            try {
                Thread.sleep(100);
            }catch (Exception ignore) {
            }
            instanceLogService = SpringUtils.getBean(InstanceLogService.class);
        }
        return instanceLogService;
    }

    private static Alarmable getAlarmService() {
        while (omsCenterAlarmService == null) {
            try {
                Thread.sleep(100);
            }catch (Exception ignore) {
            }
            omsCenterAlarmService = (Alarmable) SpringUtils.getBean("omsCenterAlarmService");
        }
        return omsCenterAlarmService;
    }

    private static WorkflowInstanceManager getWorkflowInstanceManager() {
        while (workflowInstanceManager == null) {
            try {
                Thread.sleep(100);
            }catch (Exception ignore) {
            }
            workflowInstanceManager = SpringUtils.getBean(WorkflowInstanceManager.class);
        }
        return workflowInstanceManager;
    }
}
