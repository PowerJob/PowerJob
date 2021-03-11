package com.github.kfcfans.powerjob.server.remote.transport.starter;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.routing.RoundRobinPool;
import com.github.kfcfans.powerjob.common.OmsConstant;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.server.common.PowerJobServerConfigKey;
import com.github.kfcfans.powerjob.server.common.utils.PropertyUtils;
import com.github.kfcfans.powerjob.server.remote.server.FriendRequestHandler;
import com.github.kfcfans.powerjob.server.remote.worker.handler.impl.WorkerRequestAkkaHandler;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletionStage;

/**
 * 服务端 ActorSystem 启动器
 *
 * @author tjq
 * @since 2020/4/2
 */
@Slf4j
public class AkkaStarter {

    public static ActorSystem actorSystem;
    @Getter
    private static String actorSystemAddress;

    private static final String AKKA_PATH = "akka://%s@%s/user/%s";

    public static void init() {

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("[PowerJob] PowerJob's akka system start to bootstrap...");

        // 忽略了一个问题，机器是没办法访问外网的，除非架设自己的NTP服务器
        // TimeUtils.check();

        // 解析配置文件
        Properties properties = PropertyUtils.getProperties();
        int port = Integer.parseInt(properties.getProperty(PowerJobServerConfigKey.AKKA_PORT, String.valueOf(OmsConstant.SERVER_DEFAULT_AKKA_PORT)));
        String portFromJVM = System.getProperty(PowerJobServerConfigKey.AKKA_PORT);
        if (StringUtils.isNotEmpty(portFromJVM)) {
            log.info("[PowerJob] use port from jvm params: {}", portFromJVM);
            port = Integer.parseInt(portFromJVM);
        }

        // 启动 ActorSystem
        Map<String, Object> overrideConfig = Maps.newHashMap();
        String localIP = NetUtils.getLocalHost();
        overrideConfig.put("akka.remote.artery.canonical.hostname", localIP);
        overrideConfig.put("akka.remote.artery.canonical.port", port);
        actorSystemAddress = localIP + ":" + port;
        log.info("[PowerJob] akka-remote server address: {}", actorSystemAddress);

        Config akkaBasicConfig = ConfigFactory.load(RemoteConstant.SERVER_AKKA_CONFIG_NAME);
        Config akkaFinalConfig = ConfigFactory.parseMap(overrideConfig).withFallback(akkaBasicConfig);
        actorSystem = ActorSystem.create(RemoteConstant.SERVER_ACTOR_SYSTEM_NAME, akkaFinalConfig);

        actorSystem.actorOf(Props.create(WorkerRequestAkkaHandler.class)
                .withDispatcher("akka.server-actor-dispatcher")
                .withRouter(new RoundRobinPool(Runtime.getRuntime().availableProcessors() * 4)), RemoteConstant.SERVER_ACTOR_NAME);
        actorSystem.actorOf(Props.create(FriendRequestHandler.class), RemoteConstant.SERVER_FRIEND_ACTOR_NAME);

        log.info("[PowerJob] PowerJob's akka system started successfully, using time {}.", stopwatch);
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
        String path = String.format(AKKA_PATH, RemoteConstant.WORKER_ACTOR_SYSTEM_NAME, address, RemoteConstant.TASK_TRACKER_ACTOR_NAME);
        return actorSystem.actorSelection(path);
    }

    public static ActorSelection getWorkerActor(String address) {
        String path = String.format(AKKA_PATH, RemoteConstant.WORKER_ACTOR_SYSTEM_NAME, address, RemoteConstant.WORKER_ACTOR_NAME);
        return actorSystem.actorSelection(path);
    }

    /**
     * ASK 其他 powerjob-server，要求 AskResponse 中的 Data 为 String
     * @param address 其他 powerjob-server 的地址（ip:port）
     * @param request 请求
     * @return 返回值 OR 异常
     */
    public static String askFriend(String address, Object request) throws Exception {
        CompletionStage<Object> askCS = Patterns.ask(getFriendActor(address), request, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
        AskResponse askResponse = (AskResponse) askCS.toCompletableFuture().get();
        if (askResponse.isSuccess()) {
            return askResponse.parseDataAsString();
        }
        throw new PowerJobException("remote server process failed:" + askResponse.getMessage());
    }
}
