import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.oms.client.OhMyClient;
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
    public void testStopInstance() throws Exception {
        ResultDTO<Void> res = ohMyClient.stopInstance(132522955178508352L);
        System.out.println(res.toString());
    }
    @Test
    public void testFetchInstanceStatus() throws Exception {
        System.out.println(ohMyClient.fetchInstanceStatus(132522955178508352L));
    }
}
