package tech.powerjob.server.remote.transport;

import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.response.AskResponse;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    private static final Map<Protocol, String> protocol2Address = Maps.newHashMap();

    @Getter
    private final Map<Protocol, Transporter> protocol2Transporter = Maps.newConcurrentMap();

    @Autowired
    public TransportService(List<Transporter> transporters) {
        transporters.forEach(t -> {
            log.info("[TransportService] Transporter[protocol:{},address:{}] registration successful!", t.getProtocol(), t.getAddress());
            protocol2Transporter.put(t.getProtocol(), t);
            protocol2Address.put(t.getProtocol(), t.getAddress());
        });
    }

    public void tell(Protocol protocol, String address, PowerSerializable object) {
        getTransporter(protocol).tell(address, object);
    }

    public AskResponse ask(Protocol protocol, String address, PowerSerializable object) throws Exception {

        return getTransporter(protocol).ask(address, object);
    }

    public Transporter getTransporter(Protocol protocol) {
        Transporter transporter = protocol2Transporter.get(protocol);
        if (transporter == null) {
            log.error("[TransportService] can't find transporter by protocol[{}], this is a bug!", protocol);
            throw new UnknownProtocolException("can't find transporter by protocol: " + protocol);
        }
        return transporter;
    }

    public static class UnknownProtocolException extends RuntimeException {
        public UnknownProtocolException(String message) {
            super(message);
        }
    }

    public static Map<Protocol, String> getAllAddress() {
        return protocol2Address;
    }
}
