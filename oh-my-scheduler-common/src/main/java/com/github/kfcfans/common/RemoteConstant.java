package com.github.kfcfans.common;

/**
 * RemoteConstant
 *
 * @author tjq
 * @since 2020/3/17
 */
public class RemoteConstant {


    /**
     * 顶层Actor（actorSystem名称）
     */
    /* ************************ AKKA CLIENT ************************ */
    public static final int DEFAULT_CLIENT_PORT = 25520;

    public static final String ACTOR_SYSTEM_NAME = "oms";

    public static final String Task_TRACKER_ACTOR_NAME = "task_tracker";
    public static final String PROCESSOR_TRACKER_ACTOR_NAME = "processor_tracker";

    public static final String WORKER_AKKA_CONFIG_NAME = "oms-worker.akka.conf";


    /* ************************ AKKA SERVER ************************ */
    public static final String SERVER_ACTOR_SYSTEM_NAME = "oms-server";
    public static final String SERVER_ACTOR_NAME = "server_actor";
    public static final String SERVER_AKKA_CONFIG_NAME = "oms-worker.akka.conf";


    /* ************************ OTHERS ************************ */
    public static final String EMPTY_ADDRESS = "N/A";
}
