package tech.powerjob.remote.http;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.cs.CSInitializer;
import tech.powerjob.remote.framework.cs.CSInitializerConfig;
import tech.powerjob.remote.framework.engine.EngineConfig;
import tech.powerjob.remote.framework.engine.EngineOutput;
import tech.powerjob.remote.framework.engine.RemoteEngine;
import tech.powerjob.remote.framework.engine.impl.PowerJobRemoteEngine;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpVertxCSInitializerTest
 *
 * @author tjq
 * @since 2023/1/2
 */
class HttpVertxCSInitializerTest {

    @Test
    void testHttpVertxCSInitializerTest() {

        final Address address = new Address().setPort(7890).setHost("127.0.0.1");

        EngineConfig engineConfig = new EngineConfig()
                .setTypes(Sets.newHashSet(Protocol.HTTP.name()))
                .setBindAddress(address);

        RemoteEngine engine = new PowerJobRemoteEngine();
        final EngineOutput engineOutput = engine.start(engineConfig);
    }

}