package tech.powerjob.remote.framework.cs;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.ServerType;

import java.io.Serializable;

/**
 * CSInitializerConfig
 *
 * @author tjq
 * @since 2022/12/31
 */
@Getter
@Setter
@Accessors(chain = true)
public class CSInitializerConfig implements Serializable {

    /**
     * 需要绑定的地址（本地）
     */
    private Address bindAddress;
    /**
     * 外部地址（需要 NAT 等情况存在）
     */
    private Address externalAddress;

    private ServerType serverType;
}
