package com.github.kfcfans.common.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务器要求任务实例停止执行请求
 *
 * @author tjq
 * @since 2020/4/9
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerStopInstanceReq implements Serializable {
    private Long instanceId;
}
