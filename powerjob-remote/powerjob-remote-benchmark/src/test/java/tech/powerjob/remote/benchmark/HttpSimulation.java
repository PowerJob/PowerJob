package tech.powerjob.remote.benchmark;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
/**
 * description
 *
 * @author tjq
 * @since 2023/1/8
 */
public class HttpSimulation extends Simulation {

    String baseUrl = String.format("http://%s:8080", Constant.SERVER_HOST);
    HttpProtocolBuilder httpProtocol = http // 4
            .baseUrl(baseUrl) // 5
            .acceptHeader("application/json") // 6
            .doNotTrackHeader("1")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .acceptEncodingHeader("gzip, deflate")
            .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");

    ScenarioBuilder scn = scenario("HttpSimulation") // 7
            .exec(http("request_http") // 请求名称，用于压测报表展示
                    .get("/httpAsk?debug=true&responseSize=1024")) // 9
            .pause(5); // 10

    {
        setUp( // 11
                scn.injectOpen(atOnceUsers(1)) // 12
        ).protocols(httpProtocol); // 13
    }
}
