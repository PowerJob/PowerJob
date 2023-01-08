package tech.powerjob.remote.benchmark;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
/**
 * HTTP 压测模拟
 *
 *
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
                    .get("/pressure/ask?protocol=HTTP&debug=true&responseSize=1024")) // 9
            .pause(5); // 10

    /*
    atOnceUsers(10) 一次模拟的用户数量(10)
    nothingFor(4 seconds)  在指定的时间段(4 seconds)内什么都不干
    constantUsersPerSec(10) during(20 seconds) 以固定的速度模拟用户，指定每秒模拟的用户数(10)，指定模拟测试时间长度(20 seconds)
    rampUsersPerSec(10) to (20) during(20 seconds) 在指定的时间(20 seconds)内，使每秒模拟的用户从数量1(10)逐渐增加到数量2(20)，速度匀速
    heavisideUsers(100) over(10 seconds)    在指定的时间(10 seconds)内使用类似单位阶跃函数的方法逐渐增加模拟并发的用户，直到总数达到指定的数量(100).简单说就是每秒并发用户数递增
     */

    {
        setUp( // 11
                scn.injectOpen(incrementUsersPerSec(10.0).times(2).eachLevelLasting(10)) // 12
        ).protocols(httpProtocol); // 13
    }
}
