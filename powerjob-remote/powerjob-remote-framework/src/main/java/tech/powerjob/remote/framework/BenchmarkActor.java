package tech.powerjob.remote.framework;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.remote.framework.actor.Handler;

import java.util.Optional;

/**
 * 基准测试
 *
 * @author tjq
 * @since 2023/1/1
 */
@Slf4j
@Actor(path = "benchmark")
public class BenchmarkActor {

    @Handler(path = "standard")
    public BenchmarkResponse standardRequest(BenchmarkRequest request) {
        long startTs = System.currentTimeMillis();
        log.info("[BenchmarkActor] [standardRequest] receive request: {}", request);
        BenchmarkResponse response = new BenchmarkResponse()
                .setSuccess(true)
                .setContent(request.getContent())
                .setProcessThread(Thread.currentThread().getName())
                .setServerReceiveTs(System.currentTimeMillis());
        if (request.getResponseSize() != null && request.getResponseSize() > 0) {
            response.setExtra(RandomStringUtils.randomPrint(request.getResponseSize()));
        }
        executeSleep(request);
        response.setServerCost(System.currentTimeMillis() - startTs);
        return response;
    }

    @Handler(path = "emptyReturn")
    public void emptyReturn(BenchmarkRequest request) {
        log.info("[BenchmarkActor] [emptyReturn] receive request: {}", request);
        executeSleep(request);
    }

    @Handler(path = "stringReturn")
    public String stringReturn(BenchmarkRequest request) {
        log.info("[BenchmarkActor] [stringReturn] receive request: {}", request);
        executeSleep(request);
        return RandomStringUtils.randomPrint(Optional.ofNullable(request.getResponseSize()).orElse(100));
    }

    private static void executeSleep(BenchmarkRequest request) {
        if (request.getBlockingMills() != null && request.getBlockingMills() > 0) {
            CommonUtils.easySleep(request.getBlockingMills());
        }
    }


    @Data
    @Accessors(chain = true)
    public static class BenchmarkRequest implements PowerSerializable {
        /**
         * 请求内容
         */
        private String content;
        /**
         * 期望的响应大小，可空
         */
        private Integer responseSize;
        /**
         * 阻塞时间，模拟 IO 耗时
         */
        private Integer blockingMills;
    }

    @Data
    @Accessors(chain = true)
    public static class BenchmarkResponse implements PowerSerializable {
        private boolean success;
        /**
         * 原路返回原来的 content
         */
        private String content;

        private String processThread;
        private long serverReceiveTs;

        private long serverCost;

        private String extra;
    }
}
