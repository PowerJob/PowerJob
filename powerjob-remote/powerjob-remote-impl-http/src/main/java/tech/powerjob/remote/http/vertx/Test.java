package tech.powerjob.remote.http.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Map;

/**
 * description
 *
 * @author tjq
 * @since 2023/1/1
 */
public class Test {

    public static void main(String[] args) {
        final Vertx vertx = Vertx.vertx();
        final HttpServer httpServer = vertx.createHttpServer();

        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/test/abc").handler(ctx -> {

            final Map<String, Object> data = ctx.data();
            System.out.println("ctx.data: " + data);
            final String body = ctx.body().asString();
            System.out.println("request: " + body);
            JsonObject jsonObject = new JsonObject();
            jsonObject.put("aa", "vv");


//            ctx.end(jsonObject.toBuffer());
            ctx.fail(404);
            ctx.end("failedFromServer");
        });

        httpServer
                .requestHandler(router)
                .exceptionHandler(e -> e.printStackTrace())
                .listen(7890);
        System.out.println("aa");
    }
}
