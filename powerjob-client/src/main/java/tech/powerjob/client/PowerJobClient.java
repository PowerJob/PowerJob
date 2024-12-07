package tech.powerjob.client;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.client.module.AppAuthRequest;
import tech.powerjob.client.module.AppAuthResult;
import tech.powerjob.client.service.PowerRequestBody;
import tech.powerjob.client.service.RequestService;
import tech.powerjob.client.service.impl.ClusterRequestServiceOkHttp3Impl;
import tech.powerjob.common.OpenAPIConstant;
import tech.powerjob.common.enums.EncryptType;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.request.http.SaveWorkflowNodeRequest;
import tech.powerjob.common.request.http.SaveWorkflowRequest;
import tech.powerjob.common.request.query.InstancePageQuery;
import tech.powerjob.common.request.query.JobInfoQuery;
import tech.powerjob.common.response.*;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.DigestUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static tech.powerjob.client.TypeStore.*;

/**
 * PowerJobClient, the client for OpenAPI.
 *
 * @author tjq
 * @since 2020/4/15
 */
@Slf4j
public class PowerJobClient implements IPowerJobClient, Closeable {

    private Long appId;
    
    private final RequestService requestService;

    public PowerJobClient(ClientConfig config) {
        
        List<String> addressList = config.getAddressList();
        String appName = config.getAppName();

        CommonUtils.requireNonNull(addressList, "addressList can't be null!");
        CommonUtils.requireNonNull(appName, "appName can't be null");

        this.requestService = new ClusterRequestServiceOkHttp3Impl(config);

        AppAuthRequest appAuthRequest = new AppAuthRequest();
        appAuthRequest.setAppName(appName);
        appAuthRequest.setEncryptedPassword(DigestUtils.md5(config.getPassword()));
        appAuthRequest.setEncryptType(EncryptType.MD5.getCode());

        String assertResponse = requestService.request(OpenAPIConstant.AUTH_APP, PowerRequestBody.newJsonRequestBody(appAuthRequest));

        if (StringUtils.isNotEmpty(assertResponse)) {
            ResultDTO<AppAuthResult> resultDTO = JSON.parseObject(assertResponse, APP_AUTH_RESULT_TYPE);
            if (resultDTO.isSuccess()) {
                appId = resultDTO.getData().getAppId();
            } else {
                throw new PowerJobException(resultDTO.getMessage());
            }
        }
        
        if (appId == null) {
            throw new PowerJobException("appId is null, please check your config");
        }
        
        log.info("[PowerJobClient] [INIT] {}'s PowerJobClient bootstrap successfully", appName);
    }
    /**
     * Init PowerJobClient with domain, appName and password.
     *
     * @param domain   like powerjob-server.apple-inc.com (Intranet Domain)
     * @param appName  name of the application
     * @param password password of the application
     */
    public PowerJobClient(String domain, String appName, String password) {
        this(new ClientConfig().setAppName(appName).setPassword(password).setAddressList(Lists.newArrayList(domain)));
    }


    /**
     * Init PowerJobClient with server address, appName and password.
     *
     * @param addressList IP:Port address list, like 192.168.1.1:7700
     * @param appName     name of the application
     * @param password    password of the application
     */
    public PowerJobClient(List<String> addressList, String appName, String password) {
        this(new ClientConfig().setAppName(appName).setPassword(password).setAddressList(addressList));
    }

    /* ************* Job 区 ************* */

    /**
     * Save one Job
     * When an ID exists in SaveJobInfoRequest, it is an update operation. Otherwise, it is a crate operation.
     *
     * @param request Job meta info
     * @return jobId
     */
    @Override
    public ResultDTO<Long> saveJob(SaveJobInfoRequest request) {

        request.setAppId(appId);
        String post = requestService.request(OpenAPIConstant.SAVE_JOB, PowerRequestBody.newJsonRequestBody(request));
        return JSON.parseObject(post, LONG_RESULT_TYPE);
    }


    /**
     * Copy one Job
     *
     * @param jobId Job id
     * @return Id of job copy
     */
    @Override
    public ResultDTO<Long> copyJob(Long jobId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("jobId", jobId.toString());
        param.put("appId", appId.toString());

        String post = requestService.request(OpenAPIConstant.COPY_JOB, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, LONG_RESULT_TYPE);
    }

    @Override
    public ResultDTO<SaveJobInfoRequest> exportJob(Long jobId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("jobId", jobId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.EXPORT_JOB, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, SAVE_JOB_INFO_REQUEST_RESULT_TYPE);
    }

    /**
     * Query JobInfo by jobId
     *
     * @param jobId jobId
     * @return Job meta info
     */
    @Override
    public ResultDTO<JobInfoDTO> fetchJob(Long jobId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("jobId", jobId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.FETCH_JOB, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, JOB_RESULT_TYPE);
    }

