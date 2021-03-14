package tech.powerjob.server.remote.transport.impl;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.server.remote.transport.Transporter;
import tech.powerjob.server.remote.transport.starter.AkkaStarter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * akka transporter
 *
 * @author tjq
 * @since 2021/2/7
 */
@Service
public class AkkaTransporter implements Transporter {

    @Override
    public Protocol getProtocol() {
        return Protocol.AKKA;
    }

    @Override
    public String getAddress() {
        return AkkaStarter.getActorSystemAddress();
    }

    @Override
    public void tell(String address, PowerSerializable object) {
        ActorSelection taskTrackerActor = AkkaStarter.getWorkerActor(address);
        taskTrackerActor.tell(object, null);
    }

    @Override
    public AskResponse ask(String address, PowerSerializable object) throws Exception {
        ActorSelection taskTrackerActor = AkkaStarter.getWorkerActor(address);
        CompletionStage<Object> askCS = Patterns.ask(taskTrackerActor, object, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
        return  (AskResponse) askCS.toCompletableFuture().get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }
}
