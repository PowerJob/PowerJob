package tech.powerjob.worker.common.utils;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.common.RemoteConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * AKKA 工具类
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
public class AkkaUtils {

    /**
     * akka://<actor system>@<hostname>:<port>/<actor path>
     */
    private static final String AKKA_NODE_PATH = "akka://%s@%s/user/%s";

    public static String getAkkaWorkerPath(String address, String actorName) {
        return String.format(AKKA_NODE_PATH, RemoteConstant.WORKER_ACTOR_SYSTEM_NAME, address, actorName);
    }

    public static String getServerActorPath(String serverAddress) {
        if (StringUtils.isEmpty(serverAddress)) {
            return null;
        }
        return String.format(AKKA_NODE_PATH, RemoteConstant.SERVER_ACTOR_SYSTEM_NAME, serverAddress, RemoteConstant.SERVER_ACTOR_NAME);
    }

    /**
     * 可靠传输
     * @param remote 远程 AKKA 节点
     * @param msg 需要传输的对象
     * @return true: 对方接收成功 / false: 对方接收失败（可能传输成功但对方处理失败，需要协同处理 AskResponse 返回值）
     */
    public static boolean reliableTransmit(ActorSelection remote, Object msg) {
        try {
            return easyAsk(remote, msg).isSuccess();
        }catch (Exception e) {
            log.warn("[PowerTransmitter] transmit {} failed", msg, e);
        }
        return false;
    }

    public static AskResponse easyAsk(ActorSelection remote, Object msg) {
        try {
            CompletionStage<Object> ask = Patterns.ask(remote, msg, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
            return (AskResponse) ask.toCompletableFuture().get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }catch (Exception e) {
            throw new PowerJobException(e);
        }
    }

}
