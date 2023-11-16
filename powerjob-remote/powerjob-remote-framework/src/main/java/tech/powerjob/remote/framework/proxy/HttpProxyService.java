package tech.powerjob.remote.framework.proxy;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.remote.framework.base.RemotingException;
import tech.powerjob.remote.framework.base.ServerType;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.engine.config.ProxyConfig;
import tech.powerjob.remote.framework.proxy.module.ProxyMethod;
import tech.powerjob.remote.framework.proxy.module.ProxyRequest;
import tech.powerjob.remote.framework.proxy.module.ProxyResult;
import tech.powerjob.remote.framework.transporter.Protocol;
import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.shade.io.netty.handler.codec.http.HttpHeaderNames;
import tech.powerjob.shade.io.netty.handler.codec.http.HttpHeaderValues;
import tech.powerjob.shade.io.netty.handler.codec.http.HttpResponseStatus;
import tech.powerjob.shade.io.vertx.core.Future;
import tech.powerjob.shade.io.vertx.core.Vertx;
import tech.powerjob.shade.io.vertx.core.VertxOptions;
import tech.powerjob.shade.io.vertx.core.http.*;
import tech.powerjob.shade.io.vertx.core.json.JsonObject;
import tech.powerjob.shade.io.vertx.ext.web.RequestBody;
import tech.powerjob.shade.io.vertx.ext.web.Router;
import tech.powerjob.shade.io.vertx.ext.web.handler.BodyHandler;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 代理服务
 *
 * @author tjq
 * @since 2023/11/16
 */
@Slf4j
public class HttpProxyService implements ProxyService {

    private Vertx _vertx;
    private ProxyConfig proxyConfig;

    private final Transporter transporter;

    private static final String PROXY_PATH = "/proxy";

    public HttpProxyService(Transporter transporter) {
        this.transporter = transporter;
    }

    @Override
    @SneakyThrows
    public void initializeProxyServer(ProxyConfig proxyConfig) {

        this.proxyConfig = proxyConfig;
        if (proxyConfig == null || !proxyConfig.isEnableProxyServer()) {
            log.info("[HttpProxyService] no proxy server config, skip initialize.");
            return;
        }

        log.info("[HttpProxyService] start to initialize proxy server by proxy config: {}", proxyConfig);

        HttpServerOptions httpServerOptions = new HttpServerOptions().setIdleTimeout(300);
        HttpServer httpServer = vertx().createHttpServer(httpServerOptions);
        Router router = Router.router(vertx());
        router.route().handler(BodyHandler.create());

        router.post(PROXY_PATH).blockingHandler(ctx -> {
            final RequestBody body = ctx.body();
            ProxyRequest proxyRequest = body.asPojo(ProxyRequest.class);

            PowerSerializable ret = (PowerSerializable) JsonUtils.parseObjectUnsafe(proxyRequest.getRequest(), proxyRequest.getClz());

            if (ProxyMethod.TELL.getV().equals(proxyRequest.getProxyMethod())) {
                transporter.tell(proxyRequest.getUrl(), ret);
                ctx.json(JsonObject.mapFrom(new ProxyResult().setSuccess(true)));
                return;
            }

            ProxyResult proxyResult = new ProxyResult();
            try {
                CompletionStage<Object> proxyRequestStage = transporter.ask(proxyRequest.getUrl(), ret, Object.class);
                Object originResult = proxyRequestStage.toCompletableFuture().get(1, TimeUnit.SECONDS);
                proxyResult.setSuccess(true).setData(JsonUtils.toJSONString(originResult));
            } catch (Exception e) {
                proxyResult.setSuccess(false).setMsg(ExceptionUtils.getMessage(e));
                log.error("[HttpProxyService] proxy request failed!", e);
            }

            log.debug("[HttpProxyService] send proxy result: {}", proxyResult);
            ctx.json(JsonObject.mapFrom(proxyResult));
        });

        Integer proxyServerPort = proxyConfig.getProxyServerPort();

        httpServer.requestHandler(router)
                .exceptionHandler(e -> log.error("[HttpProxyService] unknown exception in Actor communication!", e))
                .listen(proxyServerPort)
                .toCompletionStage()
                .toCompletableFuture()
                .get(1, TimeUnit.MINUTES);

        log.info("[HttpProxyService] initialize proxy server in port: {}", proxyServerPort);
    }

