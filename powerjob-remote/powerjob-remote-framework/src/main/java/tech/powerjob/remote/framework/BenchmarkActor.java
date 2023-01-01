package tech.powerjob.remote.framework;

import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.remote.framework.actor.Handler;

/**
 * 基准测试
 *
 * @author tjq
 * @since 2023/1/1
 */
@Actor(path = "benchmark")
public class BenchmarkActor {

    @Handler(path = "simple")
    public String simpleRequest(String k) {
        return k;
    }

}
