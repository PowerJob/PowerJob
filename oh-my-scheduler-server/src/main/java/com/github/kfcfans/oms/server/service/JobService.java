package com.github.kfcfans.oms.server.service;

import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.oms.server.common.constans.JobStatus;
import com.github.kfcfans.oms.server.persistence.model.InstanceLogDO;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.InstanceLogRepository;
import com.github.kfcfans.oms.server.persistence.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.service.id.IdGenerateService;
import com.github.kfcfans.oms.server.service.instance.InstanceService;
import lombok.extern.slf4j.Slf4j;
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
    private InstanceLogRepository instanceLogRepository;

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

        InstanceLogDO executeLog = new InstanceLogDO();
        executeLog.setJobId(jobInfo.getId());
        executeLog.setAppId(jobInfo.getAppId());
        executeLog.setInstanceId(instanceId);
        executeLog.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
        executeLog.setExpectedTriggerTime(System.currentTimeMillis());
        executeLog.setGmtCreate(new Date());
        executeLog.setGmtModified(executeLog.getGmtCreate());

        instanceLogRepository.saveAndFlush(executeLog);
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
        jobInfoRepository.saveAndFlush(jobInfoDO);

        // 2. 关闭秒级任务
        TimeExpressionType timeExpressionType = TimeExpressionType.of(jobInfoDO.getTimeExpressionType());
        if (timeExpressionType == TimeExpressionType.CRON || timeExpressionType == TimeExpressionType.API) {
            return;
        }
        List<InstanceLogDO> executeLogs = instanceLogRepository.findByJobIdAndStatusIn(jobId, InstanceStatus.generalizedRunningStatus);
        if (CollectionUtils.isEmpty(executeLogs)) {
            return;
        }
        if (executeLogs.size() > 1) {
            log.warn("[JobController] frequent job should just have one running instance, there must have some bug.");
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
