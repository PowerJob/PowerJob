package com.github.kfcfans.oms.worker.actors;

import akka.actor.AbstractActor;
import lombok.extern.slf4j.Slf4j;

/**
 * Worker节点Actor，主要用于和服务器保持心跳
 *
 * @author tjq
 * @since 2020/3/24
 */
@Slf4j
public class WorkerActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(obj -> log.warn("[WorkerActor] receive unknown request: {}.", obj))
                .build();
    }
}
