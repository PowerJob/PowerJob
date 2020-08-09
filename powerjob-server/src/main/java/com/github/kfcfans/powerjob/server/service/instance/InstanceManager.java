package com.github.kfcfans.powerjob.server.service.instance;

import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import com.github.kfcfans.powerjob.server.common.utils.SpringUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.UserInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.powerjob.server.service.DispatchService;
import com.github.kfcfans.powerjob.server.service.InstanceLogService;
import com.github.kfcfans.powerjob.server.service.UserService;
import com.github.kfcfans.powerjob.server.service.alarm.AlarmCenter;
import com.github.kfcfans.powerjob.server.service.alarm.JobInstanceAlarm;
import com.github.kfcfans.powerjob.server.service.timing.schedule.HashedWheelTimerHolder;
import com.github.kfcfans.powerjob.server.service.workflow.WorkflowInstanceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 管理被调度的服务
 *
 * @author tjq
 * @since 2020/4/7
 */
@Slf4j
@Service
public class InstanceManager {

    @Resource
    private DispatchService dispatchService;
    @Resource
    private InstanceLogService instanceLogService;
    @Resource
    private InstanceMetadataService instanceMetadataService;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;
    @Resource
    private WorkflowInstanceManager workflowInstanceManager;


    /**
     * 更新任务状态
     * @param req TaskTracker上报任务实例状态的请求
     */
    public void updateStatus(TaskTrackerReportInstanceStatusReq req) throws Exception {

        Long instanceId = req.getInstanceId();

        // 获取相关数据
        JobInfoDO jobInfo = instanceMetadataService.fetchJobInfoByInstanceId(req.getInstanceId());
        InstanceInfoDO instanceInfo = instanceInfoRepository.findByInstanceId(instanceId);
        if (instanceInfo == null) {
            log.warn("[InstanceManager-{}] can't find InstanceInfo from database", instanceId);
            return;
        }
        // 丢弃过期的上报数据
        if (req.getReportTime() <= instanceInfo.getLastReportTime()) {
            log.warn("[InstanceManager-{}] receive the expired status report request: {}, this report will be dropped.", instanceId, req);
            return;
        }

        // 丢弃非目标 TaskTracker 的上报数据（脑裂情况）
        if (!req.getSourceAddress().equals(instanceInfo.getTaskTrackerAddress())) {
            log.warn("[InstanceManager-{}] receive the other TaskTracker's report: {}, but current TaskTracker is {}, this report will br dropped.", instanceId, req, instanceInfo.getTaskTrackerAddress());
            return;
        }

        InstanceStatus newStatus = InstanceStatus.of(req.getInstanceStatus());
        Integer timeExpressionType = jobInfo.getTimeExpressionType();

        instanceInfo.setStatus(newStatus.getV());
        instanceInfo.setLastReportTime(req.getReportTime());
        instanceInfo.setGmtModified(new Date());

        // FREQUENT 任务没有失败重试机制，TaskTracker一直运行即可，只需要将存活信息同步到DB即可
        // FREQUENT 任务的 newStatus 只有2中情况，一种是 RUNNING，一种是 FAILED（表示该机器 overload，需要重新选一台机器执行）
        // 综上，直接把 status 和 runningNum 同步到DB即可
        if (TimeExpressionType.frequentTypes.contains(timeExpressionType)) {

            instanceInfo.setResult(req.getResult());
            instanceInfo.setRunningTimes(req.getTotalTaskNum());
            instanceInfoRepository.saveAndFlush(instanceInfo);
            return;
        }

        boolean finished = false;
        if (newStatus == InstanceStatus.SUCCEED) {
            instanceInfo.setResult(req.getResult());
            instanceInfo.setFinishedTime(System.currentTimeMillis());

            finished = true;
        }else if (newStatus == InstanceStatus.FAILED) {

            // 当前重试次数 <= 最大重试次数，进行重试 （第一次运行，runningTimes为1，重试一次，instanceRetryNum也为1，故需要 =）
            if (instanceInfo.getRunningTimes() <= jobInfo.getInstanceRetryNum()) {

                log.info("[InstanceManager-{}] instance execute failed but will take the {}th retry.", instanceId, instanceInfo.getRunningTimes());

                // 延迟10S重试（由于重试不改变 instanceId，如果派发到同一台机器，上一个 TaskTracker 还处于资源释放阶段，无法创建新的TaskTracker，任务失败）
                HashedWheelTimerHolder.INACCURATE_TIMER.schedule(() -> {
                    dispatchService.redispatch(jobInfo, instanceId, instanceInfo.getRunningTimes());
                }, 10, TimeUnit.SECONDS);

                // 修改状态为 等待派发，正式开始重试
                // 问题：会丢失以往的调度记录（actualTriggerTime什么的都会被覆盖）
                instanceInfo.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
            }else {
                instanceInfo.setResult(req.getResult());
                instanceInfo.setFinishedTime(System.currentTimeMillis());
                finished = true;
                log.info("[InstanceManager-{}] instance execute failed and have no chance to retry.", instanceId);
            }
        }

        // 同步状态变更信息到数据库
        instanceInfoRepository.saveAndFlush(instanceInfo);

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
    public void processFinishedInstance(Long instanceId, Long wfInstanceId, InstanceStatus status, String result) {

        log.info("[Instance-{}] process finished, final status is {}.", instanceId, status.name());

        // 上报日志数据
        HashedWheelTimerHolder.INACCURATE_TIMER.schedule(() -> instanceLogService.sync(instanceId), 15, TimeUnit.SECONDS);

        // workflow 特殊处理
        if (wfInstanceId != null) {
            // 手动停止在工作流中也认为是失败（理论上不应该发生）
            workflowInstanceManager.move(wfInstanceId, instanceId, status, result);
        }

        // 告警
        if (status == InstanceStatus.FAILED) {
            JobInfoDO jobInfo;
            try {
                jobInfo = instanceMetadataService.fetchJobInfoByInstanceId(instanceId);
            }catch (Exception e) {
                log.warn("[InstanceManager-{}] can't find jobInfo, alarm failed.", instanceId);
                return;
            }

            InstanceInfoDO instanceInfo = instanceInfoRepository.findByInstanceId(instanceId);
            JobInstanceAlarm content = new JobInstanceAlarm();
            BeanUtils.copyProperties(jobInfo, content);
            BeanUtils.copyProperties(instanceInfo, content);

            List<UserInfoDO> userList = SpringUtils.getBean(UserService.class).fetchNotifyUserList(jobInfo.getNotifyUserIds());
            AlarmCenter.alarmFailed(content, userList);
        }

        // 主动移除缓存，减小内存占用
        instanceMetadataService.invalidateJobInfo(instanceId);
    }

}
