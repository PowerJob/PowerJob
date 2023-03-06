package tech.powerjob.client;

import com.alibaba.fastjson.JSON;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.OpenAPIConstant;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.request.http.SaveWorkflowNodeRequest;
import tech.powerjob.common.request.http.SaveWorkflowRequest;
import tech.powerjob.common.request.query.JobInfoQuery;
import tech.powerjob.common.response.*;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.HttpUtils;
import tech.powerjob.common.serialize.JsonUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static tech.powerjob.client.TypeStore.*;

/**
 * PowerJobClient, the client for OpenAPI.
 *
 * @author tjq
 * @since 2020/4/15
 */
@Slf4j
public class PowerJobClient implements IPowerJobClient {

    private Long appId;
    private String currentAddress;
    private final List<String> allAddress;

    private static final String URL_PATTERN = "http://%s%s%s";

    /**
     * Init PowerJobClient with domain, appName and password.
     *
     * @param domain   like powerjob-server.apple-inc.com (Intranet Domain)
     * @param appName  name of the application
     * @param password password of the application
     */
    public PowerJobClient(String domain, String appName, String password) {
        this(Lists.newArrayList(domain), appName, password);
    }


    /**
     * Init PowerJobClient with server address, appName and password.
     *
     * @param addressList IP:Port address list, like 192.168.1.1:7700
     * @param appName     name of the application
     * @param password    password of the application
     */
    public PowerJobClient(List<String> addressList, String appName, String password) {

        CommonUtils.requireNonNull(addressList, "addressList can't be null!");
        CommonUtils.requireNonNull(appName, "appName can't be null");

        allAddress = addressList;
        for (String addr : addressList) {
            String url = getUrl(OpenAPIConstant.ASSERT, addr);
            try {
                String result = assertApp(appName, password, url);
                if (StringUtils.isNotEmpty(result)) {
                    ResultDTO<Long> resultDTO = JSON.parseObject(result, LONG_RESULT_TYPE);
                    if (resultDTO.isSuccess()) {
                        appId = resultDTO.getData();
                        currentAddress = addr;
                        break;
                    } else {
                        throw new PowerJobException(resultDTO.getMessage());
                    }
                }
            } catch (IOException ignore) {
                //
            }
        }

        if (StringUtils.isEmpty(currentAddress)) {
            throw new PowerJobException("no server available for PowerJobClient");
        }
        log.info("[PowerJobClient] {}'s PowerJobClient bootstrap successfully, using server: {}", appName, currentAddress);
    }

    private static String assertApp(String appName, String password, String url) throws IOException {
        FormBody.Builder builder = new FormBody.Builder()
                .add("appName", appName);
        if (password != null) {
            builder.add("password", password);
        }
        return HttpUtils.post(url, builder.build());
    }


