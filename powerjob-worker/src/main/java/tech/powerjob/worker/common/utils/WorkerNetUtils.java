package tech.powerjob.worker.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.HttpUtils;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.common.utils.net.PingPongServer;
import tech.powerjob.common.utils.net.PingPongSocketServer;

import java.util.List;

/**
 * PowerJob Worker 专用的网络工具类
 *
 * @author tjq
 * @since 2024/2/8
 */
@Slf4j
public class WorkerNetUtils {

    private static final String SERVER_CONNECTIVITY_CHECK_URL_PATTERN = "http://%s/server/checkConnectivity?targetIp=%s&targetPort=%d";

    /**
     * 多网卡情况下，解析可与 server 通讯的本地 IP 地址
     * @param port 目标端口
     * @param serverAddress server 服务地址
     * @return 本机IP
     */
    public static String parseLocalBindIp(int port, List<String> serverAddress) {
        PingPongServer pingPongServer = null;

        try {
            pingPongServer = new PingPongSocketServer();
            pingPongServer.initialize(port);
            log.info("[WorkerNetUtils] initialize PingPongSocketServer successfully~");
        } catch (Exception e) {
            log.warn("[WorkerNetUtils] PingPongSocketServer failed to start, which may result in an incorrectly bound IP, please pay attention to the initialize log.", e);
        }

        String localHostWithNetworkInterfaceChecker = NetUtils.getLocalHostWithNetworkInterfaceChecker(((networkInterface, inetAddress) -> {

            if (inetAddress == null) {
                return false;
            }

            String workerIp = inetAddress.getHostAddress();
            for (String address : serverAddress) {
                String url = String.format(SERVER_CONNECTIVITY_CHECK_URL_PATTERN, address, workerIp, port);
                try {
                    String resp = HttpUtils.get(url);
                    log.info("[WorkerNetUtils] check connectivity by url[{}], response: {}", url, resp);
                    if (StringUtils.isNotEmpty(resp)) {
                        ResultDTO<?> resultDTO = JsonUtils.parseObject(resp, ResultDTO.class);
                        return Boolean.TRUE.toString().equalsIgnoreCase(String.valueOf(resultDTO.getData()));
                    }
                } catch (Exception ignore) {
                }
            }
            return false;
        }));

        if (pingPongServer != null) {
            try {
                pingPongServer.close();
                log.info("[WorkerNetUtils] close PingPongSocketServer successfully~");
            } catch (Exception e) {
                log.warn("[WorkerNetUtils] close PingPongSocketServer failed!", e);
            }
        }

        return localHostWithNetworkInterfaceChecker;
    }

}
