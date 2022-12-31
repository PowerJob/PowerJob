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
        return String.format("%s:%d", host, port);
    }
}
