package tech.powerjob.server.remote.server.election;

import tech.powerjob.common.PowerSerializable;
import lombok.Data;


/**
 * 检测目标机器是否存活
 *
 * @author tjq
 * @since 2020/4/5
 */
@Data
public class Ping implements PowerSerializable {
    private long currentTime;
}