    private static String getUrl(String path, String address) {
        return String.format(URL_PATTERN, address, OpenAPIConstant.WEB_PATH, path);
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
        MediaType jsonType = MediaType.parse(OmsConstant.JSON_MEDIA_TYPE);
        String json = JSON.toJSONString(request);
        String post = postHA(OpenAPIConstant.SAVE_JOB, RequestBody.create(jsonType, json));
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
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.COPY_JOB, body);
        return JSON.parseObject(post, LONG_RESULT_TYPE);
    }

    @Override
    public ResultDTO<SaveJobInfoRequest> exportJob(Long jobId) {
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.EXPORT_JOB, body);
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
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_JOB, body);
        return JSON.parseObject(post, JOB_RESULT_TYPE);
    }

    /**
     * Query all JobInfo
     *
     * @return All JobInfo
     */
    @Override
    public ResultDTO<List<JobInfoDTO>> fetchAllJob() {
        RequestBody body = new FormBody.Builder()
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_ALL_JOB, body);
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
        MediaType jsonType = MediaType.parse(OmsConstant.JSON_MEDIA_TYPE);
        String json = JsonUtils.toJSONStringUnsafe(powerQuery);
        String post = postHA(OpenAPIConstant.QUERY_JOB, RequestBody.create(jsonType, json));
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
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.DISABLE_JOB, body);
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
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.ENABLE_JOB, body);
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
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.DELETE_JOB, body);
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
        FormBody.Builder builder = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .add("delay", String.valueOf(delayMS));

        if (StringUtils.isNotEmpty(instanceParams)) {
            builder.add("instanceParams", instanceParams);
        }
        String post = postHA(OpenAPIConstant.RUN_JOB, builder.build());
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
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.STOP_INSTANCE, body);
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
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.CANCEL_INSTANCE, body);
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
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.RETRY_INSTANCE, body);
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
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_INSTANCE_STATUS, body);
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
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_INSTANCE_INFO, body);
        return JSON.parseObject(post, INSTANCE_RESULT_TYPE);
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
        MediaType jsonType = MediaType.parse(OmsConstant.JSON_MEDIA_TYPE);
        // 中坑记录：用 FastJSON 序列化会导致 Server 接收时 pEWorkflowDAG 为 null，无语.jpg
        String json = JsonUtils.toJSONStringUnsafe(request);
        String post = postHA(OpenAPIConstant.SAVE_WORKFLOW, RequestBody.create(jsonType, json));
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
        RequestBody body = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.COPY_WORKFLOW, body);
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
        MediaType jsonType = MediaType.parse(OmsConstant.JSON_MEDIA_TYPE);
        String json = JsonUtils.toJSONStringUnsafe(requestList);
        String post = postHA(OpenAPIConstant.SAVE_WORKFLOW_NODE, RequestBody.create(jsonType, json));
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
        RequestBody body = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_WORKFLOW, body);
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
        RequestBody body = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.DISABLE_WORKFLOW, body);
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
        RequestBody body = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.ENABLE_WORKFLOW, body);
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
        RequestBody body = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.DELETE_WORKFLOW, body);
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
        FormBody.Builder builder = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .add("delay", String.valueOf(delayMS));
        if (StringUtils.isNotEmpty(initParams)) {
            builder.add("initParams", initParams);
        }
        String post = postHA(OpenAPIConstant.RUN_WORKFLOW, builder.build());
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
        RequestBody body = new FormBody.Builder()
                .add("wfInstanceId", wfInstanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.STOP_WORKFLOW_INSTANCE, body);
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
        RequestBody body = new FormBody.Builder()
                .add("wfInstanceId", wfInstanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.RETRY_WORKFLOW_INSTANCE, body);
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
        RequestBody body = new FormBody.Builder()
                .add("wfInstanceId", wfInstanceId.toString())
                .add("nodeId", nodeId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.MARK_WORKFLOW_NODE_AS_SUCCESS, body);
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
        RequestBody body = new FormBody.Builder()
                .add("wfInstanceId", wfInstanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_WORKFLOW_INSTANCE_INFO, body);
        return JSON.parseObject(post, WF_INSTANCE_RESULT_TYPE);
    }


    private String postHA(String path, RequestBody requestBody) {

        // 先尝试默认地址
        String url = getUrl(path, currentAddress);
        try {
            String res = HttpUtils.post(url, requestBody);
            if (StringUtils.isNotEmpty(res)) {
                return res;
            }
        } catch (IOException e) {
            log.warn("[PowerJobClient] request url:{} failed, reason is {}.", url, e.toString());
        }

        // 失败，开始重试
        for (String addr : allAddress) {
            if (Objects.equals(addr, currentAddress)) {
                continue;
            }
            url = getUrl(path, addr);
            try {
                String res = HttpUtils.post(url, requestBody);
                if (StringUtils.isNotEmpty(res)) {
                    log.warn("[PowerJobClient] server change: from({}) -> to({}).", currentAddress, addr);
                    currentAddress = addr;
                    return res;
                }
            } catch (IOException e) {
                log.warn("[PowerJobClient] request url:{} failed, reason is {}.", url, e.toString());
            }
        }

        log.error("[PowerJobClient] do post for path: {} failed because of no server available in {}.", path, allAddress);
        throw new PowerJobException("no server available when send post request");
    }
}
