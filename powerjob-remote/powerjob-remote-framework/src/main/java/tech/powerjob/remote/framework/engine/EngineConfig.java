package tech.powerjob.remote.framework.engine;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.ServerType;

import java.io.Serializable;
import java.util.Set;

/**
 * EngineConfig
 *
 * @author tjq
 * @since 2022/12/31
 */
@Data
@Accessors(chain = true)
public class EngineConfig implements Serializable {

    private ServerType serverType;
    /**
     * 需要启动的引擎类型
     */
    private Set<String> types;
    /**
     * 绑定的本地地址
     */
    private Address bindAddress;
}
