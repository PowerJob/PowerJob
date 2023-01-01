package tech.powerjob.remote.http;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.cs.CSInitializer;
import tech.powerjob.remote.framework.cs.CSInitializerConfig;
import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.remote.http.vertx.VertxInitializer;
import tech.powerjob.remote.http.vertx.VertxTransporter;

import java.io.IOException;
import java.util.List;

/**
 * HttpCSInitializer
 *
 * @author tjq
 * @since 2022/12/31
 */
public class HttpCSInitializer implements CSInitializer {

    private Vertx vertx;
    private HttpServer httpServer;
    private HttpClient httpClient;

    @Override
    public String type() {
        return tech.powerjob.common.enums.Protocol.HTTP.name();
    }

    @Override
    public void init(CSInitializerConfig config) {
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

    }

    @Override
    public void close() throws IOException {
        vertx.close();
        httpServer.close();
        httpClient.close();
    }
}
