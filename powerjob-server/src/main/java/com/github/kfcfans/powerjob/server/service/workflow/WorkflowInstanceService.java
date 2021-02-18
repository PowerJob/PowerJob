package com.github.kfcfans.powerjob.server.service.workflow;

import com.alibaba.fastjson.JSON;
import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.SystemInstanceResult;
import com.github.kfcfans.powerjob.common.WorkflowInstanceStatus;
import com.github.kfcfans.powerjob.common.model.PEWorkflowDAG;
import com.github.kfcfans.powerjob.common.response.WorkflowInstanceInfoDTO;
import com.github.kfcfans.powerjob.server.common.constans.SwitchableStatus;
import com.github.kfcfans.powerjob.server.common.utils.WorkflowDAGUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.WorkflowInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.WorkflowInstanceInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.WorkflowInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.WorkflowInstanceInfoRepository;
import com.github.kfcfans.powerjob.server.service.instance.InstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static com.github.kfcfans.powerjob.server.common.utils.WorkflowDAGUtils.isNotAllowSkipWhenFailed;

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
    @Resource
    private WorkflowInstanceManager workflowInstanceManager;
    @Resource
    private WorkflowInfoRepository workflowInfoRepository;


    /**
     * 停止工作流实例
     *
     * @param wfInstanceId 工作流实例ID
     * @param appId        所属应用ID
     */
    public void stopWorkflowInstance(Long wfInstanceId, Long appId) {
        WorkflowInstanceInfoDO wfInstance = fetchWfInstance(wfInstanceId, appId);
        if (!WorkflowInstanceStatus.generalizedRunningStatus.contains(wfInstance.getStatus())) {
            throw new PowerJobException("workflow instance already stopped");
        }
        // 停止所有已启动且未完成的服务
        PEWorkflowDAG workflowDAG = JSON.parseObject(wfInstance.getDag(), PEWorkflowDAG.class);
        WorkflowDAGUtils.listRoots(workflowDAG).forEach(node -> {
            try {
                if (node.getInstanceId() != null && InstanceStatus.GENERALIZED_RUNNING_STATUS.contains(node.getStatus())) {
                    log.debug("[WfInstance-{}] instance({}) is running, try to stop it now.", wfInstanceId, node.getInstanceId());
                    node.setStatus(InstanceStatus.STOPPED.getV());
                    node.setResult(SystemInstanceResult.STOPPED_BY_USER);

                    instanceService.stopInstance(node.getInstanceId());
                }
            } catch (Exception e) {
                log.warn("[WfInstance-{}] stop instance({}) failed.", wfInstanceId, JSON.toJSONString(node), e);
            }
        });

        // 修改数据库状态
        wfInstance.setStatus(WorkflowInstanceStatus.STOPPED.getV());
        wfInstance.setResult(SystemInstanceResult.STOPPED_BY_USER);
        wfInstance.setGmtModified(new Date());
        wfInstanceInfoRepository.saveAndFlush(wfInstance);

        log.info("[WfInstance-{}] stop workflow instance successfully~", wfInstanceId);
    }

    /**
     * Add by Echo009 on 2021/02/07
     *
     * @param wfInstanceId 工作流实例ID
     * @param appId        应用ID
     */
    public void retryWorkflowInstance(Long wfInstanceId, Long appId) {
        WorkflowInstanceInfoDO wfInstance = fetchWfInstance(wfInstanceId, appId);
        // 仅允许重试 失败的工作流
        if (WorkflowInstanceStatus.generalizedRunningStatus.contains(wfInstance.getStatus())) {
            throw new PowerJobException("workflow instance is running");
        }
        if (wfInstance.getStatus() == WorkflowInstanceStatus.SUCCEED.getV()) {
            throw new PowerJobException("workflow instance is already successful");
        }
        // 因为 DAG 非法而终止的工作流实例无法重试
        PEWorkflowDAG dag = null;
        try {
            dag = JSON.parseObject(wfInstance.getDag(), PEWorkflowDAG.class);
            if (!WorkflowDAGUtils.valid(dag)) {
                throw new PowerJobException(SystemInstanceResult.INVALID_DAG);
            }

        } catch (Exception e) {
            throw new PowerJobException("you can't retry the workflow instance whose DAG is illegal!");
        }
        // 检查当前工作流信息
        Optional<WorkflowInfoDO> workflowInfo = workflowInfoRepository.findById(wfInstance.getWorkflowId());
        if (!workflowInfo.isPresent() || workflowInfo.get().getStatus() == SwitchableStatus.DISABLE.getV()) {
            throw new PowerJobException("you can't retry the workflow instance whose metadata is unavailable!");
        }
        // 将需要重试的节点状态重置（失败且不允许跳过的）
        for (PEWorkflowDAG.Node node : dag.getNodes()) {
            if (node.getStatus() == InstanceStatus.FAILED.getV()
                    && isNotAllowSkipWhenFailed(node)) {
                node.setStatus(InstanceStatus.WAITING_DISPATCH.getV()).setInstanceId(null);
            }
        }
        wfInstance.setDag(JSON.toJSONString(dag));
        // 更新工作流实例状态，不覆盖实际触发时间
        wfInstance.setStatus(WorkflowInstanceStatus.WAITING.getV());
        wfInstance.setGmtModified(new Date());
        wfInstanceInfoRepository.saveAndFlush(wfInstance);
        // 立即开始
        workflowInstanceManager.start(workflowInfo.get(), wfInstanceId);
    }


    public WorkflowInstanceInfoDTO fetchWorkflowInstanceInfo(Long wfInstanceId, Long appId) {
        WorkflowInstanceInfoDO wfInstance = fetchWfInstance(wfInstanceId, appId);
        WorkflowInstanceInfoDTO dto = new WorkflowInstanceInfoDTO();
        BeanUtils.copyProperties(wfInstance, dto);
        return dto;
    }

    public WorkflowInstanceInfoDO fetchWfInstance(Long wfInstanceId, Long appId) {
        WorkflowInstanceInfoDO wfInstance = wfInstanceInfoRepository.findByWfInstanceId(wfInstanceId).orElseThrow(() -> new IllegalArgumentException("can't find workflow instance by wfInstanceId: " + wfInstanceId));
        if (!Objects.equals(appId, wfInstance.getAppId())) {
            throw new PowerJobException("Permission Denied!");
        }
        return wfInstance;
    }

}
