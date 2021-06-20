package tech.powerjob.server.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tech.powerjob.common.PowerQuery;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.server.common.SJ;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.server.common.timewheel.holder.InstanceTimeWheelService;
import tech.powerjob.server.common.utils.TimeUtils;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.instance.InstanceService;
import tech.powerjob.server.persistence.QueryConvertUtils;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.repository.InstanceInfoRepository;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;
import tech.powerjob.server.remote.server.redirector.DesignateServer;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
     *
     * @param request 任务请求
     * @return 创建的任务ID（jobId）
     * @exception ParseException 异常
     */
    public Long saveJob(SaveJobInfoRequest request) throws ParseException {

        request.valid();

        JobInfoDO jobInfoDO;
        if (request.getId() != null) {
            jobInfoDO = jobInfoRepository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("can't find job by jobId: " + request.getId()));
        } else {
            jobInfoDO = new JobInfoDO();
        }

        // 值拷贝
        BeanUtils.copyProperties(request, jobInfoDO);

        // 拷贝枚举值
        jobInfoDO.setExecuteType(request.getExecuteType().getV());
        jobInfoDO.setProcessorType(request.getProcessorType().getV());
        jobInfoDO.setTimeExpressionType(request.getTimeExpressionType().getV());
        jobInfoDO.setStatus(request.isEnable() ? SwitchableStatus.ENABLE.getV() : SwitchableStatus.DISABLE.getV());
        jobInfoDO.setDispatchStrategy(request.getDispatchStrategy().getV());

        // 填充默认值，非空保护防止 NPE
        fillDefaultValue(jobInfoDO);

        // 转化报警用户列表
        if (!CollectionUtils.isEmpty(request.getNotifyUserIds())) {
            jobInfoDO.setNotifyUserIds(SJ.COMMA_JOINER.join(request.getNotifyUserIds()));
        }

        calculateNextTriggerTime(jobInfoDO);
        if (request.getId() == null) {
            jobInfoDO.setGmtCreate(new Date());
        }
        JobInfoDO res = jobInfoRepository.saveAndFlush(jobInfoDO);
        return res.getId();
    }

    /**
     * 复制任务
     * @param jobId 目标任务ID
     * @return 复制后的任务 ID
     */
    public JobInfoDO copyJob(Long jobId) {

        JobInfoDO origin = jobInfoRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("can't find job by jobId: " + jobId));
        if (origin.getStatus() == SwitchableStatus.DELETED.getV()) {
            throw new IllegalStateException("can't copy the job which has been deleted!");
        }
        JobInfoDO copyJob = new JobInfoDO();
        // 值拷贝
        BeanUtils.copyProperties(origin, copyJob);
        // 填充默认值，理论上应该不需要
        fillDefaultValue(copyJob);
        // 修正创建时间以及更新时间
        copyJob.setId(null);
        copyJob.setJobName(copyJob.getJobName()+"_COPY");
        copyJob.setGmtCreate(new Date());
        copyJob.setGmtModified(new Date());

        copyJob = jobInfoRepository.saveAndFlush(copyJob);
        return copyJob;

    }


    public JobInfoDTO fetchJob(Long jobId) {
        return convert(jobInfoRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("can't find job by jobId: " + jobId)));
    }

    public List<JobInfoDTO> fetchAllJob(Long appId) {
        return jobInfoRepository.findByAppId(appId).stream().map(JobService::convert).collect(Collectors.toList());
    }

    public List<JobInfoDTO> queryJob(PowerQuery powerQuery) {
        Specification<JobInfoDO> specification = QueryConvertUtils.toSpecification(powerQuery);
        return jobInfoRepository.findAll(specification).stream().map(JobService::convert).collect(Collectors.toList());
    }

    /**
     * 手动立即运行某个任务
     *
     * @param jobId          任务ID
     * @param instanceParams 任务实例参数（仅 OpenAPI 存在）
     * @param delay          延迟时间，单位 毫秒
     * @return 任务实例ID
     */
    @DesignateServer
    public long runJob(Long appId, Long jobId, String instanceParams, Long delay) {

        delay = delay == null ? 0 : delay;
        JobInfoDO jobInfo = jobInfoRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("can't find job by id:" + jobId));

        log.info("[Job-{}] try to run job in app[{}], instanceParams={},delay={} ms.", jobInfo.getId(), appId, instanceParams, delay);
        Long instanceId = instanceService.create(jobInfo.getId(), jobInfo.getAppId(), jobInfo.getJobParams(), instanceParams, null, System.currentTimeMillis() + Math.max(delay, 0));
        instanceInfoRepository.flush();
        if (delay <= 0) {
            dispatchService.dispatch(jobInfo, instanceId);
        } else {
            InstanceTimeWheelService.schedule(instanceId, delay, () -> dispatchService.dispatch(jobInfo, instanceId));
        }
        log.info("[Job-{}|{}] execute 'runJob' successfully, params={}", jobInfo.getId(), instanceId, instanceParams);
        return instanceId;
    }


    /**
     * 删除某个任务
     *
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
     *
     * @param jobId 任务ID
     * @exception ParseException 异常（CRON表达式错误）
     */
    public void enableJob(Long jobId) throws ParseException {
        JobInfoDO jobInfoDO = jobInfoRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("can't find job by jobId:" + jobId));

        jobInfoDO.setStatus(SwitchableStatus.ENABLE.getV());
        calculateNextTriggerTime(jobInfoDO);

        jobInfoRepository.saveAndFlush(jobInfoDO);
    }

    /**
     * 停止或删除某个JOB
     * 秒级任务还要额外停止正在运行的任务实例
     */
    private void shutdownOrStopJob(Long jobId, SwitchableStatus status) {

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
        List<InstanceInfoDO> executeLogs = instanceInfoRepository.findByJobIdAndStatusIn(jobId, InstanceStatus.GENERALIZED_RUNNING_STATUS);
        if (CollectionUtils.isEmpty(executeLogs)) {
            return;
        }
        if (executeLogs.size() > 1) {
            log.warn("[Job-{}] frequent job should just have one running instance, there must have some bug.", jobId);
        }
        executeLogs.forEach(instance -> {
            try {
                // 重复查询了数据库，不过问题不大，这个调用量很小
                instanceService.stopInstance(instance.getAppId(), instance.getInstanceId());
            } catch (Exception ignore) {
                // ignore exception
            }
        });
    }

    private void calculateNextTriggerTime(JobInfoDO jobInfoDO) throws ParseException {
        // 计算下次调度时间
        Date now = new Date();
        TimeExpressionType timeExpressionType = TimeExpressionType.of(jobInfoDO.getTimeExpressionType());

        if (timeExpressionType == TimeExpressionType.CRON) {
            Date nextValidTime = TimeUtils.calculateNextCronTime(jobInfoDO.getTimeExpression(), System.currentTimeMillis(), jobInfoDO.getLifecycle());
            if (nextValidTime == null) {
                throw new PowerJobException("cron expression is out of date: " + jobInfoDO.getTimeExpression());
            }
            jobInfoDO.setNextTriggerTime(nextValidTime.getTime());
        } else if (timeExpressionType == TimeExpressionType.API || timeExpressionType == TimeExpressionType.WORKFLOW) {
            jobInfoDO.setTimeExpression(null);
        }
        // 重写最后修改时间
        jobInfoDO.setGmtModified(now);
    }

    private void fillDefaultValue(JobInfoDO jobInfoDO) {
        if (jobInfoDO.getMaxWorkerCount() == null) {
            jobInfoDO.setMaxWorkerCount(0);
        }
        if (jobInfoDO.getMaxInstanceNum() == null) {
            jobInfoDO.setMaxInstanceNum(0);
        }
        if (jobInfoDO.getConcurrency() == null) {
            jobInfoDO.setConcurrency(5);
        }
        if (jobInfoDO.getInstanceRetryNum() == null) {
            jobInfoDO.setInstanceRetryNum(0);
        }
        if (jobInfoDO.getTaskRetryNum() == null) {
            jobInfoDO.setTaskRetryNum(0);
        }
        if (jobInfoDO.getInstanceTimeLimit() == null) {
            jobInfoDO.setInstanceTimeLimit(0L);
        }
    }

    private static JobInfoDTO convert(JobInfoDO jobInfoDO) {
        JobInfoDTO jobInfoDTO = new JobInfoDTO();
        BeanUtils.copyProperties(jobInfoDO, jobInfoDTO);
        return jobInfoDTO;
    }

}
