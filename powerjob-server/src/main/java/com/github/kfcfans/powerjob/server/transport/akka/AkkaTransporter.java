package com.github.kfcfans.powerjob.server.transport.akka;

import akka.actor.ActorSelection;
import com.github.kfcfans.powerjob.common.OmsSerializable;
import com.github.kfcfans.powerjob.common.Protocol;
import com.github.kfcfans.powerjob.server.transport.Transporter;
import org.springframework.stereotype.Service;

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
        return OhMyServer.getActorSystemAddress();
    }

    @Override
    public void transfer(String address, OmsSerializable object) {
        ActorSelection taskTrackerActor = OhMyServer.getTaskTrackerActor(address);
        taskTrackerActor.tell(object, null);
    }
}
