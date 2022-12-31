package tech.powerjob.remote.framework.test;

import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.remote.framework.actor.Handler;

/**
 * TestActor
 *
 * @author tjq
 * @since 2022/12/31
 */
@Actor(path = "/test")
public class TestActor {

    public static void simpleStaticMethod() {
    }

    public void simpleMethod() {
    }

    @Handler(path = "/method1")
    public String handlerMethod1() {
        return "1";
    }

    @Handler(path = "/method2")
    public String handlerMethod2(String name) {
        return name;
    }

}
