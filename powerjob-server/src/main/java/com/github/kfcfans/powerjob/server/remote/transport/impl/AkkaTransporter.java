package com.github.kfcfans.powerjob.server.remote.transport.impl;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.powerjob.common.OmsSerializable;
import com.github.kfcfans.powerjob.common.Protocol;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.github.kfcfans.powerjob.server.remote.transport.Transporter;
import com.github.kfcfans.powerjob.server.remote.transport.starter.AkkaStarter;
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
    public void tell(String address, OmsSerializable object) {
        ActorSelection taskTrackerActor = AkkaStarter.getTaskTrackerActor(address);
        taskTrackerActor.tell(object, null);
    }

    @Override
    public AskResponse ask(String address, OmsSerializable object) throws Exception {
        ActorSelection taskTrackerActor = AkkaStarter.getTaskTrackerActor(address);
        CompletionStage<Object> askCS = Patterns.ask(taskTrackerActor, object, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
        return  (AskResponse) askCS.toCompletableFuture().get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }
}
