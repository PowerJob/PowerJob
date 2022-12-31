package tech.powerjob.remote.framework.engine.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.test.TestActor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HandlerFactoryTest
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
class ActorFactoryTest {

    @Test
    void load() {
        final List<ActorInfo> actorInfos = ActorFactory.load();
        log.info("[ActorFactoryTest] actorInfos: {}", actorInfos);

        final Set<String> clzNames = actorInfos.stream().map(x -> x.getActor().getClass().getName()).collect(Collectors.toSet());
        log.info("[ActorFactoryTest] all load clzNames: {}", clzNames);

        assert clzNames.contains(TestActor.class.getName());
    }

}