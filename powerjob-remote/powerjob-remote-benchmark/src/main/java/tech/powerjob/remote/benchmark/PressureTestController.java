package tech.powerjob.remote.benchmark;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    @GetMapping("/httpTell")
    public void httpTell(Integer blockMs, Integer responseSize, String content) {
        URL url = new URL().setLocation(HL).setAddress(new Address().setPort(SERVER_HTTP_PORT).setHost(HOST));
        final BenchmarkActor.BenchmarkRequest request = new BenchmarkActor.BenchmarkRequest().setContent(content).setBlockingMills(blockMs).setResponseSize(responseSize);
        try {
            engineService.getHttpTransporter().tell(url, request);
        } catch (Exception e) {
            log.error("[HttpTell] process failed!", e);
        }
    }

    @GetMapping("/httpAsk")
    public void httpAsk(Integer blockMs, Integer responseSize, String content, Boolean debug) {
        URL url = new URL().setLocation(HL).setAddress(new Address().setPort(SERVER_HTTP_PORT).setHost(HOST));
        final BenchmarkActor.BenchmarkRequest request = new BenchmarkActor.BenchmarkRequest().setContent(content).setBlockingMills(blockMs).setResponseSize(responseSize);
        try {
            CompletionStage<BenchmarkActor.BenchmarkResponse> responseOpt = engineService.getHttpTransporter().ask(url, request, BenchmarkActor.BenchmarkResponse.class);
            final BenchmarkActor.BenchmarkResponse response = responseOpt.toCompletableFuture().get();
            if (BooleanUtils.isTrue(debug)) {
                log.info("[httpAsk] response: {}", response);
            }
        } catch (Exception e) {
            log.error("[httpAsk] process failed", e);
        }
    }

}
