package com.github.kfcfans.powerjob.server.handler.inner.requests;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 查询 Worker 集群状态
 *
 * @author tjq
 * @since 2020/4/14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendQueryWorkerClusterStatusReq implements OmsSerializable {
    private Long appId;
}
