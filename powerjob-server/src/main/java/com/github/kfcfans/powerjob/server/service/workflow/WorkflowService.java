package com.github.kfcfans.powerjob.server.service.workflow;

import com.alibaba.fastjson.JSON;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.model.PEWorkflowDAG;
import com.github.kfcfans.powerjob.common.request.http.SaveWorkflowRequest;
import com.github.kfcfans.powerjob.server.common.SJ;
import com.github.kfcfans.powerjob.server.common.constans.SwitchableStatus;
import com.github.kfcfans.powerjob.server.common.utils.CronExpression;
import com.github.kfcfans.powerjob.server.common.utils.WorkflowDAGUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.WorkflowInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.WorkflowNodeInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.WorkflowInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.WorkflowNodeInfoRepository;
import com.github.kfcfans.powerjob.server.remote.server.redirector.DesignateServer;
import com.github.kfcfans.powerjob.server.service.instance.InstanceTimeWheelService;
import com.github.kfcfans.powerjob.common.request.http.AddWorkflowNodeRequest;
import com.github.kfcfans.powerjob.common.request.http.ModifyWorkflowNodeRequest;
import com.github.kfcfans.powerjob.common.request.http.SaveWorkflowDAGRequest;
import com.github.kfcfans.powerjob.server.web.response.WorkflowInfoVO;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Workflow 服务
 *
 * @author tjq
 * @author zenggonggu
 * @author Echo009
 * @since 2020/5/26
 */
@Slf4j
@Service
public class WorkflowService {

    @Resource
    private WorkflowInstanceManager workflowInstanceManager;
    @Resource
    private WorkflowInfoRepository workflowInfoRepository;
    @Resource
    private WorkflowNodeInfoRepository workflowNodeInfoRepository;
    @Resource
    private JobInfoRepository jobInfoRepository;

    /**
     * 保存/修改工作流信息
     *
     * 注意这里不会保存 DAG 信息
     *
     * @param req 请求
     * @return 工作流ID
     * @exception ParseException 异常
     */
    public Long saveWorkflow(SaveWorkflowRequest req) throws ParseException {

        req.valid();

        Long wfId = req.getId();
        WorkflowInfoDO wf;
        if (wfId == null) {
            wf = new WorkflowInfoDO();
            wf.setGmtCreate(new Date());
        } else {
            wf = workflowInfoRepository.findById(wfId).orElseThrow(() -> new IllegalArgumentException("can't find workflow by id:" + wfId));
        }

        BeanUtils.copyProperties(req, wf);
        wf.setGmtModified(new Date());
        wf.setStatus(req.isEnable() ? SwitchableStatus.ENABLE.getV() : SwitchableStatus.DISABLE.getV());
        wf.setTimeExpressionType(req.getTimeExpressionType().getV());

        if (req.getNotifyUserIds() != null) {
            wf.setNotifyUserIds(SJ.commaJoiner.join(req.getNotifyUserIds()));
        }

        // 计算 NextTriggerTime
        if (req.getTimeExpressionType() == TimeExpressionType.CRON) {
            CronExpression cronExpression = new CronExpression(req.getTimeExpression());
            Date nextValidTime = cronExpression.getNextValidTimeAfter(new Date());
            wf.setNextTriggerTime(nextValidTime.getTime());
        } else {
            wf.setTimeExpression(null);
        }

        WorkflowInfoDO newEntity = workflowInfoRepository.saveAndFlush(wf);
        return newEntity.getId();
    }

    /**
     * 获取工作流元信息，这里获取到的 DAG 包含节点的完整信息（是否启用、是否允许失败跳过）
     *
     * @param wfId  工作流ID
     * @param appId 应用ID
     * @return 对外输出对象
     */
    public WorkflowInfoVO fetchWorkflow(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = permissionCheck(wfId, appId);
        return convert2WorkflowInfoVO(wfInfo);
    }

    /**
     * 删除工作流（软删除）
     *
     * @param wfId  工作流ID
     * @param appId 所属应用ID
     */
    public void deleteWorkflow(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = permissionCheck(wfId, appId);
        wfInfo.setStatus(SwitchableStatus.DELETED.getV());
        wfInfo.setGmtModified(new Date());
        workflowInfoRepository.saveAndFlush(wfInfo);
    }

