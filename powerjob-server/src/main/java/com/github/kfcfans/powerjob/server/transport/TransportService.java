package com.github.kfcfans.powerjob.server.transport;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import com.github.kfcfans.powerjob.common.Protocol;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * TransportService
 *
 * @author tjq
 * @since 2021/2/7
 */
@Slf4j
@Service
public class TransportService {

    private final Map<Protocol, Transporter> protocol2Transporter = Maps.newConcurrentMap();

    @Autowired
    public TransportService(List<Transporter> transporters) {
        transporters.forEach(t -> {
            log.info("[TransportService] Transporter[protocol:{},address:{}] registration successful!", t.getProtocol(), t.getAddress());
            protocol2Transporter.put(t.getProtocol(), t);
        });
    }

    public void tell(Protocol protocol, String address, OmsSerializable object) {
        Transporter transporter = protocol2Transporter.get(protocol);
        if (transporter == null) {
            log.error("[TransportService] can't find transporter by protocol[{}], this is a bug!", protocol);
            return;
        }
        transporter.tell(address, object);
    }

    public AskResponse ask(Protocol protocol, String address, OmsSerializable object) throws Exception {
        Transporter transporter = protocol2Transporter.get(protocol);
        if (transporter == null) {
            log.error("[TransportService] can't find transporter by protocol[{}], this is a bug!", protocol);
            throw new IOException("can't find transporter by protocol: " + protocol);
        }
        return transporter.ask(address, object);
    }
}
