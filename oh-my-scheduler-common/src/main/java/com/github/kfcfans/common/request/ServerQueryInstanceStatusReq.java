package com.github.kfcfans.common.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务器查询实例运行状态，需要返回详细的运行数据
 *
 * @author tjq
 * @since 2020/4/10
 */
@Data
public class ServerQueryInstanceStatusReq implements Serializable {
    private Long instanceId;
}
