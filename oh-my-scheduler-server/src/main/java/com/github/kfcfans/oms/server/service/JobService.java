package com.github.kfcfans.oms.server.service;

import com.github.kfcfans.oms.common.ExecuteType;
import com.github.kfcfans.oms.common.InstanceStatus;
import com.github.kfcfans.oms.common.ProcessorType;
import com.github.kfcfans.oms.common.TimeExpressionType;
import com.github.kfcfans.oms.common.request.http.JobInfoRequest;
import com.github.kfcfans.oms.server.common.constans.JobStatus;
import com.github.kfcfans.oms.server.common.utils.CronExpression;
import com.github.kfcfans.oms.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.service.id.IdGenerateService;
import com.github.kfcfans.oms.server.service.instance.InstanceService;
import com.google.common.base.Joiner;
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
    private IdGenerateService idGenerateService;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    private static final Joiner commaJoiner = Joiner.on(",").skipNulls();

    /**
     * 保存/修改任务
     * @param request 任务请求
     * @return 创建的任务ID（jobId）
     * @throws Exception 异常
     */
    public Long saveJob(JobInfoRequest request) throws Exception {

        JobInfoDO jobInfoDO;
        if (request.getId() != null) {
            jobInfoDO = jobInfoRepository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("can't find job by jobId: " + request.getId()));
        }else {
            jobInfoDO = new JobInfoDO();
        }

        // 值拷贝
        BeanUtils.copyProperties(request, jobInfoDO);

        // 拷贝枚举值
        TimeExpressionType timeExpressionType = TimeExpressionType.valueOf(request.getTimeExpressionType());
        jobInfoDO.setExecuteType(ExecuteType.valueOf(request.getExecuteType()).getV());
        jobInfoDO.setProcessorType(ProcessorType.valueOf(request.getProcessorType()).getV());
        jobInfoDO.setTimeExpressionType(timeExpressionType.getV());
        jobInfoDO.setStatus(request.isEnable() ? JobStatus.ENABLE.getV() : JobStatus.DISABLE.getV());

        if (jobInfoDO.getMaxWorkerCount() == null) {
            jobInfoDO.setMaxInstanceNum(0);
        }

        // 转化报警用户列表
        if (!CollectionUtils.isEmpty(request.getNotifyUserIds())) {
            jobInfoDO.setNotifyUserIds(commaJoiner.join(request.getNotifyUserIds()));
        }

        // 计算下次调度时间
        Date now = new Date();
        if (timeExpressionType == TimeExpressionType.CRON) {
            CronExpression cronExpression = new CronExpression(request.getTimeExpression());
            Date nextValidTime = cronExpression.getNextValidTimeAfter(now);
            jobInfoDO.setNextTriggerTime(nextValidTime.getTime());
        }

        jobInfoDO.setGmtModified(now);
        if (request.getId() == null) {
            jobInfoDO.setGmtCreate(now);
        }
        JobInfoDO res = jobInfoRepository.saveAndFlush(jobInfoDO);
        return res.getId();
    }

    /**
     * 手动立即运行某个任务
     * @param jobId 任务ID
     * @param instanceParams 任务实例参数
     * @return 任务实例ID
     */
    public long runJob(Long jobId, String instanceParams) {
        Optional<JobInfoDO> jobInfoOPT = jobInfoRepository.findById(jobId);
        if (!jobInfoOPT.isPresent()) {
            throw new IllegalArgumentException("can't find job by jobId:" + jobId);
        }
        return runJob(jobInfoOPT.get(), instanceParams);
    }

    public long runJob(JobInfoDO jobInfo, String instanceParams) {
        long instanceId = idGenerateService.allocate();

        InstanceInfoDO executeLog = new InstanceInfoDO();
        executeLog.setJobId(jobInfo.getId());
        executeLog.setAppId(jobInfo.getAppId());
        executeLog.setInstanceId(instanceId);
        executeLog.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
        executeLog.setExpectedTriggerTime(System.currentTimeMillis());
        executeLog.setGmtCreate(new Date());
        executeLog.setGmtModified(executeLog.getGmtCreate());

        instanceInfoRepository.saveAndFlush(executeLog);
        dispatchService.dispatch(jobInfo, executeLog.getInstanceId(), 0, instanceParams);
        return instanceId;
    }

    /**
     * 删除某个任务
     * @param jobId 任务ID
     */
    public void deleteJob(Long jobId) {
        shutdownOrStopJob(jobId, JobStatus.DELETED);
    }

    /**
     * 禁用某个任务
     */
    public void disableJob(Long jobId) {
        shutdownOrStopJob(jobId, JobStatus.DISABLE);
    }

    /**
     * 停止或删除某个JOB
     * 秒级任务还要额外停止正在运行的任务实例
     */
    private void shutdownOrStopJob(Long jobId, JobStatus status) throws IllegalArgumentException {

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
        TimeExpressionType timeExpressionType = TimeExpressionType.of(jobInfoDO.getTimeExpressionType());
        if (timeExpressionType == TimeExpressionType.CRON || timeExpressionType == TimeExpressionType.API) {
            return;
        }
        List<InstanceInfoDO> executeLogs = instanceInfoRepository.findByJobIdAndStatusIn(jobId, InstanceStatus.generalizedRunningStatus);
        if (CollectionUtils.isEmpty(executeLogs)) {
            return;
        }
        if (executeLogs.size() > 1) {
            log.warn("[JobService] frequent job should just have one running instance, there must have some bug.");
        }
        executeLogs.forEach(instance -> {
            try {
                // 重复查询了数据库，不过问题不大，这个调用量很小
                instanceService.stopInstance(instance.getInstanceId());
            }catch (Exception ignore) {
            }
        });
    }

}
