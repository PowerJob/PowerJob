package tech.powerjob.common.utils.net;

import org.junit.jupiter.api.Test;
import tech.powerjob.common.utils.NetUtils;

/**
 * desc
 *
 * @author tjq
 * @since 2024/2/8
 */
class PingPongSocketServerTest {

    @Test
    void test() throws Exception {

        int port = 8877;

        PingPongSocketServer pingPongSocketServer = new PingPongSocketServer();
        pingPongSocketServer.initialize(port);

        System.out.println("[PingPongSocketServerTest] finished initialize");

        assert PingPongUtils.checkConnectivity(NetUtils.getLocalHost(), port);

        assert !PingPongUtils.checkConnectivity(NetUtils.getLocalHost(), port + 1);

        pingPongSocketServer.close();
        System.out.println("[PingPongSocketServerTest] finished close");
    }
}