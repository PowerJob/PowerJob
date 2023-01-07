package tech.powerjob.remote.akka;

import akka.actor.AbstractActor;
import akka.actor.DeadLetter;
import lombok.extern.slf4j.Slf4j;

/**
 * TroubleshootingActor
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
public class AkkaTroubleshootingActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeadLetter.class, this::onReceiveDeadLetter)
                .build();
    }

    public void onReceiveDeadLetter(DeadLetter dl) {
        log.warn("[PowerJob-AKKA] receive DeadLetter: {}", dl);
    }
}
