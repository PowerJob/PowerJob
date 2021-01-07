import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.ExecuteType;
import com.github.kfcfans.powerjob.common.ProcessorType;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.request.http.SaveJobInfoRequest;
import com.github.kfcfans.powerjob.common.response.JobInfoDTO;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.client.OhMyClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * Test cases for {@link OhMyClient}
 *
 * @author tjq
 * @since 2020/4/15
 */
public class TestClient {

    private static OhMyClient ohMyClient;

    public static final long JOB_ID = 4L;

    @BeforeAll
    public static void initClient() throws Exception {
        ohMyClient = new OhMyClient("127.0.0.1:7700", "powerjob-agent-test", "123");
    }

    @Test
    public void testSaveJob() throws Exception {

        SaveJobInfoRequest newJobInfo = new SaveJobInfoRequest();
        newJobInfo.setId(JOB_ID);
        newJobInfo.setJobName("omsOpenAPIJobccccc");
        newJobInfo.setJobDescription("tes OpenAPI");
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
    }

    @Test
    public void testFetchJob() throws Exception {
        ResultDTO<JobInfoDTO> fetchJob = ohMyClient.fetchJob(JOB_ID);
        System.out.println(JSONObject.toJSONString(fetchJob));
    }

    @Test
    public void testDisableJob() throws Exception {
        System.out.println(ohMyClient.disableJob(JOB_ID));
    }

    @Test
    public void testEnableJob() throws Exception {
        System.out.println(ohMyClient.enableJob(JOB_ID));
    }

    @Test
    public void testDeleteJob() throws Exception {
        System.out.println(ohMyClient.deleteJob(JOB_ID));
    }

    @Test
    public void testRun() {
        System.out.println(ohMyClient.runJob(JOB_ID));
    }

    @Test
    public void testRunJobDelay() throws Exception {
        System.out.println(ohMyClient.runJob(JOB_ID, "this is instanceParams", 60000));
    }

    @Test
    public void testFetchInstanceInfo() throws Exception {
        System.out.println(ohMyClient.fetchInstanceInfo(205436386851946560L));
    }

    @Test
    public void testStopInstance() throws Exception {
        ResultDTO<Void> res = ohMyClient.stopInstance(205436995885858880L);
        System.out.println(res.toString());
    }
    @Test
    public void testFetchInstanceStatus() throws Exception {
        System.out.println(ohMyClient.fetchInstanceStatus(205436995885858880L));
    }

    @Test
    public void testCancelInstanceInTimeWheel() throws Exception {
        ResultDTO<Long> startRes = ohMyClient.runJob(JOB_ID, "start by OhMyClient", 20000);
        System.out.println("runJob result: " + JSONObject.toJSONString(startRes));
        ResultDTO<Void> cancelRes = ohMyClient.cancelInstance(startRes.getData());
        System.out.println("cancelJob result: " + JSONObject.toJSONString(cancelRes));
    }

    @Test
    public void testCancelInstanceInDatabase() throws Exception {
        ResultDTO<Long> startRes = ohMyClient.runJob(15L, "start by OhMyClient", 2000000);
        System.out.println("runJob result: " + JSONObject.toJSONString(startRes));

        // Restart server manually and clear all the data in time wheeler.
        TimeUnit.MINUTES.sleep(1);

        ResultDTO<Void> cancelRes = ohMyClient.cancelInstance(startRes.getData());
        System.out.println("cancelJob result: " + JSONObject.toJSONString(cancelRes));
    }

    @Test
    public void testRetryInstance() throws Exception {
        ResultDTO<Void> res = ohMyClient.retryInstance(169557545206153344L);
        System.out.println(res);
    }
}
