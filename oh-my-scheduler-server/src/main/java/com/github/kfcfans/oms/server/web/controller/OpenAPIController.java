package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.oms.common.InstanceStatus;
import com.github.kfcfans.oms.common.OpenAPIConstant;
import com.github.kfcfans.oms.common.request.http.SaveWorkflowRequest;
import com.github.kfcfans.oms.common.response.*;
import com.github.kfcfans.oms.server.persistence.core.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.service.CacheService;
import com.github.kfcfans.oms.server.service.JobService;
import com.github.kfcfans.oms.server.service.instance.InstanceService;
import com.github.kfcfans.oms.common.request.http.SaveJobInfoRequest;
import com.github.kfcfans.oms.server.service.workflow.WorkflowInstanceService;
import com.github.kfcfans.oms.server.service.workflow.WorkflowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 开放接口（OpenAPI）控制器，对接 oms-client
 *
 * @author tjq
 * @since 2020/4/15
 */
@RestController
@RequestMapping(OpenAPIConstant.WEB_PATH)
public class OpenAPIController {

    @Resource
    private JobService jobService;
    @Resource
    private InstanceService instanceService;
    @Resource
    private WorkflowService workflowService;
    @Resource
    private WorkflowInstanceService workflowInstanceService;

    @Resource
    private CacheService cacheService;

    @Resource
    private AppInfoRepository appInfoRepository;

    @GetMapping(OpenAPIConstant.ASSERT)
    public ResultDTO<Long> assertAppName(String appName) {
        AppInfoDO appInfo = appInfoRepository.findByAppName(appName);
        if (appInfo == null) {
            return ResultDTO.failed(appName + " is not registered!");
        }
        return ResultDTO.success(appInfo.getId());
    }

    /* ************* Job 区 ************* */
    @PostMapping(OpenAPIConstant.SAVE_JOB)
    public ResultDTO<Long> saveJob(@RequestBody SaveJobInfoRequest request) throws Exception {
        if (request.getId() != null) {
            checkJobIdValid(request.getId(), request.getAppId());
        }
        return ResultDTO.success(jobService.saveJob(request));
    }

    @PostMapping(OpenAPIConstant.FETCH_JOB)
    public ResultDTO<JobInfoDTO> fetchJob(Long jobId, Long appId) {
        checkJobIdValid(jobId, appId);
        return ResultDTO.success(jobService.fetchJob(jobId));
    }

    @PostMapping(OpenAPIConstant.DELETE_JOB)
    public ResultDTO<Void> deleteJob(Long jobId, Long appId) {
        checkJobIdValid(jobId, appId);
        jobService.deleteJob(jobId);
        return ResultDTO.success(null);
    }
    @PostMapping(OpenAPIConstant.DISABLE_JOB)
    public ResultDTO<Void> disableJob(Long jobId, Long appId) {
        checkJobIdValid(jobId, appId);
        jobService.disableJob(jobId);
        return ResultDTO.success(null);
    }
    @PostMapping(OpenAPIConstant.ENABLE_JOB)
    public ResultDTO<Void> enableJob(Long jobId, Long appId) throws Exception {
        checkJobIdValid(jobId, appId);
        jobService.enableJob(jobId);
        return ResultDTO.success(null);
    }

    @PostMapping(OpenAPIConstant.RUN_JOB)
    public ResultDTO<Long> runJob(Long appId, Long jobId, @RequestParam(required = false) String instanceParams) {
        checkJobIdValid(jobId, appId);
        return ResultDTO.success(jobService.runJob(jobId, instanceParams));
    }

    /* ************* Instance 区 ************* */

    @PostMapping(OpenAPIConstant.STOP_INSTANCE)
    public ResultDTO<Void> stopInstance(Long instanceId, Long appId) {
        checkInstanceIdValid(instanceId, appId);
        instanceService.stopInstance(instanceId);
        return ResultDTO.success(null);
    }

