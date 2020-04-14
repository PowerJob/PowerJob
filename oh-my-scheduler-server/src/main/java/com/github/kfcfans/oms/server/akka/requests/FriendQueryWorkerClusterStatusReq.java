package com.github.kfcfans.oms.server.akka.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 查询 Worker 集群状态
 *
 * @author tjq
 * @since 2020/4/14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendQueryWorkerClusterStatusReq implements Serializable {
    private Long appId;
}
