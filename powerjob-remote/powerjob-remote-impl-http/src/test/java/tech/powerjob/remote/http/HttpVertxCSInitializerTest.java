package tech.powerjob.remote.http;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.remote.framework.BenchmarkActor;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.HandlerLocation;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.engine.EngineConfig;
import tech.powerjob.remote.framework.engine.EngineOutput;
import tech.powerjob.remote.framework.engine.RemoteEngine;
import tech.powerjob.remote.framework.engine.impl.PowerJobRemoteEngine;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * HttpVertxCSInitializerTest
 *
 * @author tjq
 * @since 2023/1/2
 */
@Slf4j
class HttpVertxCSInitializerTest {

    @Test
    void testHttpVertxCSInitializerTest() throws Exception {

        final Address address = new Address().setPort(7890).setHost("127.0.0.1");

        EngineConfig engineConfig = new EngineConfig()
                .setType(Protocol.HTTP.name())
                .setBindAddress(address)
                .setActorList(Lists.newArrayList(new BenchmarkActor()));

        RemoteEngine engine = new PowerJobRemoteEngine();
        EngineOutput engineOutput = engine.start(engineConfig);
        log.info("[HttpVertxCSInitializerTest] engine start up successfully!");
        Transporter transporter = engineOutput.getTransporter();

        BenchmarkActor.BenchmarkRequest request = new BenchmarkActor.BenchmarkRequest()
                .setContent("request from test")
                .setBlockingMills(100)
                .setResponseSize(10240);

        log.info("[HttpVertxCSInitializerTest] test empty request!");
        URL emptyURL = new URL()
                .setAddress(address)
                .setLocation(new HandlerLocation().setMethodPath("emptyReturn").setRootPath("benchmark"));
        transporter.tell(emptyURL, request);

        log.info("[HttpVertxCSInitializerTest] test string request!");
        URL stringURL = new URL()
                .setAddress(address)
                .setLocation(new HandlerLocation().setMethodPath("stringReturn").setRootPath("benchmark"));
        final String strResponse = transporter.ask(stringURL, request, String.class).toCompletableFuture().get();
        log.info("[HttpVertxCSInitializerTest] strResponse: {}", strResponse);

        log.info("[HttpVertxCSInitializerTest] test normal request!");
        URL url = new URL()
                .setAddress(address)
                .setLocation(new HandlerLocation().setMethodPath("standard").setRootPath("benchmark"));

        final CompletionStage<BenchmarkActor.BenchmarkResponse> benchmarkResponseCompletionStage = transporter.ask(url, request, BenchmarkActor.BenchmarkResponse.class);
        final BenchmarkActor.BenchmarkResponse response = benchmarkResponseCompletionStage.toCompletableFuture().get(10, TimeUnit.SECONDS);
        log.info("[HttpVertxCSInitializerTest] response: {}", response);




        CommonUtils.easySleep(10000);
    }
}