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

    private Address bindAddress;

    private ServerType serverType;
}
