package tech.powerjob.remote.http.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import lombok.extern.slf4j.Slf4j;

/**
 * VertxInitializer
 * PowerJob 只是将 vertx 作为 toolkit 使用
 *
 * @author tjq
 * @since 2023/1/1
 */
@Slf4j
public class VertxInitializer {

    public static Vertx buildVertx() {
        VertxOptions options = new VertxOptions();
        log.info("[PowerJob-Vertx] use vertx options: {}", options);
        return Vertx.vertx(options);
    }

    public static HttpServer buildHttpServer(Vertx vertx) {
        HttpServerOptions httpServerOptions = new HttpServerOptions();
        tryEnableCompression(httpServerOptions);
        log.info("[PowerJob-Vertx] use HttpServerOptions: {}", httpServerOptions.toJson());
        return vertx.createHttpServer(httpServerOptions);
    }
    private static void tryEnableCompression(HttpServerOptions httpServerOptions) {
        // 非核心组件，不直接依赖类（无 import），加载报错可忽略
        try {
            httpServerOptions
                    .addCompressor(io.netty.handler.codec.compression.StandardCompressionOptions.gzip())
                    .setCompressionSupported(true);
            log.warn("[PowerJob-Vertx] enable server side compression successfully!");
        } catch (Exception e) {
            log.warn("[PowerJob-Vertx] enable server side compression failed!", e);
        }
    }

    public static HttpClient buildHttpClient(Vertx vertx) {
        HttpClientOptions httpClientOptions = new HttpClientOptions();
        log.info("[PowerJob-Vertx] use HttpClientOptions: {}", httpClientOptions.toJson());
        return vertx.createHttpClient(httpClientOptions);
    }

}