    /**
     * Query all JobInfo
     *
     * @return All JobInfo
     */
    @Override
    public ResultDTO<List<JobInfoDTO>> fetchAllJob() {
        Map<String, String> param = Maps.newHashMap();
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.FETCH_ALL_JOB, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, LIST_JOB_RESULT_TYPE);
    }

    /**
     * Query JobInfo by PowerQuery
     *
     * @param powerQuery JobQuery
     * @return JobInfo
     */
    @Override
    public ResultDTO<List<JobInfoDTO>> queryJob(JobInfoQuery powerQuery) {
        powerQuery.setAppIdEq(appId);
        String post = requestService.request(OpenAPIConstant.QUERY_JOB, PowerRequestBody.newJsonRequestBody(powerQuery));
        return JSON.parseObject(post, LIST_JOB_RESULT_TYPE);
    }

    /**
     * Disable one Job by jobId
     *
     * @param jobId jobId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> disableJob(Long jobId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("jobId", jobId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.DISABLE_JOB, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Enable one job by jobId
     *
     * @param jobId jobId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> enableJob(Long jobId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("jobId", jobId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.ENABLE_JOB, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Delete one job by jobId
     *
     * @param jobId jobId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> deleteJob(Long jobId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("jobId", jobId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.DELETE_JOB, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Run a job once
     *
     * @param jobId          ID of the job to be run
     * @param instanceParams Runtime parameters of the job (TaskContext#instanceParams)
     * @param delayMS        Delay time（Milliseconds）
     * @return instanceId
     */
    @Override
    public ResultDTO<Long> runJob(Long jobId, String instanceParams, long delayMS) {

        Map<String, String> param = Maps.newHashMap();
        param.put("jobId", jobId.toString());
        param.put("appId", appId.toString());
        param.put("delay", String.valueOf(delayMS));

        if (StringUtils.isNotEmpty(instanceParams)) {
            param.put("instanceParams", instanceParams);
        }
        String post = requestService.request(OpenAPIConstant.RUN_JOB, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, LONG_RESULT_TYPE);
    }

    public ResultDTO<Long> runJob(Long jobId) {
        return runJob(jobId, null, 0);
    }

    /* ************* Instance API list ************* */

    /**
     * Stop one job instance
     *
     * @param instanceId instanceId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> stopInstance(Long instanceId) {

        Map<String, String> param = Maps.newHashMap();
        param.put("instanceId", instanceId.toString());
        param.put("appId", appId.toString());

        String post = requestService.request(OpenAPIConstant.STOP_INSTANCE, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Cancel a job instance that is not yet running
     * Notice：There is a time interval between the call interface time and the expected execution time of the job instance to be cancelled, otherwise reliability is not guaranteed
     *
     * @param instanceId instanceId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> cancelInstance(Long instanceId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("instanceId", instanceId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.CANCEL_INSTANCE, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Retry failed job instance
     * Notice: Only job instance with completion status (success, failure, manually stopped, cancelled) can be retried, and retries of job instances within workflows are not supported yet.
     *
     * @param instanceId instanceId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> retryInstance(Long instanceId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("instanceId", instanceId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.RETRY_INSTANCE, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Query status about a job instance
     *
     * @param instanceId instanceId
     * @return {@link InstanceStatus}
     */
    @Override
    public ResultDTO<Integer> fetchInstanceStatus(Long instanceId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("instanceId", instanceId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.FETCH_INSTANCE_STATUS, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, INTEGER_RESULT_TYPE);
    }

    /**
     * Query detail about a job instance
     *
     * @param instanceId instanceId
     * @return instance detail
     */
    @Override
    public ResultDTO<InstanceInfoDTO> fetchInstanceInfo(Long instanceId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("instanceId", instanceId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.FETCH_INSTANCE_INFO, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, INSTANCE_RESULT_TYPE);
    }

    @Override
    public ResultDTO<PageResult<InstanceInfoDTO>> queryInstanceInfo(InstancePageQuery instancePageQuery) {
        instancePageQuery.setAppIdEq(appId);
        String post = requestService.request(OpenAPIConstant.QUERY_INSTANCE, PowerRequestBody.newJsonRequestBody(instancePageQuery));
        return JSON.parseObject(post, PAGE_INSTANCE_RESULT_TYPE);
    }

    /* ************* Workflow API list ************* */

    /**
     * Save one workflow
     * When an ID exists in SaveWorkflowRequest, it is an update operation. Otherwise, it is a crate operation.
     *
     * @param request Workflow meta info
     * @return workflowId
     */
    @Override
    public ResultDTO<Long> saveWorkflow(SaveWorkflowRequest request) {
        request.setAppId(appId);
        // 中坑记录：用 FastJSON 序列化会导致 Server 接收时 pEWorkflowDAG 为 null，无语.jpg
        String json = JsonUtils.toJSONStringUnsafe(request);
        String post = requestService.request(OpenAPIConstant.SAVE_WORKFLOW, PowerRequestBody.newJsonRequestBody(json));
        return JSON.parseObject(post, LONG_RESULT_TYPE);
    }

    /**
     * Copy one workflow
     *
     * @param workflowId Workflow id
     * @return Id of workflow copy
     */
    @Override
    public ResultDTO<Long> copyWorkflow(Long workflowId) {

        Map<String, String> param = Maps.newHashMap();
        param.put("workflowId", workflowId.toString());
        param.put("appId", appId.toString());

        String post = requestService.request(OpenAPIConstant.COPY_WORKFLOW, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, LONG_RESULT_TYPE);
    }


    /**
     * 添加工作流节点
     *
     * @param requestList Node info list of Workflow
     * @return Standard return object
     */
    @Override
    public ResultDTO<List<WorkflowNodeInfoDTO>> saveWorkflowNode(List<SaveWorkflowNodeRequest> requestList) {
        for (SaveWorkflowNodeRequest saveWorkflowNodeRequest : requestList) {
            saveWorkflowNodeRequest.setAppId(appId);
        }

        String json = JsonUtils.toJSONStringUnsafe(requestList);
        String post = requestService.request(OpenAPIConstant.SAVE_WORKFLOW_NODE, PowerRequestBody.newJsonRequestBody(json));
        return JSON.parseObject(post, WF_NODE_LIST_RESULT_TYPE);
    }



    /**
     * Query Workflow by workflowId
     *
     * @param workflowId workflowId
     * @return Workflow meta info
     */
    @Override
    public ResultDTO<WorkflowInfoDTO> fetchWorkflow(Long workflowId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("workflowId", workflowId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.FETCH_WORKFLOW, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, WF_RESULT_TYPE);
    }

    /**
     * Disable Workflow by workflowId
     *
     * @param workflowId workflowId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> disableWorkflow(Long workflowId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("workflowId", workflowId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.DISABLE_WORKFLOW, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Enable Workflow by workflowId
     *
     * @param workflowId workflowId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> enableWorkflow(Long workflowId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("workflowId", workflowId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.ENABLE_WORKFLOW, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Delete Workflow by workflowId
     *
     * @param workflowId workflowId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> deleteWorkflow(Long workflowId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("workflowId", workflowId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.DELETE_WORKFLOW, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Run a workflow once
     *
     * @param workflowId workflowId
     * @param initParams workflow startup parameters
     * @param delayMS    Delay time（Milliseconds）
     * @return workflow instanceId
     */
    @Override
    public ResultDTO<Long> runWorkflow(Long workflowId, String initParams, long delayMS) {

        Map<String, String> param = Maps.newHashMap();
        param.put("workflowId", workflowId.toString());
        param.put("appId", appId.toString());
        param.put("delay", String.valueOf(delayMS));


        if (StringUtils.isNotEmpty(initParams)) {
            param.put("initParams", initParams);
        }
        String post = requestService.request(OpenAPIConstant.RUN_WORKFLOW, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, LONG_RESULT_TYPE);
    }

    public ResultDTO<Long> runWorkflow(Long workflowId) {
        return runWorkflow(workflowId, null, 0);
    }

    /* ************* Workflow Instance API list ************* */

    /**
     * Stop one workflow instance
     *
     * @param wfInstanceId workflow instanceId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> stopWorkflowInstance(Long wfInstanceId) {

        Map<String, String> param = Maps.newHashMap();
        param.put("wfInstanceId", wfInstanceId.toString());
        param.put("appId", appId.toString());

        String post = requestService.request(OpenAPIConstant.STOP_WORKFLOW_INSTANCE, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Retry one workflow instance
     *
     * @param wfInstanceId workflow instanceId
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> retryWorkflowInstance(Long wfInstanceId) {
        Map<String, String> param = Maps.newHashMap();
        param.put("wfInstanceId", wfInstanceId.toString());
        param.put("appId", appId.toString());
        String post = requestService.request(OpenAPIConstant.RETRY_WORKFLOW_INSTANCE, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * mark the workflow node as success
     *
     * @param wfInstanceId workflow instanceId
     * @param nodeId       node id
     * @return Standard return object
     */
    @Override
    public ResultDTO<Void> markWorkflowNodeAsSuccess(Long wfInstanceId, Long nodeId) {

        Map<String, String> param = Maps.newHashMap();
        param.put("wfInstanceId", wfInstanceId.toString());
        param.put("appId", appId.toString());
        param.put("nodeId", nodeId.toString());

        String post = requestService.request(OpenAPIConstant.MARK_WORKFLOW_NODE_AS_SUCCESS, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, VOID_RESULT_TYPE);
    }

    /**
     * Query detail about a workflow instance
     *
     * @param wfInstanceId workflow instanceId
     * @return detail about a workflow
     */
    @Override
    public ResultDTO<WorkflowInstanceInfoDTO> fetchWorkflowInstanceInfo(Long wfInstanceId) {

        Map<String, String> param = Maps.newHashMap();
        param.put("wfInstanceId", wfInstanceId.toString());
        param.put("appId", appId.toString());

        String post = requestService.request(OpenAPIConstant.FETCH_WORKFLOW_INSTANCE_INFO, PowerRequestBody.newFormRequestBody(param));
        return JSON.parseObject(post, WF_INSTANCE_RESULT_TYPE);
    }

    @Override
    public void close() throws IOException {
        requestService.close();
    }
}