    @Override
    public Transporter warpProxyTransporter(ServerType currentServerType) {

        if (proxyConfig.isUseProxy()) {
            String proxyUrl = proxyConfig.getProxyUrl();
            if (StringUtils.isEmpty(proxyUrl)) {
                throw new IllegalArgumentException("when you use proxy, you must set the proxy url(ProxyConfig.proxyUrl)!");
            }
            log.info("[HttpProxyService] use proxy to visit other type node, proxy url: {}", proxyUrl);
            return new ProxyTransporter(currentServerType);
        }
        return transporter;
    }

    CompletionStage<ProxyResult> sendProxyRequest(ProxyRequest proxyRequest) {

        String fullUrl = String.format("%s/%s", proxyConfig.getProxyUrl(), PROXY_PATH);

        HttpClient httpClient = vertx().createHttpClient();
        RequestOptions requestOptions = new RequestOptions()
                .setMethod(HttpMethod.POST)
                .setAbsoluteURI(fullUrl);
        // 转换 -> 发送请求获取响应
        Future<HttpClientResponse> responseFuture = httpClient
                .request(requestOptions)
                .compose(httpClientRequest ->
                        httpClientRequest
                                .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                                .send(JsonObject.mapFrom(proxyRequest).toBuffer())
        );
        return responseFuture.compose(httpClientResponse -> {
                    // throw exception
                    final int statusCode = httpClientResponse.statusCode();
                    if (statusCode != HttpResponseStatus.OK.code()) {
                        // CompletableFuture.get() 时会传递抛出该异常
                        throw new RemotingException(String.format("request [%s] failed, status: %d, msg: %s",
                                fullUrl, statusCode, httpClientResponse.statusMessage()
                        ));
                    }

                    return httpClientResponse.body().compose(x -> Future.succeededFuture(x.toJsonObject().mapTo(ProxyResult.class)));
                })
                .onFailure(t -> log.warn("[HttpProxyService] sendProxyRequest to url[{}] failed,msg: {}", fullUrl, ExceptionUtils.getMessage(t)))
                .toCompletionStage();

    }

    private Vertx vertx() {

        if (_vertx != null) {
            return _vertx;
        }

        synchronized (this) {
            if (_vertx == null) {
                VertxOptions options = new VertxOptions().setWorkerPoolSize(32).setInternalBlockingPoolSize(32);
                _vertx = Vertx.vertx(options);
            }
        }
        return _vertx;
    }


    class ProxyTransporter implements Transporter {

        private final ServerType myServerType;

        public ProxyTransporter(ServerType myServerType) {
            this.myServerType = myServerType;
        }

        @Override
        public Protocol getProtocol() {
            return transporter.getProtocol();
        }

        @Override
        public void tell(URL url, PowerSerializable request) {
            if (skipProxy(url)) {
                transporter.tell(url, request);
                return;
            }

            ProxyRequest proxyRequest = new ProxyRequest().setUrl(url).setRequest(request).setProxyMethod(ProxyMethod.TELL.getV());
            sendProxyRequest(proxyRequest);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletionStage<T> ask(URL url, PowerSerializable request, Class<T> clz) throws RemotingException {
            if (skipProxy(url)) {
                return transporter.ask(url, request, clz);
            }
            ProxyRequest proxyRequest = new ProxyRequest().setUrl(url).setRequest(request).setProxyMethod(ProxyMethod.ASK.getV());
            CompletionStage<ProxyResult> proxyRequestCompletionStage = sendProxyRequest(proxyRequest);
            return proxyRequestCompletionStage.thenApply(pr -> {
                if (pr.isSuccess()) {
                    if (clz == null) {
                        return null;
                    }
                    if (clz.equals(String.class)) {
                        return (T) pr.getData();
                    }
                    try {
                        return JsonUtils.parseObject(pr.getData(), clz);
                    } catch (Exception e) {
                        ExceptionUtils.rethrow(e);
                    }
                }
                throw new RemotingException("proxy failed, msg: " + pr.getMsg());
            });
        }

        private boolean skipProxy(URL url) {

            if (proxyConfig == null) {
                return true;
            }
            if (!proxyConfig.isUseProxy()) {
                return true;
            }
            if (StringUtils.isEmpty(proxyConfig.getProxyUrl())) {
                return true;
            }

            // 仅对向通讯需要使用代理
            return Objects.equals(url.getServerType(), myServerType);
        }
    }

}
