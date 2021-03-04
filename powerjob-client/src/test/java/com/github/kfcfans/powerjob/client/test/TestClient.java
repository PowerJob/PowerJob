package com.github.kfcfans.powerjob.client.test;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.client.OhMyClient;
import com.github.kfcfans.powerjob.common.ExecuteType;
import com.github.kfcfans.powerjob.common.ProcessorType;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.request.http.SaveJobInfoRequest;
import com.github.kfcfans.powerjob.common.response.InstanceInfoDTO;
import com.github.kfcfans.powerjob.common.response.JobInfoDTO;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * Test cases for {@link OhMyClient}
 *
 * @author tjq
 * @author Echo009
 * @since 2020/4/15
 */
class TestClient extends ClientInitializer {

    public static final long JOB_ID = 4L;

    @Test
    void testSaveJob() {

        SaveJobInfoRequest newJobInfo = new SaveJobInfoRequest();
        newJobInfo.setId(JOB_ID);
        newJobInfo.setJobName("omsOpenAPIJobccccc");
        newJobInfo.setJobDescription("test OpenAPI");
        newJobInfo.setJobParams("{'aa':'bb'}");
        newJobInfo.setTimeExpressionType(TimeExpressionType.CRON);
        newJobInfo.setTimeExpression("0 0 * * * ? ");
        newJobInfo.setExecuteType(ExecuteType.STANDALONE);
        newJobInfo.setProcessorType(ProcessorType.EMBEDDED_JAVA);
        newJobInfo.setProcessorInfo("com.github.kfcfans.powerjob.samples.processors.StandaloneProcessorDemo");
        newJobInfo.setDesignatedWorkers("");

        newJobInfo.setMinCpuCores(1.1);
        newJobInfo.setMinMemorySpace(1.2);
        newJobInfo.setMinDiskSpace(1.3);

        ResultDTO<Long> resultDTO = ohMyClient.saveJob(newJobInfo);
        System.out.println(JSONObject.toJSONString(resultDTO));
        Assertions.assertNotNull(resultDTO);
    }

    @Test
    void testCopyJob() {
        ResultDTO<Long> copyJobRes = ohMyClient.copyJob(JOB_ID);
        System.out.println(JSONObject.toJSONString(copyJobRes));
        Assertions.assertNotNull(copyJobRes);
    }

    @Test
    void testFetchJob() {
        ResultDTO<JobInfoDTO> fetchJob = ohMyClient.fetchJob(JOB_ID);
        System.out.println(JSONObject.toJSONString(fetchJob));
        Assertions.assertNotNull(fetchJob);
    }

    @Test
    void testDisableJob() {
        ResultDTO<Void> res = ohMyClient.disableJob(JOB_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testEnableJob() {
        ResultDTO<Void> res = ohMyClient.enableJob(JOB_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testDeleteJob() {
        ResultDTO<Void> res = ohMyClient.deleteJob(JOB_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testRun() {
        ResultDTO<Long> res = ohMyClient.runJob(JOB_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testRunJobDelay() {
        ResultDTO<Long> res = ohMyClient.runJob(JOB_ID, "this is instanceParams", 60000);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testFetchInstanceInfo() {
        ResultDTO<InstanceInfoDTO> res = ohMyClient.fetchInstanceInfo(205436386851946560L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testStopInstance() {
        ResultDTO<Void> res = ohMyClient.stopInstance(205436995885858880L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testFetchInstanceStatus() {
        ResultDTO<Integer> res = ohMyClient.fetchInstanceStatus(205436995885858880L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testCancelInstanceInTimeWheel() {
        ResultDTO<Long> startRes = ohMyClient.runJob(JOB_ID, "start by OhMyClient", 20000);
        System.out.println("runJob result: " + JSONObject.toJSONString(startRes));
        ResultDTO<Void> cancelRes = ohMyClient.cancelInstance(startRes.getData());
        System.out.println("cancelJob result: " + JSONObject.toJSONString(cancelRes));
        Assertions.assertTrue(cancelRes.isSuccess());
    }

    @Test
    @SneakyThrows
    void testCancelInstanceInDatabase() {
        ResultDTO<Long> startRes = ohMyClient.runJob(15L, "start by OhMyClient", 2000000);
        System.out.println("runJob result: " + JSONObject.toJSONString(startRes));

        // Restart server manually and clear all the data in time wheeler.
        TimeUnit.MINUTES.sleep(1);

        ResultDTO<Void> cancelRes = ohMyClient.cancelInstance(startRes.getData());
        System.out.println("cancelJob result: " + JSONObject.toJSONString(cancelRes));
        Assertions.assertTrue(cancelRes.isSuccess());
    }

    @Test
    void testRetryInstance() {
        ResultDTO<Void> res = ohMyClient.retryInstance(169557545206153344L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }
}
