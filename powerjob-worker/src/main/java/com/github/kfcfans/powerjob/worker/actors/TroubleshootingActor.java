package com.github.kfcfans.powerjob.worker.actors;

import akka.actor.AbstractActor;
import akka.actor.DeadLetter;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理系统异常的 Actor
 *
 * @author 朱八
 * @since 2020/7/16
 */
@Slf4j
public class TroubleshootingActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeadLetter.class, this::onReceiveDeadLetter)
                .build();
    }

    public void onReceiveDeadLetter(DeadLetter dl) {
        log.warn("[TroubleshootingActor] receive DeadLetter: {}", dl);
    }
}
