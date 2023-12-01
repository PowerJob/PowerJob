package tech.powerjob.remote.framework.proxy.module;

import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.remote.framework.base.URL;

import java.io.Serializable;

/**
 * 请求对象
 *
 * @author tjq
 * @since 2023/11/15
 */
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
    private String request;

    private Class<?> clz;

    /**
     * {@link ProxyMethod}
     */
    private Integer proxyMethod;

    public URL getUrl() {
        return url;
    }

    public ProxyRequest setUrl(URL url) {
        this.url = url;
        return this;
    }

    public String getRequest() {
        return request;
    }

    public ProxyRequest setRequest(Object request) {
        this.request = JsonUtils.toJSONString(request);
        this.clz = request.getClass();
        return this;
    }

    public Class<?> getClz() {
        return clz;
    }

    public Integer getProxyMethod() {
        return proxyMethod;
    }

    public ProxyRequest setProxyMethod(Integer proxyMethod) {
        this.proxyMethod = proxyMethod;
        return this;
    }

    @Override
    public String toString() {
        return JsonUtils.toJSONString(this);
    }
}
