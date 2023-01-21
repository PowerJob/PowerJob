package tech.powerjob.server.remote.tp;

import lombok.Getter;
import lombok.ToString;
import tech.powerjob.remote.framework.transporter.Transporter;

/**
 * ProtocolInfo
 *
 * @author tjq
 * @since 2023/1/21
 */
@Getter
@ToString
public class ProtocolInfo {

    private final String protocol;

    private final String address;

    private final transient Transporter transporter;

    public ProtocolInfo(String protocol, String address, Transporter transporter) {
        this.protocol = protocol;
        this.address = address;
        this.transporter = transporter;
    }
}
