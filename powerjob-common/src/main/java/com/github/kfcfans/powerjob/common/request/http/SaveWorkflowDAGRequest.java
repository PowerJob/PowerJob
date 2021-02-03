package com.github.kfcfans.powerjob.common.request.http;

import com.github.kfcfans.powerjob.common.model.PEWorkflowDAG;
import com.github.kfcfans.powerjob.common.utils.CommonUtils;
import lombok.Data;

/**
 * 新增工作流节点信息请求
 *
 * @author zenggonggu
 * @since 2021/02/02
 */
@Data
public class SaveWorkflowDAGRequest {
    /** workflowId **/
    private Long id;

    /** 所属应用ID（OpenClient不需要用户填写，自动填充）*/
    private Long appId;

    /** 点线表示法*/
    private PEWorkflowDAG dag;

    public void valid() {
        CommonUtils.requireNonNull(this.appId, "appId can't be empty");
        CommonUtils.requireNonNull(this.id, "workflowId can't be empty");
        CommonUtils.requireNonNull(dag, "dag can't be empty");
        CommonUtils.requireNonNull(dag.getNodes(), "nodes can't be empty");
        CommonUtils.requireNonNull(dag.getEdges(), "edges can't be empty");
        for (PEWorkflowDAG.Node node : dag.getNodes()) {
            // 清空其他信息
            node.setEnable(null).setSkipWhenFailed(null).setInstanceId(null).setJobName(null).setResult(null);
        }
    }
}
