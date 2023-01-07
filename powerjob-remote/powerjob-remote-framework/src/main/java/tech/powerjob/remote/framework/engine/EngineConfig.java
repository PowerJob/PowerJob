package tech.powerjob.remote.framework.engine;

import lombok.Data;
import lombok.experimental.Accessors;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.ServerType;

import java.io.Serializable;
import java.util.List;

/**
 * EngineConfig
 *
 * @author tjq
 * @since 2022/12/31
 */
@Data
@Accessors(chain = true)
public class EngineConfig implements Serializable {

    /**
     * 服务类型
     */
    private ServerType serverType;
    /**
     * 需要启动的引擎类型
     */
    private String type;
    /**
     * 绑定的本地地址
     */
    private Address bindAddress;
    /**
     * actor实例，交由使用侧自己实例化以便自行注入各种 bean
     */
    private List<Object> actorList;
}
