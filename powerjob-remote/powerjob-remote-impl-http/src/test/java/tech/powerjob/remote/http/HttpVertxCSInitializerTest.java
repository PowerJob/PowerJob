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
import tech.powerjob.remote.framework.engine.config.EngineConfig;
import tech.powerjob.remote.framework.engine.EngineOutput;
import tech.powerjob.remote.framework.engine.RemoteEngine;
import tech.powerjob.remote.framework.engine.impl.PowerJobRemoteEngine;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.util.UUID;
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
                .setResponseSize(1024);

        log.info("[HttpVertxCSInitializerTest] test empty request!");
        URL emptyURL = new URL()
                .setAddress(address)
                .setLocation(new HandlerLocation().setMethodPath("emptyReturn").setRootPath("benchmark"));
        request.setId(UUID.randomUUID().toString());
        long s1 = System.currentTimeMillis();
        transporter.tell(emptyURL, request);
        log.info("[HttpVertxCSInitializerTest] test empty request, tell cost: {}ms", System.currentTimeMillis() - s1);

        log.info("[HttpVertxCSInitializerTest] test string request!");
        URL stringURL = new URL()
                .setAddress(address)
                .setLocation(new HandlerLocation().setMethodPath("stringReturn").setRootPath("benchmark"));
        request.setId(UUID.randomUUID().toString());
        long s2 = System.currentTimeMillis();
        CompletionStage<String> stringCompletionStage = transporter.ask(stringURL, request, String.class);
        long s3 = System.currentTimeMillis();
        final String strResponse = stringCompletionStage.toCompletableFuture().get();
        long s4 = System.currentTimeMillis();
        log.info("[HttpVertxCSInitializerTest] askCost:{}, waitCost:{}, strResponse: {}", s3-s2, s4-s3, strResponse);

        log.info("[HttpVertxCSInitializerTest] test normal request!");
        URL url = new URL()
                .setAddress(address)
                .setLocation(new HandlerLocation().setMethodPath("standard").setRootPath("benchmark"));

        request.setId(UUID.randomUUID().toString());
        final CompletionStage<BenchmarkActor.BenchmarkResponse> benchmarkResponseCompletionStage = transporter.ask(url, request, BenchmarkActor.BenchmarkResponse.class);
        final BenchmarkActor.BenchmarkResponse response = benchmarkResponseCompletionStage.toCompletableFuture().get(10, TimeUnit.SECONDS);
        log.info("[HttpVertxCSInitializerTest] response: {}", response);




        CommonUtils.easySleep(10000);
    }
}