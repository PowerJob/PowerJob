import com.github.kfcfans.powerjob.common.ExecuteType;
import com.github.kfcfans.powerjob.common.ProcessorType;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.request.http.SaveJobInfoRequest;
import com.github.kfcfans.powerjob.common.response.JobInfoDTO;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.client.OhMyClient;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 测试 Client
 *
 * @author tjq
 * @since 2020/4/15
 */
public class TestClient {

    private static OhMyClient ohMyClient;

    @BeforeAll
    public static void initClient() throws Exception {
        ohMyClient = new OhMyClient("127.0.0.1:7700", "oms-test2", null);
    }

    @Test
    public void testSaveJob() throws Exception {

        SaveJobInfoRequest newJobInfo = new SaveJobInfoRequest();
//        newJobInfo.setId(8L);
        newJobInfo.setJobName("omsOpenAPIJobccccc");
        newJobInfo.setJobDescription("tes OpenAPI");
        newJobInfo.setJobParams("{'aa':'bb'}");
        newJobInfo.setTimeExpressionType(TimeExpressionType.CRON);
        newJobInfo.setTimeExpression("0 0 * * * ? ");
        newJobInfo.setExecuteType(ExecuteType.STANDALONE);
        newJobInfo.setProcessorType(ProcessorType.EMBEDDED_JAVA);
        newJobInfo.setProcessorInfo("com.github.kfcfans.oms.server.tester.OmsLogPerformanceTester");
        newJobInfo.setDesignatedWorkers("192.168.1.1:2777");

        newJobInfo.setMinCpuCores(1.1);
        newJobInfo.setMinMemorySpace(1.2);
        newJobInfo.setMinDiskSpace(1.3);

        ResultDTO<Long> resultDTO = ohMyClient.saveJob(newJobInfo);
        System.out.println(JsonUtils.toJSONString(resultDTO));
    }

    @Test
    public void testFetchJob() throws Exception {
        ResultDTO<JobInfoDTO> fetchJob = ohMyClient.fetchJob(1L);
        System.out.println(JsonUtils.toJSONStringUnsafe(fetchJob));
    }

    @Test
    public void testDisableJob() throws Exception {
        System.out.println(ohMyClient.disableJob(7L));
    }

    @Test
    public void testEnableJob() throws Exception {
        System.out.println(ohMyClient.enableJob(7L));
    }

    @Test
    public void testDeleteJob() throws Exception {
        System.out.println(ohMyClient.deleteJob(7L));
    }

    @Test
    public void testRunJob() throws Exception {
        System.out.println(ohMyClient.runJob(8L, "this is instanceParams"));
    }

    @Test
    public void testFetchInstanceInfo() throws Exception {
        System.out.println(ohMyClient.fetchInstanceInfo(141251409466097728L));
    }

    @Test
    public void testStopInstance() throws Exception {
        ResultDTO<Void> res = ohMyClient.stopInstance(141251409466097728L);
        System.out.println(res.toString());
    }
    @Test
    public void testFetchInstanceStatus() throws Exception {
        System.out.println(ohMyClient.fetchInstanceStatus(141251409466097728L));
    }
}
