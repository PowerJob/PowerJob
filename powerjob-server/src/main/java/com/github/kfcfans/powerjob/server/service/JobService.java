package com.github.kfcfans.powerjob.server.service;

import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.request.http.SaveJobInfoRequest;
import com.github.kfcfans.powerjob.common.response.JobInfoDTO;
import com.github.kfcfans.powerjob.server.common.SJ;
import com.github.kfcfans.powerjob.server.common.constans.SwitchableStatus;
import com.github.kfcfans.powerjob.server.common.utils.CronExpression;
import com.github.kfcfans.powerjob.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.powerjob.server.service.instance.InstanceService;
import com.github.kfcfans.powerjob.server.service.instance.InstanceTimeWheelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 任务服务
 *
 * @author tjq
 * @since 2020/4/15
 */
@Slf4j
@Service
public class JobService {

    @Resource
    private InstanceService instanceService;

    @Resource
    private DispatchService dispatchService;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    /**
     * 保存/修改任务
     * @param request 任务请求
     * @return 创建的任务ID（jobId）
     * @throws Exception 异常
     */
    public Long saveJob(SaveJobInfoRequest request) throws Exception {

        request.valid();

        JobInfoDO jobInfoDO;
        if (request.getId() != null) {
            jobInfoDO = jobInfoRepository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("can't find job by jobId: " + request.getId()));
        }else {
            jobInfoDO = new JobInfoDO();
        }

        // 值拷贝
        BeanUtils.copyProperties(request, jobInfoDO);

        // 拷贝枚举值
        jobInfoDO.setExecuteType(request.getExecuteType().getV());
        jobInfoDO.setProcessorType(request.getProcessorType().getV());
        jobInfoDO.setTimeExpressionType(request.getTimeExpressionType().getV());
        jobInfoDO.setStatus(request.isEnable() ? SwitchableStatus.ENABLE.getV() : SwitchableStatus.DISABLE.getV());

        if (jobInfoDO.getMaxWorkerCount() == null) {
            jobInfoDO.setMaxWorkerCount(0);
        }

        // 转化报警用户列表
        if (!CollectionUtils.isEmpty(request.getNotifyUserIds())) {
            jobInfoDO.setNotifyUserIds(SJ.commaJoiner.join(request.getNotifyUserIds()));
        }

        refreshJob(jobInfoDO);
        if (request.getId() == null) {
            jobInfoDO.setGmtCreate(new Date());
        }
        JobInfoDO res = jobInfoRepository.saveAndFlush(jobInfoDO);
        return res.getId();
    }

    public JobInfoDTO fetchJob(Long jobId) {
        JobInfoDO jobInfoDO = jobInfoRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("can't find job by jobId: " + jobId));
        JobInfoDTO jobInfoDTO = new JobInfoDTO();
        BeanUtils.copyProperties(jobInfoDO, jobInfoDTO);
        return jobInfoDTO;
    }

    /**
     * 手动立即运行某个任务
     * @param jobId 任务ID
     * @param instanceParams 任务实例参数（仅 OpenAPI 存在）
     * @param delay 延迟时间，单位 毫秒
     * @return 任务实例ID
     */
    public long runJob(Long jobId, String instanceParams, long delay) {

        log.info("[Job-{}] try to run job, instanceParams={},delay={} ms.", jobId, instanceParams, delay);

        JobInfoDO jobInfo = jobInfoRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("can't find job by id:" + jobId));
        Long instanceId = instanceService.create(jobInfo.getId(), jobInfo.getAppId(), instanceParams, null, System.currentTimeMillis() + Math.max(delay, 0));
        instanceInfoRepository.flush();

        if (delay <= 0) {
            dispatchService.dispatch(jobInfo, instanceId, 0, instanceParams, null);
        }else {
            InstanceTimeWheelService.schedule(instanceId, delay, () -> {
                dispatchService.dispatch(jobInfo, instanceId, 0, instanceParams, null);
            });
        }
        log.info("[Job-{}] run job successfully, instanceId={}", jobId, instanceId);
        return instanceId;
    }

    /**
     * 删除某个任务
     * @param jobId 任务ID
     */
    public void deleteJob(Long jobId) {
        shutdownOrStopJob(jobId, SwitchableStatus.DELETED);
    }

    /**
     * 禁用某个任务
     */
    public void disableJob(Long jobId) {
        shutdownOrStopJob(jobId, SwitchableStatus.DISABLE);
    }

    /**
     * 启用某个任务
     * @param jobId 任务ID
     * @throws Exception 异常（CRON表达式错误）
     */
    public void enableJob(Long jobId) throws Exception {
        JobInfoDO jobInfoDO = jobInfoRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("can't find job by jobId:" + jobId));

        jobInfoDO.setStatus(SwitchableStatus.ENABLE.getV());
        refreshJob(jobInfoDO);

        jobInfoRepository.saveAndFlush(jobInfoDO);
    }

    /**
     * 停止或删除某个JOB
     * 秒级任务还要额外停止正在运行的任务实例
     */
    private void shutdownOrStopJob(Long jobId, SwitchableStatus status) throws IllegalArgumentException {

        // 1. 先更新 job_info 表
        Optional<JobInfoDO> jobInfoOPT = jobInfoRepository.findById(jobId);
        if (!jobInfoOPT.isPresent()) {
            throw new IllegalArgumentException("can't find job by jobId:" + jobId);
        }
        JobInfoDO jobInfoDO = jobInfoOPT.get();
        jobInfoDO.setStatus(status.getV());
        jobInfoDO.setGmtModified(new Date());
        jobInfoRepository.saveAndFlush(jobInfoDO);

        // 2. 关闭秒级任务
        if (!TimeExpressionType.frequentTypes.contains(jobInfoDO.getTimeExpressionType())) {
            return;
        }
        List<InstanceInfoDO> executeLogs = instanceInfoRepository.findByJobIdAndStatusIn(jobId, InstanceStatus.generalizedRunningStatus);
        if (CollectionUtils.isEmpty(executeLogs)) {
            return;
        }
        if (executeLogs.size() > 1) {
            log.warn("[Job-{}] frequent job should just have one running instance, there must have some bug.", jobId);
        }
        executeLogs.forEach(instance -> {
            try {
                // 重复查询了数据库，不过问题不大，这个调用量很小
                instanceService.stopInstance(instance.getInstanceId());
            }catch (Exception ignore) {
            }
        });
    }

    private void refreshJob(JobInfoDO jobInfoDO) throws Exception {
        // 计算下次调度时间
        Date now = new Date();
        TimeExpressionType timeExpressionType = TimeExpressionType.of(jobInfoDO.getTimeExpressionType());

        if (timeExpressionType == TimeExpressionType.CRON) {
            CronExpression cronExpression = new CronExpression(jobInfoDO.getTimeExpression());
            Date nextValidTime = cronExpression.getNextValidTimeAfter(now);
            jobInfoDO.setNextTriggerTime(nextValidTime.getTime());
        }else if (timeExpressionType == TimeExpressionType.API || timeExpressionType == TimeExpressionType.WORKFLOW) {
            jobInfoDO.setTimeExpression(null);
        }
        // 重写最后修改时间
        jobInfoDO.setGmtModified(now);
    }

}
