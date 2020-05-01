package com.github.kfcfans.oms.common.request;

import com.github.kfcfans.oms.common.OmsSerializable;
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
}
