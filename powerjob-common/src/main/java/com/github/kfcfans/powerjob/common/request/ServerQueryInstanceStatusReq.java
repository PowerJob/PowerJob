package com.github.kfcfans.powerjob.common.request;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import com.github.kfcfans.powerjob.common.ProtocolConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务器查询实例运行状态，需要返回详细的运行数据
 *
 * @author tjq
 * @since 2020/4/10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerQueryInstanceStatusReq implements OmsSerializable {
    private Long instanceId;

    @Override
    public String path() {
        return ProtocolConstant.WORKER_PATH_QUERY_INSTANCE_INFO;
    }
}
