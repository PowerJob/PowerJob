package tech.powerjob.remote.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.actor.HandlerInfo;
import tech.powerjob.remote.framework.actor.ProcessType;
import tech.powerjob.remote.framework.cs.CSInitializer;
import tech.powerjob.remote.framework.cs.CSInitializerConfig;
import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.remote.framework.utils.RemoteUtils;
import tech.powerjob.remote.http.vertx.VertxInitializer;
import tech.powerjob.remote.http.vertx.VertxTransporter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * HttpCSInitializer
 * 在纠结了1晚上后，最终决定选用 vertx 作为 http 底层，而不是直接使用 netty，理由如下：
 *  - netty 实现容易，但性能调优方面需要时间成本和实践经验，而 vertx 作为 netty 的"嫡系"框架，对 netty 的封装理论上炉火纯青，性能不成问题
 *  - vertx 唯一的缺点是其作为相对上层的框架，可能存在较为严重的包冲突问题，尤其是对于那些本身跑在 vertx-framework 上的用户
 *      - 不过该问题可以通过更换协议解决，预计后续提供一个基于 netty 和自定义协议的实现
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
public class HttpVertxCSInitializer implements CSInitializer {

    private Vertx vertx;
    private HttpServer httpServer;
    private HttpClient httpClient;

    private CSInitializerConfig config;

    @Override
    public String type() {
        return tech.powerjob.common.enums.Protocol.HTTP.name();
    }

    @Override
    public void init(CSInitializerConfig config) {
        this.config = config;
        vertx = VertxInitializer.buildVertx();
        httpServer = VertxInitializer.buildHttpServer(vertx);
        httpClient = VertxInitializer.buildHttpClient(vertx);
    }

    @Override
    public Transporter buildTransporter() {
        return new VertxTransporter(httpClient);
    }

    @Override
    @SneakyThrows
    public void bindHandlers(List<ActorInfo> actorInfos) {
        Router router = Router.router(vertx);
        // 处理请求响应
        router.route().handler(BodyHandler.create());
        actorInfos.forEach(actorInfo -> {
            Optional.ofNullable(actorInfo.getHandlerInfos()).orElse(Collections.emptyList()).forEach(handlerInfo -> {
                String handlerHttpPath = handlerInfo.getLocation().toPath();
                ProcessType processType = handlerInfo.getAnno().processType();

                Handler<RoutingContext> routingContextHandler = buildRequestHandler(actorInfo, handlerInfo);
                Route route = router.post(handlerHttpPath);
                if (processType == ProcessType.BLOCKING) {
                    route.blockingHandler(routingContextHandler, false);
                } else {
                    route.handler(routingContextHandler);
                }
            });
        });

        // 启动 vertx http server
        final int port = config.getBindAddress().getPort();
        final String host = config.getBindAddress().getHost();

        httpServer.requestHandler(router)
                .exceptionHandler(e -> log.error("[PowerJob] unknown exception in Actor communication!", e))
                .listen(port, host)
                .toCompletionStage()
                .toCompletableFuture()
                .get(1, TimeUnit.MINUTES);

        log.info("[PowerJobRemoteEngine] startup vertx HttpServer successfully!");
    }

    private Handler<RoutingContext> buildRequestHandler(ActorInfo actorInfo, HandlerInfo handlerInfo) {
        Method method = handlerInfo.getMethod();
        Optional<Class<?>> powerSerializeClz = RemoteUtils.findPowerSerialize(method.getParameterTypes());

        // 内部框架，严格模式，绑定失败直接报错
        if (!powerSerializeClz.isPresent()) {
            throw new PowerJobException("can't find any 'PowerSerialize' object in handler args: " + handlerInfo.getLocation());
        }

        return ctx -> {
            final RequestBody body = ctx.body();
            final Object convertResult = body.asPojo(powerSerializeClz.get());
            try {
                Object response = method.invoke(actorInfo.getActor(), convertResult);
                if (response != null) {
                    if (response instanceof String) {
                        ctx.end((String) response);
                    } else {
                        ctx.json(JsonObject.mapFrom(response));
                    }
                    return;
                }

                ctx.end();
            } catch (Throwable t) {
                // 注意这里是框架实际运行时，日志输出用标准 PowerJob 格式
                log.error("[PowerJob] invoke Handler[{}] failed!", handlerInfo.getLocation(), t);
                ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), t);
            }
        };
    }


    @Override
    public void close() throws IOException {
        httpClient.close();
        httpServer.close();
        vertx.close();
    }
}
