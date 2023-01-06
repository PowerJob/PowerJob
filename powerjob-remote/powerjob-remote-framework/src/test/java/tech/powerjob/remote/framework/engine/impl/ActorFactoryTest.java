package tech.powerjob.remote.framework.engine.impl;

import com.google.common.collect.Lists;
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
        ActorFactory.load(Lists.newArrayList(new TestActor()));
    }

    @Test
    void testSuitPath() {
        final String testPath1 = ActorFactory.suitPath("/test");
        final String testPath2 = ActorFactory.suitPath("test");
        log.info("[ActorFactoryTest] testPath1: {}, testPath2: {}", testPath1, testPath2);
        assert testPath1.equals(testPath2);
    }

}