package tech.powerjob.server.remote.transporter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tech.powerjob.remote.framework.transporter.Transporter;

/**
 * ProtocolInfo
 *
 * @author tjq
 * @since 2023/1/21
 */
@Getter
@Setter
@ToString
public class ProtocolInfo {

    private String protocol;

    private String address;

    private transient Transporter transporter;

    public ProtocolInfo(String protocol, String address, Transporter transporter) {
        this.protocol = protocol;
        this.address = address;
        this.transporter = transporter;
    }
}
