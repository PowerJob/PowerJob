package tech.powerjob.common.request;

import tech.powerjob.common.PowerSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * worker 查询 执行器集群（动态上线需要）
 *
 * @author tjq
 * @since 10/17/20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerQueryExecutorClusterReq implements PowerSerializable {
    private Long appId;
    private Long jobId;
}
