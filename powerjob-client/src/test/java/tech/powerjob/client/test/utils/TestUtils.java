package tech.powerjob.client.test.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.http.SaveJobInfoRequest;

/**
 * TestUtils
 *
 * @author tjq
 * @since 2024/8/19
 */
@Slf4j
public class TestUtils {

    public static SaveJobInfoRequest newSaveJobRequest(Long jobId, String jobName) {
        SaveJobInfoRequest newJobInfo = new SaveJobInfoRequest();
        newJobInfo.setId(jobId);
        String jobNameF = StringUtils.isEmpty(jobName) ? "OpenApiJob-" + System.currentTimeMillis() : jobName;
        newJobInfo.setJobName(jobNameF);
        newJobInfo.setJobDescription("test OpenAPI" + System.currentTimeMillis());
        newJobInfo.setJobParams("{'aa':'bb'}");
        newJobInfo.setTimeExpressionType(TimeExpressionType.CRON);
        newJobInfo.setTimeExpression("0 0 * * * ? ");
        newJobInfo.setExecuteType(ExecuteType.STANDALONE);
        newJobInfo.setProcessorType(ProcessorType.BUILT_IN);
        newJobInfo.setProcessorInfo("tech.powerjob.samples.processors.StandaloneProcessorDemo");
        newJobInfo.setDesignatedWorkers("");

        newJobInfo.setMinCpuCores(1.1);
        newJobInfo.setMinMemorySpace(1.2);
        newJobInfo.setMinDiskSpace(1.3);

        log.info("[TestClient] [testSaveJob] SaveJobInfoRequest: {}", JSONObject.toJSONString(newJobInfo));

        return newJobInfo;
    }
}
