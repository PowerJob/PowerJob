package tech.powerjob.remote.framework.engine.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.actor.HandlerInfo;
import tech.powerjob.remote.framework.test.TestActor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HandlerFactoryTest
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
class HandlerFactoryTest {

    @Test
    void load() {
        final List<HandlerInfo> handlerInfos = HandlerFactory.load();
        log.info("[HandlerFactoryTest] handlerInfos: {}", handlerInfos);
    }

    @Test
    void loadActorInfos() {
        final List<ActorInfo> actorInfos = HandlerFactory.loadActorInfos();
        final Set<String> clzNames = actorInfos.stream().map(x -> x.getActor().getClass().getName()).collect(Collectors.toSet());
        log.info("[HandlerFactoryTest] all load clzNames: {}", clzNames);

        assert clzNames.contains(TestActor.class.getName());
    }
}