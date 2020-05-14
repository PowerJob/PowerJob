import com.github.kfcfans.oms.client.model.ClientJobInfo;
import com.github.kfcfans.oms.common.ExecuteType;
import com.github.kfcfans.oms.common.ProcessorType;
import com.github.kfcfans.oms.common.TimeExpressionType;
import com.github.kfcfans.oms.common.response.ResultDTO;
import com.github.kfcfans.oms.client.OhMyClient;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.google.common.collect.Lists;
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
        ohMyClient = new OhMyClient("127.0.0.1:7700", "oms-test");
    }

    @Test
    public void testSaveJob() throws Exception {

        ClientJobInfo newJobInfo = new ClientJobInfo();
        newJobInfo.setJobId(6L);
        newJobInfo.setJobName("omsOpenAPIJob");
        newJobInfo.setJobDescription("tes OpenAPI");
        newJobInfo.setJobParams("{'aa':'bb'}");
        newJobInfo.setTimeExpressionType(TimeExpressionType.CRON);
        newJobInfo.setTimeExpression("0 0 * * * ? ");
        newJobInfo.setExecuteType(ExecuteType.STANDALONE);
        newJobInfo.setProcessorType(ProcessorType.EMBEDDED_JAVA);
        newJobInfo.setProcessorInfo("com.github.kfcfans.oms.server.tester.OmsLogPerformanceTester");
        newJobInfo.setDesignatedWorkers(Lists.newArrayList("192.168.1.1:2777"));

        ResultDTO<Long> resultDTO = ohMyClient.saveJob(newJobInfo);
        System.out.println(JsonUtils.toJSONString(resultDTO));
    }

    @Test
    public void testStopInstance() throws Exception {
        ResultDTO<Void> res = ohMyClient.stopInstance(132522955178508352L);
        System.out.println(res.toString());
    }
    @Test
    public void testFetchInstanceStatus() throws Exception {
        System.out.println(ohMyClient.fetchInstanceStatus(132522955178508352L));
    }
}
