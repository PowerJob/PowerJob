package com.github.kfcfans.oms.client;

import com.github.kfcfans.oms.common.InstanceStatus;
import com.github.kfcfans.oms.common.OpenAPIConstant;
import com.github.kfcfans.oms.common.response.ResultDTO;
import com.github.kfcfans.oms.common.utils.HttpUtils;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * OpenAPI 客户端
 * V1.0.0 摒弃一切优雅设计，先实现再说...
 *
 * @author tjq
 * @since 2020/4/15
 */
@Slf4j
@SuppressWarnings("rawtypes, unchecked")
public class OhMyClient {

    private String domain;
    private Long appId;

    private static final String URL_PATTERN = "http://%s%s%s";

    /**
     * 初始化 OhMyClient 客户端
     * @param domain 服务器地址，eg:192.168.1.1:7700（选定主机，无HA保证） / www.oms-server.com（内网域名，自行完成DNS & Proxy）
     * @param appName 负责的应用名称
     */
    public OhMyClient(String domain, String appName) throws Exception {

        Objects.requireNonNull(domain, "domain can't be null!");
        Objects.requireNonNull(appName, "appName can't be null");

        this.domain = domain;

        // 验证 appName 可用性 & server可用性
        String url = getUrl(OpenAPIConstant.ASSERT) + "?appName=" + appName;
        String result = HttpUtils.get(url);
        if (StringUtils.isNotEmpty(result)) {
            ResultDTO resultDTO = JsonUtils.parseObject(result, ResultDTO.class);
            if (resultDTO.isSuccess()) {
                appId = Long.parseLong(resultDTO.getData().toString());
            }else {
                throw new RuntimeException(resultDTO.getMessage());
            }
        }
        log.info("[OhMyClient] {}'s client bootstrap successfully.", appName);
    }


    private String getUrl(String path) {
        return String.format(URL_PATTERN, domain, OpenAPIConstant.WEB_PATH, path);
    }

    /* ************* Job 区 ************* */
    /**
     * 禁用某个任务
     * @param jobId 任务ID
     * @return 标准返回对象
     * @throws Exception 异常
     */
    public ResultDTO<Void> disableJob(Long jobId) throws Exception {
        String url = getUrl(OpenAPIConstant.DISABLE_JOB);
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = HttpUtils.post(url, body);
        return JsonUtils.parseObject(post, ResultDTO.class);
    }

    /**
     * 删除某个任务
     * @param jobId 任务ID
     * @return 标准返回对象
     * @throws Exception 异常
     */
    public ResultDTO<Void> deleteJob(Long jobId) throws Exception {
        String url = getUrl(OpenAPIConstant.DELETE_JOB);
        RequestBody body = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString())
                .build();
        String post = HttpUtils.post(url, body);
        return JsonUtils.parseObject(post, ResultDTO.class);
    }

    /**
     * 运行某个任务
     * @param jobId 任务ID
     * @param instanceParams 任务实例的参数
     * @return 任务实例ID（instanceId）
     * @throws Exception 异常
     */
    public ResultDTO<Long> runJob(Long jobId, String instanceParams) throws Exception {
        String url = getUrl(OpenAPIConstant.RUN_JOB);
        final FormBody.Builder builder = new FormBody.Builder()
                .add("jobId", jobId.toString())
                .add("appId", appId.toString());

        if (StringUtils.isNotEmpty(instanceParams)) {
            builder.add("instanceParams", instanceParams);
        }
        String post = HttpUtils.post(url, builder.build());
        return JsonUtils.parseObject(post, ResultDTO.class);
    }
    public ResultDTO<Long> runJob(Long jobId) throws Exception {
        return runJob(jobId, null);
    }

    /* ************* Instance 区 ************* */
    /**
     * 停止应用实例
     * @param instanceId 应用实例ID
     * @return true -> 停止成功，false -> 停止失败
     * @throws Exception 异常
     */
    public ResultDTO<Void> stopInstance(Long instanceId) throws Exception {
        String url = getUrl(OpenAPIConstant.STOP_INSTANCE);
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .add("appId", appId.toString())
                .build();
        String post = HttpUtils.post(url, body);
        return JsonUtils.parseObject(post, ResultDTO.class);
    }

    /**
     * 查询应用实例状态
     * @param instanceId 应用实例ID
     * @return {@link InstanceStatus} 的枚举值
     * @throws Exception 异常
     */
    public ResultDTO<Integer> fetchInstanceStatus(Long instanceId) throws Exception {
        String url = getUrl(OpenAPIConstant.FETCH_INSTANCE_STATUS);
        RequestBody body = new FormBody.Builder()
                .add("instanceId", instanceId.toString())
                .build();
        String post = HttpUtils.post(url, body);
        return JsonUtils.parseObject(post, ResultDTO.class);
    }
}
