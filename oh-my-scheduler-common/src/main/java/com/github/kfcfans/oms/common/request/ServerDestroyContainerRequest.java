package com.github.kfcfans.oms.common.request;

import com.github.kfcfans.oms.common.OmsSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务器销毁容器请求
 *
 * @author tjq
 * @since 2020/5/18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerDestroyContainerRequest implements OmsSerializable {
    private String containerName;
}
