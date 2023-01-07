package tech.powerjob.remote.benchmark;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.powerjob.remote.framework.BenchmarkActor;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.HandlerLocation;
import tech.powerjob.remote.framework.base.URL;

import javax.annotation.Resource;

import static tech.powerjob.remote.benchmark.EngineService.*;

/**
 * 压测测试入口
 *
 * @author tjq
 * @since 2023/1/7
 */
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
        engineService.getHttpTransporter().tell(url, request);
    }

}
