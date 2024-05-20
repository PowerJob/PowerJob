package tech.powerjob.common.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.powerjob.common.PowerSerializable;

/**
 * 服务器查询实例运行状态，需要返回详细的运行数据
 *
 * @author tjq
 * @since 2020/4/10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerQueryInstanceStatusReq implements PowerSerializable {
    private Long instanceId;

    /**
     * 自定义查询
     * 针对高阶用户，直接开放底库查询，便于运维和排查问题
     * 此处只传递查询条件，前置拼接 select *，后置拼接 limit
     */
    private String customQuery;

}
