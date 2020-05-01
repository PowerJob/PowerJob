package com.github.kfcfans.oms.server.akka;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.github.kfcfans.oms.common.RemoteConstant;
import com.github.kfcfans.oms.common.utils.NetUtils;
import com.github.kfcfans.oms.server.akka.actors.FriendActor;
import com.github.kfcfans.oms.server.akka.actors.ServerActor;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 服务端 ActorSystem 启动器
 *
 * @author tjq
 * @since 2020/4/2
 */
@Slf4j
public class OhMyServer {

    public static ActorSystem actorSystem;
    @Getter
    private static String actorSystemAddress;

    private static final String AKKA_PATH = "akka://%s@%s/user/%s";

    public static void init() {

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("[OhMyServer] OhMyServer's akka system start to bootstrap...");
        // 1. 启动 ActorSystem
        Map<String, Object> overrideConfig = Maps.newHashMap();
        String localIP = NetUtils.getLocalHost();
        int port = NetUtils.getAvailablePort();
        overrideConfig.put("akka.remote.artery.canonical.hostname", localIP);
        overrideConfig.put("akka.remote.artery.canonical.port", port);
        actorSystemAddress = localIP + ":" + port;
        log.info("[OhMyWorker] akka-remote server address: {}", actorSystemAddress);

        Config akkaBasicConfig = ConfigFactory.load(RemoteConstant.SERVER_AKKA_CONFIG_NAME);
        Config akkaFinalConfig = ConfigFactory.parseMap(overrideConfig).withFallback(akkaBasicConfig);
        actorSystem = ActorSystem.create(RemoteConstant.SERVER_ACTOR_SYSTEM_NAME, akkaFinalConfig);

        actorSystem.actorOf(Props.create(ServerActor.class), RemoteConstant.SERVER_ACTOR_NAME);
        actorSystem.actorOf(Props.create(FriendActor.class), RemoteConstant.SERVER_FRIEND_ACTOR_NAME);

        log.info("[OhMyServer] OhMyServer's akka system start successfully, using time {}.", stopwatch);
    }

    /**
     * 获取 ServerActor 的 ActorSelection
     * @param address IP:port
     * @return ActorSelection
     */
    public static ActorSelection getFriendActor(String address) {
        String path = String.format(AKKA_PATH, RemoteConstant.SERVER_ACTOR_SYSTEM_NAME, address, RemoteConstant.SERVER_FRIEND_ACTOR_NAME);
        return actorSystem.actorSelection(path);
    }

    public static ActorSelection getTaskTrackerActor(String address) {
        String path = String.format(AKKA_PATH, RemoteConstant.WORKER_ACTOR_SYSTEM_NAME, address, RemoteConstant.Task_TRACKER_ACTOR_NAME);
        return actorSystem.actorSelection(path);
    }
}
