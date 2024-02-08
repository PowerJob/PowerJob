package tech.powerjob.common.utils.net;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * socket 连通性助手
 *
 * @author tjq
 * @since 2024/2/8
 */
@Slf4j
public class PingPongUtils {

    static final String PING = "ping";
    static final String PONG = "pong";

    /**
     * 验证目标 IP 和 端口的连通性
     * @param targetIp 目标 IP
     * @param targetPort 目标端口
     * @return true or false
     */
    public static boolean checkConnectivity(String targetIp, int targetPort) {

        try (Socket s = new Socket(targetIp, targetPort);InputStream is = s.getInputStream();OutputStream os = s.getOutputStream();BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            // 发送 PING 请求
            os.write(PING.getBytes(StandardCharsets.UTF_8));
            os.flush();

            //读取服务器返回的消息
            String content = br.readLine();

            if (PONG.equalsIgnoreCase(content)) {
                return true;
            }
        } catch (UnknownHostException e) {
            log.warn("[SocketConnectivityUtils] unknown host: {}:{}", targetIp, targetPort);
        } catch (IOException e) {
            log.warn("[SocketConnectivityUtils] IOException: {}:{}, msg: {}", targetIp, targetPort, ExceptionUtils.getMessage(e));
        } catch (Exception e) {
            log.error("[SocketConnectivityUtils] unknown exception for check ip: {}:{}", targetIp, targetPort, e);
        }

        return false;
    }
}
