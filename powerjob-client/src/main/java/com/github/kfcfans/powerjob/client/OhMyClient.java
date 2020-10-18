package com.github.kfcfans.powerjob.client;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.OpenAPIConstant;
import com.github.kfcfans.powerjob.common.request.http.SaveJobInfoRequest;
import com.github.kfcfans.powerjob.common.request.http.SaveWorkflowRequest;
import com.github.kfcfans.powerjob.common.response.*;
import com.github.kfcfans.powerjob.common.utils.CommonUtils;
import com.github.kfcfans.powerjob.common.utils.HttpUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * OpenAPI 客户端
 *
 * @author tjq
 * @since 2020/4/15
 */
@Slf4j
@SuppressWarnings("rawtypes, unchecked")
public class OhMyClient {

    private Long appId;
    private String currentAddress;
    private final List<String> allAddress;

    private static final String URL_PATTERN = "http://%s%s%s";

    /**
     * 初始化 OhMyClient 客户端
     * @param domain 比如 www.powerjob-server.com（内网域名，自行完成 DNS & Proxy）
     * @param appName 负责的应用名称
     */
    public OhMyClient(String domain, String appName, String password) {
        this(Lists.newArrayList(domain), appName, password);
    }


    /**
     * 初始化 OhMyClient 客户端
     * @param addressList IP:Port 列表
     * @param appName 负责的应用名称
     */
    public OhMyClient(List<String> addressList, String appName, String password) {

        CommonUtils.requireNonNull(addressList, "domain can't be null!");
        CommonUtils.requireNonNull(appName, "appName can't be null");

        allAddress = addressList;
        for (String addr : addressList) {
            String url = getUrl(OpenAPIConstant.ASSERT, addr);
            try {
                String result = assertApp(appName, password, url);
                if (StringUtils.isNotEmpty(result)) {
                    ResultDTO resultDTO = JSONObject.parseObject(result, ResultDTO.class);
                    if (resultDTO.isSuccess()) {
                        appId = Long.parseLong(resultDTO.getData().toString());
                        currentAddress = addr;
                        break;
                    }else {
                        throw new PowerJobException(resultDTO.getMessage());
                    }
                }
            }catch (IOException ignore) {
            }
        }

        if (StringUtils.isEmpty(currentAddress)) {
            throw new PowerJobException("no server available");
        }
        log.info("[OhMyClient] {}'s oms-client bootstrap successfully, using server: {}", appName, currentAddress);
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
     * 保存任务（包括创建与修改）
     * @param request 任务详细参数
     * @return 创建的任务ID
     * @throws Exception 异常
     */
    public ResultDTO<Long> saveJob(SaveJobInfoRequest request) throws Exception {

        request.setAppId(appId);
        MediaType jsonType = MediaType.parse("application/json; charset=utf-8");
        String json = JSONObject.toJSONString(request);
        String post = postHA(OpenAPIConstant.SAVE_JOB, RequestBody.create(jsonType, json));
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 根据 jobId 查询任务信息
     * @param jobId 任务ID
     * @return 任务详细信息
     * @throws Exception 异常
     */
    public ResultDTO<JobInfoDTO> fetchJob(Long jobId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_JOB, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 禁用某个任务
     * @param jobId 任务ID
     * @return 标准返回对象
     * @throws Exception 异常
     */
    public ResultDTO<Void> disableJob(Long jobId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.DISABLE_JOB, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 启用某个任务
     * @param jobId 任务ID
     * @return 标准返回对象
     * @throws Exception 异常
     */
    public ResultDTO<Void> enableJob(Long jobId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.ENABLE_JOB, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 删除某个任务
     * @param jobId 任务ID
     * @return 标准返回对象
     * @throws Exception 异常
     */
    public ResultDTO<Void> deleteJob(Long jobId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.DELETE_JOB, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 运行某个任务
     * @param jobId 任务ID
     * @param instanceParams 任务实例的参数
     * @param delayMS 延迟时间，单位毫秒
     * @return 任务实例ID（instanceId）
     * @throws Exception 异常
     */
    public ResultDTO<Long> runJob(Long jobId, String instanceParams, long delayMS) throws Exception {
        FormBody.Builder builder = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .add("delay", String.valueOf(delayMS));

        if (StringUtils.isNotEmpty(instanceParams)) {
            builder.add("instanceParams", instanceParams);
        }
        String post = postHA(OpenAPIConstant.RUN_JOB, builder.build());
        return JSONObject.parseObject(post, ResultDTO.class);
    }
    public ResultDTO<Long> runJob(Long jobId) throws Exception {
        return runJob(jobId, null, 0);
    }

    /* ************* Instance 区 ************* */
    /**
     * 停止应用实例
     * @param instanceId 应用实例ID
     * @return true 停止成功，false 停止失败
     * @throws Exception 异常
     */
    public ResultDTO<Void> stopInstance(Long instanceId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.STOP_INSTANCE, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 取消任务实例
     * 接口使用条件：调用接口时间与待取消任务的预计执行时间有一定时间间隔，否则不保证可靠性！
     * @param instanceId 任务实例ID
     * @return true 代表取消成功，false 取消失败
     * @throws Exception 异常
     */
    public ResultDTO<Void> cancelInstance(Long instanceId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.CANCEL_INSTANCE, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 重试任务实例
     * 只有完成状态（成功、失败、手动停止、被取消）的任务才能被重试，且暂不支持工作流内任务实例的重试
     * @param instanceId 任务实例ID
     * @return true 代表取消成功，false 取消失败
     * @throws Exception 异常
     */
    public ResultDTO<Void> retryInstance(Long instanceId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.RETRY_INSTANCE, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 查询任务实例状态
     * @param instanceId 应用实例ID
     * @return {@link InstanceStatus} 的枚举值
     * @throws Exception 异常
     */
    public ResultDTO<Integer> fetchInstanceStatus(Long instanceId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_INSTANCE_STATUS, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 查询任务实例的信息
     * @param instanceId 任务实例ID
     * @return 任务实例信息
     * @throws Exception 潜在的异常
     */
    public ResultDTO<InstanceInfoDTO> fetchInstanceInfo(Long instanceId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_INSTANCE_INFO, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /* ************* Workflow 区 ************* */
    /**
     * 保存工作流（包括创建和修改）
     * @param request 创建/修改 Workflow 请求
     * @return 工作流ID
     * @throws Exception 异常
     */
    public ResultDTO<Long> saveWorkflow(SaveWorkflowRequest request) throws Exception {
        request.setAppId(appId);
        MediaType jsonType = MediaType.parse("application/json; charset=utf-8");
        String json = JSONObject.toJSONString(request);
        String post = postHA(OpenAPIConstant.SAVE_WORKFLOW, RequestBody.create(jsonType, json));
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 根据 workflowId 查询工作流信息
     * @param workflowId workflowId
     * @return 工作流信息
     * @throws Exception 异常
     */
    public ResultDTO<WorkflowInfoDTO> fetchWorkflow(Long workflowId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_WORKFLOW, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 禁用某个工作流
     * @param workflowId 工作流ID
     * @return 标准返回对象
     * @throws Exception 异常
     */
    public ResultDTO<Void> disableWorkflow(Long workflowId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.DISABLE_WORKFLOW, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 启用某个工作流
     * @param workflowId workflowId
     * @return 标准返回对象
     * @throws Exception 异常
     */
    public ResultDTO<Void> enableWorkflow(Long workflowId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.ENABLE_WORKFLOW, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 删除某个工作流
     * @param workflowId workflowId
     * @return 标准返回对象
     * @throws Exception 异常
     */
    public ResultDTO<Void> deleteWorkflow(Long workflowId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.DELETE_WORKFLOW, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 运行工作流
     * @param workflowId 工作流ID
     * @param initParams 启动参数
     * @param delayMS 延迟时间，单位毫秒 ms
     * @return 工作流实例ID
     * @throws Exception 异常信息
     */
    public ResultDTO<Long> runWorkflow(Long workflowId, String initParams, long delayMS) throws Exception {
        FormBody.Builder builder = new FormBody.Builder()
                .add("workflowId", workflowId.toString())
                .add("appId", appId.toString())
                .add("delay", String.valueOf(delayMS));
        if (StringUtils.isNotEmpty(initParams)) {
            builder.add("initParams", initParams);
        }
        String post = postHA(OpenAPIConstant.RUN_WORKFLOW, builder.build());
        return JSONObject.parseObject(post, ResultDTO.class);
    }
    public ResultDTO<Long> runWorkflow(Long workflowId) throws Exception {
        return runWorkflow(workflowId, null, 0);
    }

    /* ************* Workflow Instance 区 ************* */
    /**
     * 停止应用实例
     * @param wfInstanceId 工作流实例ID
     * @return true 停止成功 ； false 停止失败
     * @throws Exception 异常
     */
    public ResultDTO<Void> stopWorkflowInstance(Long wfInstanceId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("wfInstanceId", wfInstanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.STOP_WORKFLOW_INSTANCE, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }

    /**
     * 查询任务实例的信息
     * @param wfInstanceId 任务实例ID
     * @return 任务实例信息
     * @throws Exception 潜在的异常
     */
    public ResultDTO<WorkflowInstanceInfoDTO> fetchWorkflowInstanceInfo(Long wfInstanceId) throws Exception {
        RequestBody body = new FormBody.Builder()
                .add("wfInstanceId", wfInstanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = postHA(OpenAPIConstant.FETCH_WORKFLOW_INSTANCE_INFO, body);
        return JSONObject.parseObject(post, ResultDTO.class);
    }



    private String postHA(String path, RequestBody requestBody) {

        // 先尝试默认地址
        String url = getUrl(path, currentAddress);
        try {
            String res = HttpUtils.post(url, requestBody);
            if (StringUtils.isNotEmpty(res)) {
                return res;
            }
        }catch (Exception e) {
            log.warn("[OhMyClient] request url:{} failed, reason is {}.", url, e.toString());
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
                    log.warn("[OhMyClient] server change: from({}) -> to({}).", currentAddress, addr);
                    currentAddress = addr;
                    return res;
                }
            }catch (Exception e) {
                log.warn("[OhMyClient] request url:{} failed, reason is {}.", url, e.toString());
            }
        }

        log.error("[OhMyClient] do post for path: {} failed because of no server available in {}.", path, allAddress);
        throw new PowerJobException("no server available when send post");
    }
}
