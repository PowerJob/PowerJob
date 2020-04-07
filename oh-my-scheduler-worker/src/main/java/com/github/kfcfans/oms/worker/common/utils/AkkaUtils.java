package com.github.kfcfans.oms.worker.common.utils;

import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.common.RemoteConstant;

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
    private static final String AKKA_NODE_PATH = "akka://%s@%s/user/%s";

    public static String getAkkaWorkerPath(String address, String actorName) {
        return String.format(AKKA_NODE_PATH, RemoteConstant.WORKER_ACTOR_SYSTEM_NAME, address, actorName);
    }

    public static String getAkkaServerPath(String actorName) {
        return String.format(AKKA_NODE_PATH, RemoteConstant.SERVER_ACTOR_SYSTEM_NAME, OhMyWorker.getCurrentServer(), actorName);
    }

}
