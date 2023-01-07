package tech.powerjob.remote.framework.engine;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.engine.impl.PowerJobRemoteEngine;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RemoteEngineTest
 *
 * @author tjq
 * @since 2022/12/31
 */
class RemoteEngineTest {

    @Test
    void start() {

        RemoteEngine remoteEngine = new PowerJobRemoteEngine();

        EngineConfig engineConfig = new EngineConfig();
        engineConfig.setType("TEST");
        engineConfig.setBindAddress(new Address().setHost("127.0.0.1").setPort(10086));
        remoteEngine.start(engineConfig);
    }
}