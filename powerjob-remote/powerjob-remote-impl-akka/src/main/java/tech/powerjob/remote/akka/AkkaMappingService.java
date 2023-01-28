package tech.powerjob.remote.akka;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.common.RemoteConstant;

import java.util.Map;

/**
 * 构建 Actor Mapping
 *
 * @author tjq
 * @since 2023/1/7
 */
public class AkkaMappingService {

    /**
     * Actor's RootPath -> Akka Actor Name
     */
    private static final Map<String, ActorConfig> RP_2_ACTOR_CFG = Maps.newHashMap();

    static {
        addMappingRule(RemoteConstant.S4W_PATH, "server_actor", "w-r-c-d");
        addMappingRule(RemoteConstant.S4S_PATH, "friend_actor", "friend-request-actor-dispatcher");

        addMappingRule(RemoteConstant.WTT_PATH, "task_tracker", "task-tracker-dispatcher");
        addMappingRule(RemoteConstant.WPT_PATH, "processor_tracker", "processor-tracker-dispatcher");
    }

    private static final String DEFAULT_DISPATCH_NAME = "common-dispatcher";

    /**
     * 根据 actor 的 rootPath 获取 Akka Actor Name，不存在改写则使用当前路径
     * @param actorRootPath actorRootPath
     * @return actorName
     */
    public static ActorConfig parseActorName(String actorRootPath) {
        return RP_2_ACTOR_CFG.getOrDefault(actorRootPath,
                new ActorConfig()
                        .setActorName(actorRootPath)
                        .setDispatcherName(DEFAULT_DISPATCH_NAME)
        );
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class ActorConfig {
        private String actorName;
        private String dispatcherName;
    }

    private static void addMappingRule(String newActorPath, String oldActorName, String dispatchName) {
        ActorConfig actorConfig = new ActorConfig()
                .setActorName(oldActorName)
                .setDispatcherName(dispatchName == null ? DEFAULT_DISPATCH_NAME : dispatchName);
        RP_2_ACTOR_CFG.put(newActorPath, actorConfig);
    }
}
