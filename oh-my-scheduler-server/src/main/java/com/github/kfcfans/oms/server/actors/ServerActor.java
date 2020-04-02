package com.github.kfcfans.oms.server.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.common.request.WorkerHeartbeat;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理 Worker 请求
 *
 * @author tjq
 * @since 2020/3/30
 */
@Slf4j
public class ServerActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WorkerHeartbeat.class, this::onReceiveWorkerHeartbeat)
                .matchAny(obj -> log.warn("[ServerActor] receive unknown request: {}.", obj))
                .build();
    }

    private void onReceiveWorkerHeartbeat(WorkerHeartbeat heartbeat) {



    }
}
