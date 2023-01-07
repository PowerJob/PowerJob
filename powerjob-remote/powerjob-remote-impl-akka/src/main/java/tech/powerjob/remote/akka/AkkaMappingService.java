package tech.powerjob.remote.akka;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

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
        // TODO: 迁移时写入规则
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
}
