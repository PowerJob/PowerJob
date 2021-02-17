package com.github.kfcfans.powerjob.common.request;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import com.github.kfcfans.powerjob.common.ProtocolConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 服务器要求任务实例停止执行请求
 *
 * @author tjq
 * @since 2020/4/9
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerStopInstanceReq implements OmsSerializable {
    private Long instanceId;

    @Override
    public String path() {
        return ProtocolConstant.WORKER_PATH_STOP_INSTANCE;
    }
}
