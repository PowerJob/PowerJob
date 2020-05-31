package com.github.kfcfans.oms.server.service.workflow;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.oms.common.OmsException;
import com.github.kfcfans.oms.common.SystemInstanceResult;
import com.github.kfcfans.oms.common.WorkflowInstanceStatus;
import com.github.kfcfans.oms.common.model.WorkflowDAG;
import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.WorkflowInstanceInfoRepository;
import com.github.kfcfans.oms.server.service.instance.InstanceService;
import com.google.common.collect.Queues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;
import java.util.Queue;

/**
 * 工作流实例服务
 *
 * @author tjq
 * @since 2020/5/31
 */
@Slf4j
@Service
public class WorkflowInstanceService {

    @Resource
    private InstanceService instanceService;
    @Resource
    private WorkflowInstanceInfoRepository wfInstanceInfoRepository;


    /**
     * 停止工作流实例
     * @param wfInstanceId 工作流实例ID
     * @param appId 所属应用ID
     */
    public void stopWorkflowInstance(Long wfInstanceId, Long appId) {
        WorkflowInstanceInfoDO wfInstance = wfInstanceInfoRepository.findByWfInstanceId(wfInstanceId).orElseThrow(() -> new IllegalArgumentException("can't find workflow instance by wfInstanceId: " + wfInstanceId));
        if (!Objects.equals(appId, wfInstance.getAppId())) {
            throw new OmsException("Permission Denied!");
        }
        if (!WorkflowInstanceStatus.generalizedRunningStatus.contains(wfInstance.getStatus())) {
            throw new OmsException("already stopped");
        }

        // 修改数据库状态
        wfInstance.setStatus(WorkflowInstanceStatus.STOPPED.getV());
        wfInstance.setResult(SystemInstanceResult.STOPPED_BY_USER);
        wfInstance.setGmtModified(new Date());
        wfInstanceInfoRepository.saveAndFlush(wfInstance);

        // 停止所有已启动且未完成的服务
        WorkflowDAG workflowDAG = JSONObject.parseObject(wfInstance.getDag(), WorkflowDAG.class);
        Queue<WorkflowDAG.Node> queue = Queues.newLinkedBlockingQueue();
        queue.add(workflowDAG.getRoot());
        while (!queue.isEmpty()) {
            WorkflowDAG.Node node = queue.poll();

            if (node.getInstanceId() != null && !node.isFinished()) {
                log.debug("[WfInstance-{}] instance({}) is running, try to stop it now.", wfInstanceId, node.getInstanceId());
                instanceService.stopInstance(node.getInstanceId());
            }
            queue.addAll(node.getSuccessors());
        }

        log.info("[WfInstance-{}] stop workflow instance successfully~", wfInstanceId);
    }

}
