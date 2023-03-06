package tech.powerjob.server.remote.aware;

import tech.powerjob.server.common.aware.PowerJobAware;
import tech.powerjob.server.remote.transporter.TransportService;

/**
 * TransportServiceAware
 *
 * @author tjq
 * @since 2023/3/4
 */
public interface TransportServiceAware extends PowerJobAware {

    void setTransportService(TransportService transportService);
}
