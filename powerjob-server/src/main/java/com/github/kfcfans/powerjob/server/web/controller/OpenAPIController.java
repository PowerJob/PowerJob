package com.github.kfcfans.powerjob.server.web.controller;

import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.OpenAPIConstant;
import com.github.kfcfans.powerjob.common.request.http.SaveWorkflowRequest;
import com.github.kfcfans.powerjob.server.service.AppInfoService;
import com.github.kfcfans.powerjob.server.service.CacheService;
import com.github.kfcfans.powerjob.server.service.JobService;
import com.github.kfcfans.powerjob.server.service.instance.InstanceService;
import com.github.kfcfans.powerjob.common.request.http.SaveJobInfoRequest;
import com.github.kfcfans.powerjob.server.service.workflow.WorkflowInstanceService;
import com.github.kfcfans.powerjob.server.service.workflow.WorkflowService;
import com.github.kfcfans.powerjob.common.response.*;
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
    private AppInfoService appInfoService;
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


    @PostMapping(OpenAPIConstant.ASSERT)
    public ResultDTO<Long> assertAppName(String appName, @RequestParam(required = false) String password) {
        return ResultDTO.success(appInfoService.assertApp(appName, password));
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
    public ResultDTO<Long> runJob(Long appId, Long jobId, @RequestParam(required = false) String instanceParams, @RequestParam(required = false) Long delay) {
        checkJobIdValid(jobId, appId);
        return ResultDTO.success(jobService.runJob(appId, jobId, instanceParams, delay));
    }

    /* ************* Instance 区 ************* */

    @PostMapping(OpenAPIConstant.STOP_INSTANCE)
    public ResultDTO<Void> stopInstance(Long instanceId, Long appId) {
        checkInstanceIdValid(instanceId, appId);
        instanceService.stopInstance(instanceId);
        return ResultDTO.success(null);
    }

    @PostMapping(OpenAPIConstant.CANCEL_INSTANCE)
    public ResultDTO<Void> cancelInstance(Long instanceId, Long appId) {
        checkInstanceIdValid(instanceId, appId);
        instanceService.cancelInstance(instanceId);
        return ResultDTO.success(null);
    }

    @PostMapping(OpenAPIConstant.RETRY_INSTANCE)
    public ResultDTO<Void> retryInstance(Long instanceId, Long appId) {
        checkInstanceIdValid(instanceId, appId);
        instanceService.retryInstance(appId, instanceId);
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
    public ResultDTO<Long> runWorkflow(Long workflowId, Long appId, @RequestParam(required = false) String initParams, @RequestParam(required = false) Long delay) {
        return ResultDTO.success(workflowService.runWorkflow(workflowId, appId, initParams, delay));
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
