package tech.powerjob.server.remote.transport.starter;

import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.server.common.PowerJobServerConfigKey;
import tech.powerjob.server.common.utils.PropertyUtils;
import com.google.common.base.Stopwatch;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Properties;

/**
 * vert.x starter
 *
 * @author tjq
 * @since 2021/2/8
 */
@Slf4j
public class VertXStarter {

    public static Vertx vertx;
    @Getter
    private static String address;

    public static void init() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("[PowerJob] PowerJob's vert.x system start to bootstrap...");

        Properties properties = PropertyUtils.getProperties();
        int port = Integer.parseInt(properties.getProperty(PowerJobServerConfigKey.HTTP_PORT, String.valueOf(OmsConstant.SERVER_DEFAULT_HTTP_PORT)));
        String portFromJVM = System.getProperty(PowerJobServerConfigKey.HTTP_PORT);
        if (StringUtils.isNotEmpty(portFromJVM)) {
            port = Integer.parseInt(portFromJVM);
        }
        String localIP = NetUtils.getLocalHost();

        address = localIP + ":" + port;
        log.info("[PowerJob] vert.x server address: {}", address);

        vertx = Vertx.vertx();

        log.info("[PowerJob] PowerJob's vert.x system started successfully, using time {}.", stopwatch);
    }
}
