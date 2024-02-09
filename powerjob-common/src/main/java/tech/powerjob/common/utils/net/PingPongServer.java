package tech.powerjob.common.utils.net;

import java.io.Closeable;

/**
 * socket 服务器，用于进行连通性测试
 *
 * @author tjq
 * @since 2024/2/8
 */
public interface PingPongServer extends Closeable {

    void initialize(int port) throws Exception;
}
