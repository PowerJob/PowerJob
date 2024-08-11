package tech.powerjob.samples.tester;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.client.ClientConfig;
import tech.powerjob.client.IPowerJobClient;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.official.processors.util.CommonUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 测试 OpenAPI
 *
 * @author tjq
 * @since 2024/8/11
 */
@Slf4j
@Component
public class OpenApiTester implements BasicProcessor {

    private final Map<String, IPowerJobClient> clientCache = Maps.newHashMap();

    private static final String NEW_JOB_PARAMS = "{'aa':'bb'}";
    private static final String RUN_INSTANCE_PARAMS = "{'key':'value'}";

    private static final int NEW_JOB_MAX_INSTANCE_NUM = 9;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        IPowerJobClient client = fetchClient(context);

        SaveJobInfoRequest saveJobInfoRequest = buildSaveJobInfoRequest();

        context.getOmsLogger().info("[newJob] saveJobInfoRequest: {}", JSONObject.toJSONString(saveJobInfoRequest));
        ResultDTO<Long> saveJobResult = client.saveJob(saveJobInfoRequest);
        context.getOmsLogger().info("[newJob] RESPONSE: {}", JSONObject.toJSONString(saveJobResult));
        Long createdJobId = fetchResultData(saveJobResult);


        // 测试导出
        ResultDTO<SaveJobInfoRequest> exportJobResult = client.exportJob(createdJobId);
        context.getOmsLogger().info("[exportJob] exportJobResult: {}", JSONObject.toJSONString(exportJobResult));
        SaveJobInfoRequest exportJobInfo = fetchResultData(exportJobResult);
        assert exportJobInfo.getJobParams().equals(saveJobInfoRequest.getJobParams());
        assert exportJobInfo.getMaxInstanceNum().equals(saveJobInfoRequest.getMaxInstanceNum());

        // 测试复制
        context.getOmsLogger().info("[copyJob] sourceJobId: {}", createdJobId);
        ResultDTO<Long> copyJobResult = client.copyJob(createdJobId);
        context.getOmsLogger().info("[copyJob] copyJobResult: {}", JSONObject.toJSONString(copyJobResult));

        Long copiedJobId = fetchResultData(copyJobResult);

        context.getOmsLogger().info("[disableJob] targetJobId: {}", copiedJobId);
        ResultDTO<Void> disableJobResult = client.disableJob(copiedJobId);
        fetchResultData(disableJobResult);
        context.getOmsLogger().info("[disableJob] disableJobResult: {}", disableJobResult);

        ResultDTO<JobInfoDTO> createdJobInfoResult = client.fetchJob(createdJobId);
        context.getOmsLogger().info("[fetchJob] createdJobInfo: {}", JSONObject.toJSONString(createdJobInfoResult));
        ResultDTO<JobInfoDTO> copiedJobInfoResult = client.fetchJob(copiedJobId);
        context.getOmsLogger().info("[fetchJob] copiedJobInfo: {}", JSONObject.toJSONString(copiedJobInfoResult));

        JobInfoDTO createdJob = fetchResultData(createdJobInfoResult);
        JobInfoDTO copiedJob = fetchResultData(copiedJobInfoResult);

        assert createdJob.getJobParams().equals(copiedJob.getJobParams());
        assert createdJob.getMaxInstanceNum().equals(copiedJob.getMaxInstanceNum());
        assert copiedJob.getStatus() == SwitchableStatus.DISABLE.getV();

        ResultDTO<Void> enableJobResult = client.enableJob(copiedJob.getId());
        fetchResultData(enableJobResult);
        context.getOmsLogger().info("[enableJob] enableJobResult: {}", JSONObject.toJSONString(enableJobResult));

        // 再次查询验证 enable
        ResultDTO<JobInfoDTO> copiedJobInfoResult2 = client.fetchJob(copiedJobId);
        context.getOmsLogger().info("[fetchJob] copiedJobInfoResult2: {}", JSONObject.toJSONString(copiedJobInfoResult2));
        JobInfoDTO copiedJob2 = fetchResultData(copiedJobInfoResult2);
        assert copiedJob2.getStatus() == SwitchableStatus.ENABLE.getV();

        // 删除拷贝出来的任务
        ResultDTO<Void> deleteJobResult = client.deleteJob(copiedJobId);
        context.getOmsLogger().info("[deleteJob] deleteJobResult: {}", JSONObject.toJSONString(deleteJobResult));
        fetchResultData(deleteJobResult);

