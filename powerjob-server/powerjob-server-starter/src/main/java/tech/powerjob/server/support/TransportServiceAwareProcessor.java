package tech.powerjob.server.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.server.remote.aware.TransportServiceAware;
import tech.powerjob.server.remote.transporter.TransportService;

import java.util.List;

/**
 * TransportServiceAwareProcessor
 *
 * @author tjq
 * @since 2023/3/4
 */
@Slf4j
@Component
public class TransportServiceAwareProcessor {

    public TransportServiceAwareProcessor(TransportService transportService, List<TransportServiceAware> transportServiceAwares) {
        log.info("[TransportServiceAwareProcessor] current transportService: {}", transportService);
        transportServiceAwares.forEach(aware -> {
            aware.setTransportService(transportService);
            log.info("[TransportServiceAwareProcessor] set transportService for: {} successfully", aware);
        });
    }
}
