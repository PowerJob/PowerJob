package tech.powerjob.common.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.powerjob.common.PowerSerializable;


/**
 * 服务器要求任务实例停止执行请求
 *
 * @author tjq
 * @since 2020/4/9
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerStopInstanceReq implements PowerSerializable {
    private Long instanceId;
}