        // 执行任务
        ResultDTO<Long> runJobResult = client.runJob(createdJobId, RUN_INSTANCE_PARAMS, 0);
        context.getOmsLogger().info("[runJob] runJobResult: {}", JSONObject.toJSONString(runJobResult));
        Long instanceId = fetchResultData(runJobResult);

        // 等10S，理论上应该能执行完成
        Thread.sleep(10000);

        // 查询任务详情和状态
        ResultDTO<InstanceInfoDTO> fetchInstanceInfoResult = client.fetchInstanceInfo(instanceId);
        context.getOmsLogger().info("[fetchInstanceInfo] fetchInstanceInfoResult: {}", JSONObject.toJSONString(fetchInstanceInfoResult));
        InstanceInfoDTO instanceInfoDTO = fetchResultData(fetchInstanceInfoResult);

        ResultDTO<Integer> fetchInstanceStatusResult = client.fetchInstanceStatus(instanceId);
        context.getOmsLogger().info("[fetchInstanceStatus] fetchInstanceStatusResult: {}", JSONObject.toJSONString(fetchInstanceStatusResult));
        Integer instanceStatus = fetchResultData(fetchInstanceStatusResult);
        assert instanceInfoDTO.getStatus() == instanceStatus;

        // 回收全部资源
        ResultDTO<Void> deleteCreatedJobResult = client.deleteJob(createdJobId);
        context.getOmsLogger().info("[deleteJob] deleteCreatedJobResult: {}", JSONObject.toJSONString(deleteCreatedJobResult));
        fetchResultData(deleteCreatedJobResult);

        return new ProcessResult(true);
    }

    private static <T> T fetchResultData(ResultDTO<T> resultDTO) {
        if (resultDTO.isSuccess()) {
            return resultDTO.getData();
        }
        throw new RuntimeException(resultDTO.getMessage());
    }

    private SaveJobInfoRequest buildSaveJobInfoRequest() {

        SaveJobInfoRequest newJobInfo = new SaveJobInfoRequest();

        newJobInfo.setJobName("JobCreateByOpenAPI");
        newJobInfo.setJobDescription("Timestamp: " + System.currentTimeMillis());
        newJobInfo.setJobParams(NEW_JOB_PARAMS);
        newJobInfo.setTimeExpressionType(TimeExpressionType.API);
        newJobInfo.setExecuteType(ExecuteType.STANDALONE);
        newJobInfo.setProcessorType(ProcessorType.BUILT_IN);
        newJobInfo.setProcessorInfo("tech.powerjob.samples.processors.StandaloneProcessorDemo");

        newJobInfo.setMaxInstanceNum(NEW_JOB_MAX_INSTANCE_NUM);

        newJobInfo.setMinCpuCores(0.01);
        newJobInfo.setMinMemorySpace(0.02);
        newJobInfo.setMinDiskSpace(0.03);

        return newJobInfo;
    }

    private IPowerJobClient fetchClient(TaskContext context) {
        String params = CommonUtils.parseParams(context);
        Config clientConfig = Optional.ofNullable(params).map(x -> JSONObject.parseObject(params, Config.class)).orElse(new Config());

        String appName = Optional.ofNullable(clientConfig.getAppName()).orElse("powerjob-worker-samples");
        String password = Optional.ofNullable(clientConfig.getPassword()).orElse("powerjob123");
        List<String> addressList = Optional.ofNullable(clientConfig.getAddressList()).orElse(Lists.newArrayList("127.0.0.1:7700", "127.0.0.1:7701"));

        String key = String.format("client_%s_%s_%s", appName, password, addressList);

        return clientCache.computeIfAbsent(key, ignore -> initPowerJobClient(appName, password, addressList));
    }

    private IPowerJobClient initPowerJobClient(String appName, String password, List<String> addressList) {

        ClientConfig config = new ClientConfig();
        config.setAppName(appName);
        config.setPassword(password);
        config.setAddressList(addressList);

        log.info("[OpenApiTester] initPowerJobClient, config: {}", config);

        return new PowerJobClient(config);
    }


    @Data
    public static class Config implements Serializable {
        private String appName;

        private String password;

        private List<String> addressList;
    }
}
