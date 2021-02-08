package com.github.kfcfans.powerjob.server.handler.outer;

import com.github.kfcfans.powerjob.common.OmsConstant;
import com.github.kfcfans.powerjob.common.ProtocolConstant;
import com.github.kfcfans.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import com.github.kfcfans.powerjob.common.request.WorkerHeartbeat;
import com.github.kfcfans.powerjob.common.request.WorkerLogReportReq;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.github.kfcfans.powerjob.server.common.PowerJobServerConfigKey;
import com.github.kfcfans.powerjob.server.common.utils.PropertyUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Properties;

import static com.github.kfcfans.powerjob.server.handler.outer.WorkerRequestHandler.getWorkerRequestHandler;

/**
 * WorkerRequestHandler
 *
 * @author tjq
 * @since 2021/2/8
 */
public class WorkerRequestHttpHandler extends AbstractVerticle {

    @Override
    public void start() throws Exception {

        Properties properties = PropertyUtils.getProperties();
        int port = Integer.parseInt(properties.getProperty(PowerJobServerConfigKey.HTTP_PORT, String.valueOf(OmsConstant.SERVER_DEFAULT_HTTP_PORT)));

        HttpServerOptions options = new HttpServerOptions();
        HttpServer server = vertx.createHttpServer(options);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post(ProtocolConstant.SERVER_PATH_HEARTBEAT)
                .handler(ctx -> {
                    WorkerHeartbeat heartbeat = ctx.getBodyAsJson().mapTo(WorkerHeartbeat.class);
                    getWorkerRequestHandler().onReceiveWorkerHeartbeat(heartbeat);
                });
        router.post(ProtocolConstant.SERVER_PATH_STATUS_REPORT)
                .blockingHandler(ctx -> {
                    TaskTrackerReportInstanceStatusReq req = ctx.getBodyAsJson().mapTo(TaskTrackerReportInstanceStatusReq.class);
                    getWorkerRequestHandler().onReceiveTaskTrackerReportInstanceStatusReq(req);
                    out(ctx, AskResponse.succeed(null));
                });
        router.post(ProtocolConstant.SERVER_PATH_LOG_REPORT)
                .blockingHandler(ctx -> {
                    WorkerLogReportReq req = ctx.getBodyAsJson().mapTo(WorkerLogReportReq.class);
                    getWorkerRequestHandler().onReceiveWorkerLogReportReq(req);
                });
        server.requestHandler(router).listen(port);
    }

    private static void out(RoutingContext ctx, Object msg) {
        ctx.response()
                .putHeader("Content-Type", OmsConstant.JSON_MEDIA_TYPE)
                .end(JsonObject.mapFrom(msg).encode());
    }
}
