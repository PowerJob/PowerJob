package tech.powerjob.remote.framework.proxy.module;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 代理结果
 *
 * @author tjq
 * @since 2023/11/15
 */
@Data
@Accessors(chain = true)
public class ProxyResult implements Serializable {

    public ProxyResult() {
    }

    private boolean success;

    private String data;

    private String msg;
}
