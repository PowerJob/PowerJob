package tech.powerjob.server.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.OpenAPIConstant;
import tech.powerjob.common.PowerQuery;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.request.http.SaveWorkflowNodeRequest;
import tech.powerjob.common.request.http.SaveWorkflowRequest;
import tech.powerjob.common.request.query.JobInfoQuery;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.response.WorkflowInstanceInfoDTO;
import tech.powerjob.server.core.instance.InstanceService;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.core.service.CacheService;
import tech.powerjob.server.core.service.JobService;
import tech.powerjob.server.core.workflow.WorkflowInstanceService;
import tech.powerjob.server.core.workflow.WorkflowService;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowNodeInfoDO;
import tech.powerjob.server.web.response.WorkflowInfoVO;

import java.util.List;

/**
 * 开放接口（OpenAPI）控制器，对接 oms-client
 *
 * @author tjq
 * @since 2020/4/15
 */
@RestController
@RequestMapping(OpenAPIConstant.WEB_PATH)
@RequiredArgsConstructor
public class OpenAPIController {

    private final AppInfoService appInfoService;

    private final JobService jobService;

    private final InstanceService instanceService;

    private final WorkflowService workflowService;

    private final WorkflowInstanceService workflowInstanceService;

    private final CacheService cacheService;


    @PostMapping(OpenAPIConstant.ASSERT)
    public ResultDTO<Long> assertAppName(String appName, @RequestParam(required = false) String password) {
        return ResultDTO.success(appInfoService.assertApp(appName, password));
    }

    /* ************* Job 区 ************* */

    @PostMapping(OpenAPIConstant.SAVE_JOB)
    public ResultDTO<Long> saveJob(@RequestBody SaveJobInfoRequest request) {
        if (request.getId() != null) {
            checkJobIdValid(request.getId(), request.getAppId());
        }
        return ResultDTO.success(jobService.saveJob(request));
    }

    @PostMapping(OpenAPIConstant.COPY_JOB)
    public ResultDTO<Long> copyJob(Long jobId) {
        return ResultDTO.success(jobService.copyJob(jobId).getId());
    }

    @PostMapping(OpenAPIConstant.EXPORT_JOB)
    public ResultDTO<SaveJobInfoRequest> exportJob(Long jobId, Long appId) {
        checkJobIdValid(jobId, appId);
        return ResultDTO.success(jobService.exportJob(jobId));
    }

    @PostMapping(OpenAPIConstant.FETCH_JOB)
    public ResultDTO<JobInfoDTO> fetchJob(Long jobId, Long appId) {
        checkJobIdValid(jobId, appId);
        return ResultDTO.success(jobService.fetchJob(jobId));
    }

    @PostMapping(OpenAPIConstant.FETCH_ALL_JOB)
    public ResultDTO<List<JobInfoDTO>> fetchAllJob(Long appId) {
        return ResultDTO.success(jobService.fetchAllJob(appId));
    }

    @PostMapping(OpenAPIConstant.QUERY_JOB)
    public ResultDTO<List<JobInfoDTO>> queryJob(@RequestBody JobInfoQuery powerQuery) {
        return ResultDTO.success(jobService.queryJob(powerQuery));
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
    public ResultDTO<Void> enableJob(Long jobId, Long appId) {
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
        instanceService.stopInstance(appId, instanceId);
        return ResultDTO.success(null);
    }

    @PostMapping(OpenAPIConstant.CANCEL_INSTANCE)
    public ResultDTO<Void> cancelInstance(Long instanceId, Long appId) {
        checkInstanceIdValid(instanceId, appId);
        instanceService.cancelInstance(appId, instanceId);
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

    @PostMapping(OpenAPIConstant.QUERY_INSTANCE)
    public ResultDTO<List<InstanceInfoDTO>> queryInstance(@RequestBody PowerQuery powerQuery) {
        return ResultDTO.success(instanceService.queryInstanceInfo(powerQuery));
    }

    /* ************* Workflow 区 ************* */

    @PostMapping(OpenAPIConstant.SAVE_WORKFLOW)
    public ResultDTO<Long> saveWorkflow(@RequestBody SaveWorkflowRequest request) {
        return ResultDTO.success(workflowService.saveWorkflow(request));
    }

    @PostMapping(OpenAPIConstant.COPY_WORKFLOW)
    public ResultDTO<Long> copy(Long workflowId, Long appId) {
        return ResultDTO.success(workflowService.copyWorkflow(workflowId, appId));
    }


    @PostMapping(OpenAPIConstant.FETCH_WORKFLOW)
    public ResultDTO<WorkflowInfoVO> fetchWorkflow(Long workflowId, Long appId) {
        WorkflowInfoDO workflowInfoDO = workflowService.fetchWorkflow(workflowId, appId);
        return ResultDTO.success(WorkflowInfoVO.from(workflowInfoDO));
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

    @PostMapping(OpenAPIConstant.SAVE_WORKFLOW_NODE)
    public ResultDTO<List<WorkflowNodeInfoDO>> saveWorkflowNode(@RequestBody List<SaveWorkflowNodeRequest> request) {
        return ResultDTO.success(workflowService.saveWorkflowNode(request));
    }

    /* ************* Workflow Instance 区 ************* */

    @PostMapping(OpenAPIConstant.STOP_WORKFLOW_INSTANCE)
    public ResultDTO<Void> stopWorkflowInstance(Long wfInstanceId, Long appId) {
        workflowInstanceService.stopWorkflowInstanceEntrance(wfInstanceId, appId);
        return ResultDTO.success(null);
    }

    @PostMapping(OpenAPIConstant.RETRY_WORKFLOW_INSTANCE)
    public ResultDTO<Void> retryWorkflowInstance(Long wfInstanceId, Long appId) {
        workflowInstanceService.retryWorkflowInstance(wfInstanceId, appId);
        return ResultDTO.success(null);
    }

    @PostMapping(OpenAPIConstant.MARK_WORKFLOW_NODE_AS_SUCCESS)
    public ResultDTO<Void> markWorkflowNodeAsSuccess(Long wfInstanceId, Long nodeId, Long appId) {
        workflowInstanceService.markNodeAsSuccess(appId, wfInstanceId, nodeId);
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
