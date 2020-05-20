package com.github.kfcfans.oms.common.request.http;

import com.github.kfcfans.oms.common.OmsSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Worker需要部署容器，主动向Server请求信息
 *
 * @author tjq
 * @since 2020/5/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerNeedDeployContainerRequest implements OmsSerializable {
    private Long containerId;
}
