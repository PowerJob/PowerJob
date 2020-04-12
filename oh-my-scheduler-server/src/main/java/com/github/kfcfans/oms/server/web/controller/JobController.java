package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.ProcessorType;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.oms.server.common.constans.JobStatus;
import com.github.kfcfans.oms.server.common.utils.CronExpression;
import com.github.kfcfans.oms.server.persistence.PageResult;
import com.github.kfcfans.oms.server.persistence.model.InstanceLogDO;
import com.github.kfcfans.oms.server.persistence.repository.InstanceLogRepository;
import com.github.kfcfans.oms.server.persistence.repository.JobInfoRepository;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.service.DispatchService;
import com.github.kfcfans.oms.server.service.IdGenerateService;
import com.github.kfcfans.oms.server.service.instance.InstanceService;
import com.github.kfcfans.oms.server.web.request.ModifyJobInfoRequest;
import com.github.kfcfans.oms.server.web.response.JobInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务信息管理 Controller
 *
 * @author tjq
 * @since 2020/3/30
 */
@Slf4j
@RestController
@RequestMapping("job")
public class JobController {

    @Resource
    private DispatchService dispatchService;
    @Resource
    private InstanceService instanceService;

    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private InstanceLogRepository instanceLogRepository;

    @PostMapping("/save")
    public ResultDTO<Void> saveJobInfo(ModifyJobInfoRequest request) throws Exception {

        JobInfoDO jobInfoDO = new JobInfoDO();
        BeanUtils.copyProperties(request, jobInfoDO);

        // 拷贝枚举值
        TimeExpressionType timeExpressionType = TimeExpressionType.valueOf(request.getTimeExpressionType());
        jobInfoDO.setExecuteType(ExecuteType.valueOf(request.getExecuteType()).getV());
        jobInfoDO.setProcessorType(ProcessorType.valueOf(request.getProcessorType()).getV());
        jobInfoDO.setTimeExpressionType(timeExpressionType.getV());
        // 计算下次调度时间
        Date now = new Date();
        if (timeExpressionType == TimeExpressionType.CRON) {
            CronExpression cronExpression = new CronExpression(request.getTimeExpression());
            Date nextValidTime = cronExpression.getNextValidTimeAfter(now);
            jobInfoDO.setNextTriggerTime(nextValidTime.getTime());
        }

        if (request.getId() == null) {
            jobInfoDO.setGmtCreate(now);
        }
        jobInfoDO.setGmtModified(now);
        jobInfoRepository.saveAndFlush(jobInfoDO);

        // 秒级任务直接调度执行
        if (timeExpressionType == TimeExpressionType.FIX_RATE || timeExpressionType == TimeExpressionType.FIX_DELAY) {
            runJobImmediately(jobInfoDO);
        }

        return ResultDTO.success(null);
    }

    @GetMapping("/stop")
    public ResultDTO<Void> stopJob(Long jobId) throws Exception {
        shutdownOrStopJob(jobId, JobStatus.STOPPED);
        return ResultDTO.success(null);
    }

    @GetMapping("/delete")
    public ResultDTO<Void> deleteJob(Long jobId) throws Exception {
        shutdownOrStopJob(jobId, JobStatus.DELETED);
        return ResultDTO.success(null);
    }

    @GetMapping("/run")
    public ResultDTO<Void> runImmediately(Long jobId) {
        Optional<JobInfoDO> jobInfoOPT = jobInfoRepository.findById(jobId);
        if (!jobInfoOPT.isPresent()) {
            throw new IllegalArgumentException("can't find job by jobId:" + jobId);
        }
        runJobImmediately(jobInfoOPT.get());
        return ResultDTO.success(null);
    }

    @GetMapping("/list")
    public ResultDTO<PageResult<JobInfoVO>> listJobs(Long appId, int index, int pageSize) {

        Sort sort = Sort.by(Sort.Direction.DESC, "gmtCreate");
        PageRequest pageRequest = PageRequest.of(index, pageSize, sort);
        Page<JobInfoDO> jobInfoPage = jobInfoRepository.findByAppId(appId, pageRequest);
        List<JobInfoVO> jobInfoVOList = jobInfoPage.getContent().stream().map(jobInfoDO -> {
            JobInfoVO jobInfoVO = new JobInfoVO();
            BeanUtils.copyProperties(jobInfoDO, jobInfoVO);
            return jobInfoVO;
        }).collect(Collectors.toList());

        PageResult<JobInfoVO> pageResult = new PageResult<>(jobInfoPage);
        pageResult.setData(jobInfoVOList);
        return ResultDTO.success(pageResult);
    }

    /**
     * 立即运行JOB
     */
    private void runJobImmediately(JobInfoDO jobInfoDO) {
        InstanceLogDO executeLog = new InstanceLogDO();
        executeLog.setJobId(jobInfoDO.getId());
        executeLog.setAppId(jobInfoDO.getAppId());
        executeLog.setInstanceId(IdGenerateService.allocate());
        executeLog.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
        executeLog.setExpectedTriggerTime(System.currentTimeMillis());
        executeLog.setGmtCreate(new Date());
        executeLog.setGmtModified(executeLog.getGmtCreate());

        instanceLogRepository.saveAndFlush(executeLog);
        dispatchService.dispatch(jobInfoDO, executeLog.getInstanceId(), 0);
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
            log.warn("[JobController] frequent job has multi instance, there must ha");
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
