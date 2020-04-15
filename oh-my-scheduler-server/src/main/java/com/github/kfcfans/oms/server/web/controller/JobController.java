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
import com.github.kfcfans.oms.server.service.id.IdGenerateService;
import com.github.kfcfans.oms.server.service.instance.InstanceService;
import com.github.kfcfans.oms.server.web.request.ModifyJobInfoRequest;
import com.github.kfcfans.oms.server.web.request.QueryJobInfoRequest;
import com.github.kfcfans.oms.server.web.response.JobInfoVO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
    private IdGenerateService idGenerateService;
    @Resource
    private InstanceService instanceService;

    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private InstanceLogRepository instanceLogRepository;

    @PostMapping("/save")
    public ResultDTO<Void> saveJobInfo(@RequestBody ModifyJobInfoRequest request) throws Exception {

        JobInfoDO jobInfoDO = new JobInfoDO();
        BeanUtils.copyProperties(request, jobInfoDO);

        // 拷贝枚举值
        TimeExpressionType timeExpressionType = TimeExpressionType.valueOf(request.getTimeExpressionType());
        jobInfoDO.setExecuteType(ExecuteType.valueOf(request.getExecuteType()).getV());
        jobInfoDO.setProcessorType(ProcessorType.valueOf(request.getProcessorType()).getV());
        jobInfoDO.setTimeExpressionType(timeExpressionType.getV());
        jobInfoDO.setStatus(request.isEnable() ? JobStatus.ENABLE.getV() : JobStatus.STOPPED.getV());

        if (jobInfoDO.getMaxWorkerCount() == null) {
            jobInfoDO.setMaxInstanceNum(0);
        }

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

    @PostMapping("/list")
    public ResultDTO<PageResult<JobInfoVO>> listJobs(@RequestBody QueryJobInfoRequest request) {

        Sort sort = Sort.by(Sort.Direction.DESC, "gmtCreate");
        PageRequest pageRequest = PageRequest.of(request.getIndex(), request.getPageSize(), sort);
        Page<JobInfoDO> jobInfoPage;

        // 无查询条件，查询全部
        if (request.getJobId() == null && StringUtils.isEmpty(request.getKeyword())) {
            jobInfoPage = jobInfoRepository.findByAppIdAndStatusNot(request.getAppId(), pageRequest, JobStatus.DELETED.getV());
            return ResultDTO.success(convertPage(jobInfoPage));
        }

        // 有 jobId，直接精确查询
        if (request.getJobId() != null) {

            Optional<JobInfoDO> jobInfoOpt = jobInfoRepository.findById(request.getJobId());
            PageResult<JobInfoVO> result = new PageResult<>();
            result.setIndex(0);
            result.setPageSize(request.getPageSize());

            if (jobInfoOpt.isPresent()) {
                result.setTotalItems(1);
                result.setTotalPages(1);
                result.setData(Lists.newArrayList(convert(jobInfoOpt.get())));
            }else {
                result.setTotalPages(0);
                result.setTotalItems(0);
                result.setData(Lists.newLinkedList());
            }

            return ResultDTO.success(result);
        }

        // 模糊查询
        String condition = "%" + request.getKeyword() + "%";
        jobInfoPage = jobInfoRepository.findByAppIdAndJobNameLikeAndStatusNot(request.getAppId(), condition, JobStatus.DELETED.getV(), pageRequest);
        return ResultDTO.success(convertPage(jobInfoPage));
    }

    /**
     * 立即运行JOB
     */
    private void runJobImmediately(JobInfoDO jobInfoDO) {
        InstanceLogDO executeLog = new InstanceLogDO();
        executeLog.setJobId(jobInfoDO.getId());
        executeLog.setAppId(jobInfoDO.getAppId());
        executeLog.setInstanceId(idGenerateService.allocate());
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

    private static PageResult<JobInfoVO> convertPage(Page<JobInfoDO> jobInfoPage) {
        List<JobInfoVO> jobInfoVOList = jobInfoPage.getContent().stream().map(JobController::convert).collect(Collectors.toList());

        PageResult<JobInfoVO> pageResult = new PageResult<>(jobInfoPage);
        pageResult.setData(jobInfoVOList);
        return pageResult;
    }

    private static JobInfoVO convert(JobInfoDO jobInfoDO) {
        JobInfoVO jobInfoVO = new JobInfoVO();
        BeanUtils.copyProperties(jobInfoDO, jobInfoVO);

        TimeExpressionType timeExpressionType = TimeExpressionType.of(jobInfoDO.getTimeExpressionType());
        ExecuteType executeType = ExecuteType.of(jobInfoDO.getExecuteType());
        ProcessorType processorType = ProcessorType.of(jobInfoDO.getProcessorType());

        jobInfoVO.setTimeExpressionType(timeExpressionType.name());
        jobInfoVO.setExecuteType(executeType.name());
        jobInfoVO.setProcessorType(processorType.name());
        jobInfoVO.setEnable(jobInfoDO.getStatus() == JobStatus.ENABLE.getV());

        return jobInfoVO;
    }


}
