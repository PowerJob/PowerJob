package tech.powerjob.remote.framework.proxy;

import lombok.Data;
import lombok.experimental.Accessors;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.remote.framework.base.URL;

import java.io.Serializable;

/**
 * 请求对象
 *
 * @author tjq
 * @since 2023/11/15
 */
@Data
@Accessors(chain = true)
public class ProxyRequest implements Serializable {

    public ProxyRequest() {
    }

    /**
     * 真正地访问地址
     */
    private URL url;

    /**
     * 真正地请求数据
     */
    private PowerSerializable request;

    /**
     * {@link ProxyMethod}
     */
    private Integer proxyMethod;

}
