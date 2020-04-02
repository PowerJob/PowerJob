package com.github.kfcfans.oms.server.core.actors;

import akka.actor.ActorSystem;
import akka.actor.Props;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.common.utils.NetUtils;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
    private static String actorSystemAddress;

    public void init() {

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
        actorSystem = ActorSystem.create(RemoteConstant.SERVER_ACTOR_NAME, akkaFinalConfig);

        actorSystem.actorOf(Props.create(ServerActor.class), RemoteConstant.SERVER_ACTOR_NAME);

    }
}
