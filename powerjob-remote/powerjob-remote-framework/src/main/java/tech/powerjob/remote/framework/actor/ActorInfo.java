package tech.powerjob.remote.framework.actor;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * ActorInfo
 *
 * @author tjq
 * @since 2022/12/31
 */
@Getter
@Setter
@Accessors(chain = true)
public class ActorInfo {

    private Object actor;

    private Actor anno;

    private List<HandlerInfo> handlerInfos;

}
