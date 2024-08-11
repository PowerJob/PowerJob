package tech.powerjob.client.test;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeAll;
import tech.powerjob.client.IPowerJobClient;
import tech.powerjob.client.PowerJobClient;

/**
 * Initialize OhMyClient
 *
 * @author tjq
 * @since 1/16/21
 */
public class ClientInitializer {

    protected static IPowerJobClient powerJobClient;

    @BeforeAll
    public static void initClient() throws Exception {
        powerJobClient = new PowerJobClient(Lists.newArrayList("127.0.0.1:7700", "127.0.0.1:7701"), "powerjob-worker-samples", "powerjob123");
    }
}
