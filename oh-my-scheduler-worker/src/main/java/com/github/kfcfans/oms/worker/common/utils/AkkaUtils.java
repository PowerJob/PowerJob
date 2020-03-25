package com.github.kfcfans.oms.worker.common.utils;

import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.common.AkkaConstant;

/**
 * AKKA 工具类
 *
 * @author tjq
 * @since 2020/3/17
 */
public class AkkaUtils {

    /**
     * akka://<actor system>@<hostname>:<port>/<actor path>
     */
    private static final String AKKA_REMOTE_NODE_PATH = "akka://%s@%s:%d/user/%s";

    private static final String AKKA_SERVER_NODE_PATH = "akka://%s@%s/user/%s";

    public static String getAkkaRemotePath(String ip, String actorName) {
        Integer configPort = OhMyWorker.getConfig().getListeningPort();
        int port = configPort == null ? AkkaConstant.DEFAULT_PORT : configPort;
        return String.format(AKKA_REMOTE_NODE_PATH, AkkaConstant.ACTOR_SYSTEM_NAME, ip, port, actorName);
    }

    public static String getAkkaServerNodePath(String actorName) {
        return String.format(AKKA_SERVER_NODE_PATH, AkkaConstant.SERVER_ACTOR_SYSTEM_NAME, OhMyWorker.getCurrentServer(), actorName);
    }

}
