package tech.powerjob.client.test;

import tech.powerjob.client.PowerJobClient;
import org.junit.jupiter.api.BeforeAll;

/**
 * Initialize OhMyClient
 *
 * @author tjq
 * @since 1/16/21
 */
public class ClientInitializer {

    protected static PowerJobClient powerJobClient;

    @BeforeAll
    public static void initClient() throws Exception {
        powerJobClient = new PowerJobClient("127.0.0.1:7700", "powerjob-agent-test", "123");
    }
}