    @PostMapping(OpenAPIConstant.FETCH_INSTANCE_STATUS)
    public ResultDTO<Integer> fetchInstanceStatus(Long instanceId) {
        InstanceStatus instanceStatus = instanceService.getInstanceStatus(instanceId);
        return ResultDTO.success(instanceStatus.getV());
    }

    @PostMapping(OpenAPIConstant.FETCH_INSTANCE_INFO)
    public ResultDTO<InstanceInfoDTO> fetchInstanceInfo(Long instanceId) {
        return ResultDTO.success(instanceService.getInstanceInfo(instanceId));
    }

    /* ************* Workflow 区 ************* */
    @PostMapping(OpenAPIConstant.SAVE_WORKFLOW)
    public ResultDTO<Long> saveWorkflow(@RequestBody SaveWorkflowRequest request) throws Exception {
        if (request.getId() != null) {
            checkJobIdValid(request.getId(), request.getAppId());
        }
        return ResultDTO.success(workflowService.saveWorkflow(request));
    }

    @PostMapping(OpenAPIConstant.FETCH_WORKFLOW)
    public ResultDTO<WorkflowInfoDTO> fetchWorkflow(Long workflowId, Long appId) {
        return ResultDTO.success(workflowService.fetchWorkflow(workflowId, appId));
    }

    @PostMapping(OpenAPIConstant.DELETE_WORKFLOW)
    public ResultDTO<Void> deleteWorkflow(Long workflowId, Long appId) {
        workflowService.deleteWorkflow(workflowId, appId);
        return ResultDTO.success(null);
    }
    @PostMapping(OpenAPIConstant.DISABLE_WORKFLOW)
    public ResultDTO<Void> disableWorkflow(Long workflowId, Long appId) {
        workflowService.disableWorkflow(workflowId, appId);
        return ResultDTO.success(null);
    }
    @PostMapping(OpenAPIConstant.ENABLE_WORKFLOW)
    public ResultDTO<Void> enableWorkflow(Long workflowId, Long appId) {
        workflowService.enableWorkflow(workflowId, appId);
        return ResultDTO.success(null);
    }

    @PostMapping(OpenAPIConstant.RUN_WORKFLOW)
    public ResultDTO<Long> runWorkflow(Long workflowId, Long appId) {
        return ResultDTO.success(workflowService.runWorkflow(workflowId, appId));
    }

    /* ************* Workflow Instance 区 ************* */
    @PostMapping(OpenAPIConstant.STOP_WORKFLOW_INSTANCE)
    public ResultDTO<Void> stopWorkflowInstance(Long wfInstanceId, Long appId) {
        workflowInstanceService.stopWorkflowInstance(wfInstanceId, appId);
        return ResultDTO.success(null);
    }
    @PostMapping(OpenAPIConstant.FETCH_WORKFLOW_INSTANCE_INFO)
    public ResultDTO<WorkflowInstanceInfoDTO> fetchWorkflowInstanceInfo(Long wfInstanceId, Long appId) {
        return ResultDTO.success(workflowInstanceService.fetchWorkflowInstanceInfo(wfInstanceId, appId));
    }

    private void checkInstanceIdValid(Long instanceId, Long appId) {
        Long realAppId = cacheService.getAppIdByInstanceId(instanceId);
        if (realAppId == null) {
            throw new IllegalArgumentException("can't find instance by instanceId: " + instanceId);
        }
        if (appId.equals(realAppId)) {
            return;
        }
        throw new IllegalArgumentException("instance is not belong to the app whose appId is " + appId);
    }

    private void checkJobIdValid(Long jobId, Long appId) {
        Long realAppId = cacheService.getAppIdByJobId(jobId);
        // 查不到，说明 jobId 不存在
        if (realAppId == null) {
            throw new IllegalArgumentException("can't find job by jobId: " + jobId);
        }
        // 不等，说明该job不属于该app，无权限操作
        if (!appId.equals(realAppId)) {
            throw new IllegalArgumentException("this job is not belong to the app whose appId is " + appId);
        }
    }
}
