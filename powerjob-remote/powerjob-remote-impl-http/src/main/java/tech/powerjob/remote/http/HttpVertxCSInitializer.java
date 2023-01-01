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

/**
 * HttpCSInitializer
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
    public void bindHandlers(List<ActorInfo> actorInfos) {
        Router router = Router.router(vertx);
        // 处理请求响应
        router.route().handler(BodyHandler.create());
        actorInfos.forEach(actorInfo -> {
            log.info("[PowerJob-Vertx] start to bind Actor[{}]'s handler!", actorInfo.getAnno().path());
            Optional.ofNullable(actorInfo.getHandlerInfos()).orElse(Collections.emptyList()).forEach(handlerInfo -> {
                Method method = handlerInfo.getMethod();
                String handlerHttpPath = handlerInfo.getLocation().toPath();
                ProcessType processType = handlerInfo.getAnno().processType();
                log.info("[PowerJob-Vertx] start to register Handler with[path={},methodName={},processType={}]", handlerHttpPath, method.getName(), processType);

                Handler<RoutingContext> routingContextHandler = buildRequestHandler(actorInfo, handlerInfo);
                Route route = router.post(handlerHttpPath);
                if (processType == ProcessType.BLOCKING) {
                    route.blockingHandler(routingContextHandler, false);
                } else {
                    route.handler(routingContextHandler);
                }
                log.info("[PowerJob-Vertx] register Handler[path={},methodName={}] successfully!", handlerHttpPath, method.getName());
            });
        });

        // 启动 vertx http server
        final int port = config.getBindAddress().getPort();
        final String host = config.getBindAddress().getHost();
        httpServer.requestHandler(router)
                .exceptionHandler(e -> log.error("[PowerJob] unknown exception in Actor communication!", e))
                .listen(port, host, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        log.info("[PowerJob] startup vertx HttpServer successfully!");
                    } else {
                        log.error("[PowerJob] startup vertx HttpServer failed!", asyncResult.cause());
                        throw new PowerJobException("startup vertx HttpServer failed", asyncResult.cause());
                    }
                });

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
                        ctx.end(JsonObject.mapFrom(response).toBuffer());
                    }
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
        vertx.close();
        httpServer.close();
        httpClient.close();
    }
}
