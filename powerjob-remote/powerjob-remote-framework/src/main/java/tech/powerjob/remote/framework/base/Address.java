package tech.powerjob.remote.framework.base;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 地址
 *
 * @author tjq
 * @since 2022/12/31
 */
@Getter
@Setter
@Accessors(chain = true)
public class Address implements Serializable {
    private String host;
    private int port;

    public String toFullAddress() {
        return toFullAddress(host, port);
    }

    public static Address fromIpv4(String ipv4) {
        String[] split = ipv4.split(":");
        return new Address()
                .setHost(split[0])
                .setPort(Integer.parseInt(split[1]));
    }

    public static String toFullAddress(String host, int port) {
        return String.format("%s:%d", host, port);
    }

    @Override
    public String toString() {
        return toFullAddress();
    }
}
