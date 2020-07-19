package com.github.kfcfans.powerjob.server.akka.actors;

import akka.actor.AbstractActor;
import akka.actor.DeadLetter;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理 server 异常信息的 actor
 *
 * @author tjq
 * @since 2020/7/18
 */
@Slf4j
public class ServerTroubleshootingActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeadLetter.class, this::onReceiveDeadLetter)
                .build();
    }

    public void onReceiveDeadLetter(DeadLetter dl) {
        log.warn("[ServerTroubleshootingActor] receive DeadLetter: {}", dl);
    }
}