    /**
     * 禁用工作流
     *
     * @param wfId  工作流ID
     * @param appId 所属应用ID
     */
    public void disableWorkflow(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = permissionCheck(wfId, appId);
        wfInfo.setStatus(SwitchableStatus.DISABLE.getV());
        wfInfo.setGmtModified(new Date());
        workflowInfoRepository.saveAndFlush(wfInfo);
    }

    /**
     * 启用工作流
     *
     * @param wfId  工作流ID
     * @param appId 所属应用ID
     */
    public void enableWorkflow(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = permissionCheck(wfId, appId);
        wfInfo.setStatus(SwitchableStatus.ENABLE.getV());
        wfInfo.setGmtModified(new Date());
        workflowInfoRepository.saveAndFlush(wfInfo);
    }

    /**
     * 立即运行工作流
     *
     * @param wfId       工作流ID
     * @param appId      所属应用ID
     * @param initParams 启动参数
     * @param delay      延迟时间
     * @return 该 workflow 实例的 instanceId（wfInstanceId）
     */
    @DesignateServer(appIdParameterName = "appId")
    public Long runWorkflow(Long wfId, Long appId, String initParams, Long delay) {

        delay = delay == null ? 0 : delay;
        WorkflowInfoDO wfInfo = permissionCheck(wfId, appId);

        log.info("[WorkflowService-{}] try to run workflow, initParams={},delay={} ms.", wfInfo.getId(), initParams, delay);
        Long wfInstanceId = workflowInstanceManager.create(wfInfo, initParams, System.currentTimeMillis() + delay);
        if (delay <= 0) {
            workflowInstanceManager.start(wfInfo, wfInstanceId);
        } else {
            InstanceTimeWheelService.schedule(wfInstanceId, delay, () -> workflowInstanceManager.start(wfInfo, wfInstanceId));
        }
        return wfInstanceId;
    }

    /**
     * 添加工作流节点
     *
     * @param requestList 工作流节点列表
     * @return 新增的节点信息
     */
    public List<WorkflowNodeInfoDO> addWorkflowNode(List<AddWorkflowNodeRequest> requestList) {
        if (CollectionUtils.isEmpty(requestList)) {
            throw new PowerJobException("requestList must be not null");
        }

        List<WorkflowNodeInfoDO> saveList = new ArrayList<>();
        for (AddWorkflowNodeRequest addWorkflowNodeRequest : requestList) {
            // 校验
            addWorkflowNodeRequest.valid();
            permissionCheck(addWorkflowNodeRequest.getWorkflowId(), addWorkflowNodeRequest.getAppId());

            WorkflowNodeInfoDO workflowNodeInfoDO = new WorkflowNodeInfoDO();
            BeanUtils.copyProperties(addWorkflowNodeRequest, workflowNodeInfoDO);
            // 如果名称为空则默认取任务名称
            if (StringUtils.isEmpty(addWorkflowNodeRequest.getNodeAlias())) {
                JobInfoDO jobInfoDO = jobInfoRepository.findById(addWorkflowNodeRequest.getJobId()).orElseThrow(() -> new IllegalArgumentException("can't find job by id: " + addWorkflowNodeRequest.getJobId()));
                workflowNodeInfoDO.setNodeAlias(jobInfoDO.getJobName());
            }
            Date current = new Date();
            workflowNodeInfoDO.setGmtCreate(current);
            workflowNodeInfoDO.setGmtModified(current);
            saveList.add(workflowNodeInfoDO);
        }
        return workflowNodeInfoRepository.saveAll(saveList);
    }

    /**
     * 修改工作流节点信息
     *
     * @param req 工作流节点信息
     */
    public void modifyWorkflowNode(ModifyWorkflowNodeRequest req) {
        req.valid();
        permissionCheck(req.getWorkflowId(), req.getAppId());
        WorkflowNodeInfoDO workflowNodeInfoDO = workflowNodeInfoRepository.findById(req.getId()).orElseThrow(() -> new IllegalArgumentException("can't find workflow Node by id: " + req.getId()));
        BeanUtils.copyProperties(req, workflowNodeInfoDO);
        workflowNodeInfoDO.setGmtModified(new Date());
        workflowNodeInfoRepository.saveAndFlush(workflowNodeInfoDO);
    }

