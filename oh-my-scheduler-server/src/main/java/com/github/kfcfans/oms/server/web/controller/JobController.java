package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.ProcessorType;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.oms.server.common.constans.JobStatus;
import com.github.kfcfans.oms.server.common.utils.CronExpression;
import com.github.kfcfans.oms.server.persistence.model.ExecuteLogDO;
import com.github.kfcfans.oms.server.persistence.repository.ExecuteLogRepository;
import com.github.kfcfans.oms.server.persistence.repository.JobInfoRepository;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.service.DispatchService;
import com.github.kfcfans.oms.server.service.IdGenerateService;
import com.github.kfcfans.oms.server.web.request.ModifyJobInfoRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

/**
 * 任务信息管理 Controller
 *
 * @author tjq
 * @since 2020/3/30
 */
@RestController
@RequestMapping("job")
public class JobController {

    @Resource
    private DispatchService dispatchService;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private ExecuteLogRepository executeLogRepository;

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

            ExecuteLogDO executeLog = new ExecuteLogDO();
            executeLog.setJobId(jobInfoDO.getId());
            executeLog.setAppId(jobInfoDO.getAppId());
            executeLog.setInstanceId(IdGenerateService.allocate());
            executeLog.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
            executeLog.setExpectedTriggerTime(System.currentTimeMillis());
            executeLog.setGmtCreate(new Date());
            executeLog.setGmtModified(executeLog.getGmtCreate());

            executeLogRepository.saveAndFlush(executeLog);
            dispatchService.dispatch(jobInfoDO, executeLog.getInstanceId(), 0);
        }

        return ResultDTO.success(null);
    }

    @GetMapping("/stop")
    public ResultDTO<Void> stopJob(Long jobId) throws Exception {
        updateJobStatus(jobId, JobStatus.STOPPED);
        return ResultDTO.success(null);
    }

    @GetMapping("/delete")
    public ResultDTO<Void> deleteJob(Long jobId) throws Exception {
        updateJobStatus(jobId, JobStatus.DELETED);
        return ResultDTO.success(null);
    }

    private void updateJobStatus(Long jobId, JobStatus status) {
        JobInfoDO jobInfoDO = jobInfoRepository.findById(jobId).orElseThrow(() -> {
            throw new IllegalArgumentException("can't find job which id is " + jobId);
        });
        jobInfoDO.setStatus(status.getV());
        jobInfoRepository.saveAndFlush(jobInfoDO);

        // TODO: 关闭秒级任务

    }

}
