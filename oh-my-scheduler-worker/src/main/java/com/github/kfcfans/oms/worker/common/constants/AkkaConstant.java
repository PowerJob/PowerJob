package com.github.kfcfans.oms.worker.common.constants;

/**
 * akka actor 名称
 *
 * @author tjq
 * @since 2020/3/17
 */
public class AkkaConstant {

    /**
     * 默认端口
     */
    public static final int DEFAULT_PORT = 25520;

    /**
     * 顶层Actor（actorSystem名称）
     */
    public static final String ACTOR_SYSTEM_NAME = "oms";

    public static final String Task_TRACKER_ACTOR_NAME = "task_tracker";
    public static final String PROCESSOR_TRACKER_ACTOR_NAME = "processor_tracker";

    public static final String AKKA_CONFIG_NAME = "oms-akka-application.conf";

}
