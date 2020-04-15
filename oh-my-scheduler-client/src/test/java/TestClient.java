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
    public void testInstanceOpenAPI() throws Exception {
        System.out.println(ohMyClient.stopInstance(1586855173043L));
        System.out.println(ohMyClient.fetchInstanceStatus(1586855173043L));
    }

    @Test
    public void testJobOpenAPI() throws Exception {
        System.out.println(ohMyClient.runJob(1L, "hhhh"));
    }
}
