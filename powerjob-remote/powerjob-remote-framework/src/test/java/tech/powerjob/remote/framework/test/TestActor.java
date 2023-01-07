package tech.powerjob.remote.framework.test;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.remote.framework.actor.Handler;

import java.util.Map;

/**
 * TestActor
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
@Actor(path = "/test")
public class TestActor {

    public static void simpleStaticMethod() {
    }

    public void simpleMethod() {
    }

    @Handler(path = "/method1")
    public String handlerMethod1() {
        log.info("[TestActor] handlerMethod1");
        return "1";
    }

    @Handler(path = "/method2")
    public String handlerMethod2(String name) {
        log.info("[TestActor] handlerMethod2 req: {}", name);
        return name;
    }

    @Handler(path = "/returnEmpty")
    public void handlerEmpty(Map<String, Object> req) {
        log.info("[TestActor] handlerEmpty req: {}", req);
    }

}
