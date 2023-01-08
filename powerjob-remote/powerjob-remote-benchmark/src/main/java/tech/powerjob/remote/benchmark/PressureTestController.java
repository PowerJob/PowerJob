package tech.powerjob.remote.benchmark;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.remote.framework.BenchmarkActor;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.HandlerLocation;
import tech.powerjob.remote.framework.base.URL;

import javax.annotation.Resource;

import java.util.concurrent.CompletionStage;

import static tech.powerjob.remote.benchmark.EngineService.*;

/**
 * 压测测试入口
 *
 * @author tjq
 * @since 2023/1/7
 */
@Slf4j
@RestController
@RequestMapping("/pressure")
public class PressureTestController {

    private static final HandlerLocation HL = new HandlerLocation().setRootPath("benchmark").setMethodPath("standard");

    @Resource
    private EngineService engineService;

    @GetMapping("/tell")
    public void httpTell(String protocol, Integer blockMs, Integer responseSize, String content) {
        Address address = new Address().setHost(HOST);
        URL url = new URL().setLocation(HL).setAddress(address);
        final BenchmarkActor.BenchmarkRequest request = new BenchmarkActor.BenchmarkRequest().setContent(content).setBlockingMills(blockMs).setResponseSize(responseSize);
        try {
            if (Protocol.HTTP.name().equalsIgnoreCase(protocol)) {
                address.setPort(SERVER_HTTP_PORT);
                engineService.getHttpTransporter().tell(url, request);
            } else {
                address.setPort(SERVER_AKKA_PORT);
                engineService.getAkkaTransporter().tell(url, request);
            }
        } catch (Exception e) {
            log.error("[HttpTell] process failed!", e);
            ExceptionUtils.rethrow(e);
        }
    }


    @GetMapping("/ask")
    public void httpAsk(String protocol, Integer blockMs, Integer responseSize, String content, Boolean debug) {
        Address address = new Address().setHost(HOST);
        URL url = new URL().setLocation(HL).setAddress(address);
        final BenchmarkActor.BenchmarkRequest request = new BenchmarkActor.BenchmarkRequest().setContent(content).setBlockingMills(blockMs).setResponseSize(responseSize);
        try {
            CompletionStage<BenchmarkActor.BenchmarkResponse> responseOpt = null;

            if (Protocol.HTTP.name().equalsIgnoreCase(protocol)) {
                address.setPort(SERVER_HTTP_PORT);
                responseOpt = engineService.getHttpTransporter().ask(url, request, BenchmarkActor.BenchmarkResponse.class);
            } else {
                address.setPort(SERVER_AKKA_PORT);
                responseOpt = engineService.getAkkaTransporter().ask(url, request, BenchmarkActor.BenchmarkResponse.class);
            }
            final BenchmarkActor.BenchmarkResponse response = responseOpt.toCompletableFuture().get();
            if (BooleanUtils.isTrue(debug)) {
                log.info("[httpAsk] response: {}", response);
            }
        } catch (Exception e) {
            log.error("[httpAsk] process failed", e);
            ExceptionUtils.rethrow(e);
        }
    }

}
