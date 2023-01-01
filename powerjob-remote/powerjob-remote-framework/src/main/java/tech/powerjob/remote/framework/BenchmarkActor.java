package tech.powerjob.remote.framework;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.remote.framework.actor.Handler;

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
    public BenchmarkResponse processStandardRequest(BenchmarkRequest request) {
        log.info("[BenchmarkActor] receive request: {}", request);
        BenchmarkResponse response = new BenchmarkResponse()
                .setSuccess(true)
                .setContent(request.getContent())
                .setProcessThread(Thread.currentThread().getName())
                .setServerReceiveTs(System.currentTimeMillis());
        if (request.getResponseSize() != 0 && request.getResponseSize() > 0) {
            response.setExtra(RandomStringUtils.random(request.getResponseSize()));
        }
        if (request.getBlockingMills() !=0 && request.getBlockingMills() > 0) {
            CommonUtils.easySleep(request.getBlockingMills());
        }
        return response;
    }


    @Data
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

        private String extra;
    }
}
