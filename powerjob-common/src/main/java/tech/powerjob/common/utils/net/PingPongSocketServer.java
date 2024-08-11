package tech.powerjob.common.utils.net;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.utils.CommonUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 简易服务器
 *
 * @author tjq
 * @since 2024/2/8
 */
@Slf4j
public class PingPongSocketServer implements PingPongServer {

    private Thread thread;

    private ServerSocket serverSocket;

    private volatile boolean terminated = false;

    @Override
    public void initialize(int port) throws Exception{
        serverSocket = new ServerSocket(port);

        thread = new Thread(() -> {
            while (true) {
                if (terminated) {
                    return;
                }
                // 接收连接，如果没有连接，accept() 方法会阻塞
                try (Socket socket = serverSocket.accept();OutputStream outputStream = socket.getOutputStream();) {

                    socket.setSoTimeout(2000);
                    socket.setKeepAlive(false);

                    outputStream.write(PingPongUtils.PONG.getBytes(StandardCharsets.UTF_8));
                    // BufferedReader.readLine() 会等待直到遇到换行符（\n）或回车符（\r\n），才会返回一行内容。如果服务器发送的数据没有这些换行符，readLine() 会一直阻塞，直到超时
                    outputStream.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (Exception e) {
                    if (!terminated) {
                        log.warn("[PingPongSocketServer] process accepted socket failed!", e);
                    }
                }
            }
        }, "PingPongSocketServer-Thread");

        thread.start();
    }

    @Override
    public void close() throws IOException {
        terminated = true;
        CommonUtils.executeIgnoreException(() -> serverSocket.close());
        thread.interrupt();
    }
}
