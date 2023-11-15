package tech.powerjob.remote.framework.proxy;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 代理方法
 *
 * @author tjq
 * @since 2023/11/15
 */
@Getter
@AllArgsConstructor
public enum ProxyMethod {

    TELL(1),

    ASK(2)
    ;

    private final Integer v;

    public static ProxyMethod of(Integer vv) {
        for (ProxyMethod proxyMethod : values()) {
            if (proxyMethod.v.equals(vv)) {
                return proxyMethod;
            }
        }
        throw new IllegalArgumentException("can't find ProxyMethod by " + vv);
    }
}
