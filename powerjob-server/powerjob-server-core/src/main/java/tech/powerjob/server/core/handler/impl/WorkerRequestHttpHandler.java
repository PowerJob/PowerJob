package tech.powerjob.server.core.handler.impl;

import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.ProtocolConstant;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.common.request.WorkerHeartbeat;
import tech.powerjob.common.request.WorkerLogReportReq;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.common.PowerJobServerConfigKey;
import tech.powerjob.server.common.utils.PropertyUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Properties;

import static tech.powerjob.server.core.handler.WorkerRequestHandler.getWorkerRequestHandler;

/**
 * WorkerRequestHandler
 *
 * @author tjq
 * @since 2021/2/8
 */
@Slf4j
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
                    success(ctx);
                });
        router.post(ProtocolConstant.SERVER_PATH_STATUS_REPORT)
                .blockingHandler(ctx -> {
                    TaskTrackerReportInstanceStatusReq req = ctx.getBodyAsJson().mapTo(TaskTrackerReportInstanceStatusReq.class);
                    try {
                        getWorkerRequestHandler().onReceiveTaskTrackerReportInstanceStatusReq(req);
                        out(ctx, AskResponse.succeed(null));
                    } catch (Exception e) {
                        log.error("[WorkerRequestHttpHandler] update instance status failed for request: {}.", req, e);
                        out(ctx, AskResponse.failed(ExceptionUtils.getMessage(e)));
                    }
                });
        router.post(ProtocolConstant.SERVER_PATH_LOG_REPORT)
                .blockingHandler(ctx -> {
                    WorkerLogReportReq req = ctx.getBodyAsJson().mapTo(WorkerLogReportReq.class);
                    getWorkerRequestHandler().onReceiveWorkerLogReportReq(req);
                    success(ctx);
                });
        server.requestHandler(router).listen(port);
    }

    private static void out(RoutingContext ctx, Object msg) {
        ctx.response()
                .putHeader(OmsConstant.HTTP_HEADER_CONTENT_TYPE, OmsConstant.JSON_MEDIA_TYPE)
                .end(JsonObject.mapFrom(msg).encode());
    }

    private static void success(RoutingContext ctx) {
        out(ctx, ResultDTO.success(null));
    }
}
