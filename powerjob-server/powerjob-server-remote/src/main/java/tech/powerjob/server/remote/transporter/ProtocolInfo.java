package tech.powerjob.server.remote.transporter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tech.powerjob.common.PowerJobDKey;
import tech.powerjob.common.utils.PropertyUtils;
import tech.powerjob.remote.framework.base.Address;
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

    /**
     * 外部地址，当存在 NAT 等场景时，需要下发该地址到 worker
     */
    private String externalAddress;

    private transient Transporter transporter;

    /**
     * 序列化需要，必须存在无参构造方法！严禁删除
     */
    public ProtocolInfo() {
    }

    public ProtocolInfo(String protocol, String host, int port, Transporter transporter) {
        this.protocol = protocol;
        this.transporter = transporter;

        this.address = Address.toFullAddress(host, port);

        // 处理外部地址
        String externalAddress = PropertyUtils.readProperty(PowerJobDKey.NT_EXTERNAL_ADDRESS, host);

        // 考虑到不同协议 port 理论上不一样，server 需要为每个单独的端口配置映射，规则为 powerjob.network.external.port.${协议}，比如 powerjob.network.external.port.http
        String externalPortByProtocolKey = PowerJobDKey.NT_EXTERNAL_PORT.concat(".").concat(protocol.toLowerCase());
        // 大部分用户只使用一种协议，在此处做兼容处理降低答疑量和提高易用性（如果用户有多种协议，只有被转发的协议能成功通讯）
        String externalPort = PropertyUtils.readProperty(externalPortByProtocolKey, PropertyUtils.readProperty(PowerJobDKey.NT_EXTERNAL_PORT, String.valueOf(port)));
        this.externalAddress = Address.toFullAddress(externalAddress, Integer.parseInt(externalPort));
    }
}
