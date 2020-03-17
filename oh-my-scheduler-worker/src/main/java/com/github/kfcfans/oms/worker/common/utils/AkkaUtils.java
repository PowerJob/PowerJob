package com.github.kfcfans.oms.worker.common.utils;

import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.constants.AkkaConstant;

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
    private static final String AKKA_REMOTE_NODE_PATH = "akka://%s@%s:%d/%s";

    public static String getAkkaRemotePath(String ip, String actorName) {
        return String.format(AKKA_REMOTE_NODE_PATH, AkkaConstant.ACTOR_SYSTEM_NAME, ip, OhMyWorker.getConfig().getListeningPort(), actorName);
    }

}
