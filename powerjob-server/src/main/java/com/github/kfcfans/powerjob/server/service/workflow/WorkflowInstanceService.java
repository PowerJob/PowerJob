package com.github.kfcfans.powerjob.server.service.workflow;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.SystemInstanceResult;
import com.github.kfcfans.powerjob.common.WorkflowInstanceStatus;
import com.github.kfcfans.powerjob.common.model.PEWorkflowDAG;
import com.github.kfcfans.powerjob.common.response.WorkflowInstanceInfoDTO;
import com.github.kfcfans.powerjob.server.common.utils.WorkflowDAGUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.WorkflowInstanceInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.WorkflowInstanceInfoRepository;
import com.github.kfcfans.powerjob.server.service.instance.InstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;

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
        WorkflowInstanceInfoDO wfInstance = fetchWfInstance(wfInstanceId, appId);
        if (!WorkflowInstanceStatus.generalizedRunningStatus.contains(wfInstance.getStatus())) {
            throw new PowerJobException("workflow instance already stopped");
        }
        // 停止所有已启动且未完成的服务
        PEWorkflowDAG workflowDAG = JSONObject.parseObject(wfInstance.getDag(), PEWorkflowDAG.class);
        WorkflowDAGUtils.listRoots(workflowDAG).forEach(node -> {
            try {
                if (node.getInstanceId() != null && InstanceStatus.generalizedRunningStatus.contains(node.getStatus())) {
                    log.debug("[WfInstance-{}] instance({}) is running, try to stop it now.", wfInstanceId, node.getInstanceId());
                    node.setStatus(InstanceStatus.STOPPED.getV());
                    node.setResult(SystemInstanceResult.STOPPED_BY_USER);

                    instanceService.stopInstance(node.getInstanceId());
                }
            }catch (Exception e) {
                log.warn("[WfInstance-{}] stop instance({}) failed.", wfInstanceId, JSONObject.toJSONString(node), e);
            }
        });

        // 修改数据库状态
        wfInstance.setStatus(WorkflowInstanceStatus.STOPPED.getV());
        wfInstance.setResult(SystemInstanceResult.STOPPED_BY_USER);
        wfInstance.setGmtModified(new Date());
        wfInstanceInfoRepository.saveAndFlush(wfInstance);

        log.info("[WfInstance-{}] stop workflow instance successfully~", wfInstanceId);
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