    /**
     * 保存 DAG 信息
     * 这里会物理删除游离的节点信息
     */
    @Transactional(rollbackOn = Exception.class)
    public void saveWorkflowDAG(SaveWorkflowDAGRequest req) {
        req.valid();
        Long workflowId = req.getId();
        WorkflowInfoDO workflowInfoDO = permissionCheck(workflowId, req.getAppId());
        PEWorkflowDAG dag = req.getDag();
        if (!WorkflowDAGUtils.valid(dag)) {
            throw new PowerJobException("illegal DAG");
        }
        // 注意：这里只会保存图相关的基础信息，nodeId,jobId,jobName(nodeAlias)
        // 其中 jobId，jobName 均以节点中的信息为准
        List<Long> nodeIdList = Lists.newArrayList();
        HashMap<Long, WorkflowNodeInfoDO> nodeIdNodInfoMap = Maps.newHashMap();
        workflowNodeInfoRepository.findByWorkflowId(workflowId).forEach(
                e -> nodeIdNodInfoMap.put(e.getId(), e)
        );
        for (PEWorkflowDAG.Node node : dag.getNodes()) {
            WorkflowNodeInfoDO nodeInfo = nodeIdNodInfoMap.get(node.getNodeId());
            if (nodeInfo == null) {
                throw new PowerJobException("can't find node info by id :" + node.getNodeId());
            }
            if (!workflowInfoDO.getId().equals(nodeInfo.getWorkflowId())) {
                throw new PowerJobException("node workflowId must be same to workflowId");
            }
            // 节点中的别名一定是非空的
            node.setJobName(nodeInfo.getNodeAlias()).setJobId(nodeInfo.getJobId());
            nodeIdList.add(node.getNodeId());
        }
        workflowInfoDO.setPeDAG(JSON.toJSONString(dag));
        workflowInfoRepository.saveAndFlush(workflowInfoDO);
        // 物理删除当前工作流中 “游离” 的节点 (不在工作流 DAG 中的节点)
        int deleteCount = workflowNodeInfoRepository.deleteByWorkflowIdAndIdNotIn(workflowId, nodeIdList);
        log.warn("[WorkflowService-{}]delete {} dissociative nodes of workflow", workflowId, deleteCount);
    }


    private WorkflowInfoVO convert2WorkflowInfoVO(WorkflowInfoDO wfInfo) {

        WorkflowInfoVO res = WorkflowInfoVO.from(wfInfo);

        PEWorkflowDAG dagInfo = null;
        try {
            dagInfo = JSON.parseObject(wfInfo.getPeDAG(), PEWorkflowDAG.class);
        } catch (Exception e) {
            log.warn("[WorkflowService-{}]illegal DAG : {}", wfInfo.getId(), wfInfo.getPeDAG());
        }
        if (dagInfo == null) {
            // 这里直接返回
            return res;
        }

        HashMap<Long, WorkflowNodeInfoDO> nodeIdNodInfoMap = Maps.newHashMap();

        workflowNodeInfoRepository.findByWorkflowId(wfInfo.getId()).forEach(
                e -> nodeIdNodInfoMap.put(e.getId(), e)
        );
        // 填充节点信息
        if (!CollectionUtils.isEmpty(dagInfo.getNodes())) {
            for (PEWorkflowDAG.Node node : dagInfo.getNodes()) {

                WorkflowNodeInfoDO nodeInfo = nodeIdNodInfoMap.get(node.getNodeId());
                if (nodeInfo != null) {
                    node.setEnable(nodeInfo.getEnable())
                            .setSkipWhenFailed(nodeInfo.getSkipWhenFailed())
                            .setJobName(nodeInfo.getNodeAlias())
                            .setJobParams(nodeInfo.getNodeParams());

                } else {
                    // 默认开启 并且 不允许失败跳过
                    node.setEnable(true)
                            .setSkipWhenFailed(false);
                }
            }
        }
        res.setPEWorkflowDAG(dagInfo);
        return res;
    }


    private WorkflowInfoDO permissionCheck(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = workflowInfoRepository.findById(wfId).orElseThrow(() -> new IllegalArgumentException("can't find workflow by id: " + wfId));
        if (!wfInfo.getAppId().equals(appId)) {
            throw new PowerJobException("Permission Denied!can't delete other appId's workflow!");
        }
        return wfInfo;
    }
}
