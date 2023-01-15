package tech.powerjob.server.core.workflow;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.model.LifeCycle;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.common.request.http.SaveWorkflowNodeRequest;
import tech.powerjob.common.request.http.SaveWorkflowRequest;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.common.timewheel.holder.InstanceTimeWheelService;
import tech.powerjob.server.core.scheduler.TimingStrategyService;
import tech.powerjob.server.core.service.NodeValidateService;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAG;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAGUtils;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowNodeInfoDO;
import tech.powerjob.server.persistence.remote.repository.WorkflowInfoRepository;
import tech.powerjob.server.persistence.remote.repository.WorkflowNodeInfoRepository;
import tech.powerjob.server.remote.server.redirector.DesignateServer;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.*;

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
    private NodeValidateService nodeValidateService;
    @Resource
    private TimingStrategyService timingStrategyService;

    /**
     * 保存/修改工作流信息
     * <p>
     * 注意这里不会保存 DAG 信息
     *
     * @param req 请求
     * @return 工作流ID
     */
    @Transactional(rollbackOn = Exception.class)
    public Long saveWorkflow(SaveWorkflowRequest req) {

        req.valid();

        Long wfId = req.getId();
        WorkflowInfoDO wf;
        if (wfId == null) {
            wf = new WorkflowInfoDO();
            wf.setGmtCreate(new Date());
        } else {
            Long finalWfId = wfId;
            wf = workflowInfoRepository.findById(wfId).orElseThrow(() -> new IllegalArgumentException("can't find workflow by id:" + finalWfId));
        }

        BeanUtils.copyProperties(req, wf);
        wf.setGmtModified(new Date());
        wf.setStatus(req.isEnable() ? SwitchableStatus.ENABLE.getV() : SwitchableStatus.DISABLE.getV());
        wf.setTimeExpressionType(req.getTimeExpressionType().getV());

        if (req.getNotifyUserIds() != null) {
            wf.setNotifyUserIds(SJ.COMMA_JOINER.join(req.getNotifyUserIds()));
        }
        if (req.getLifeCycle() != null) {
            wf.setLifecycle(JSON.toJSONString(req.getLifeCycle()));
        }
        if (TimeExpressionType.FREQUENT_TYPES.contains(req.getTimeExpressionType().getV())) {
            // 固定频率类型的任务不计算
            wf.setTimeExpression(null);
        } else {
            LifeCycle lifeCycle = Optional.ofNullable(req.getLifeCycle()).orElse(LifeCycle.EMPTY_LIFE_CYCLE);
            Long nextValidTime = timingStrategyService.calculateNextTriggerTimeWithInspection(TimeExpressionType.of(wf.getTimeExpressionType()), wf.getTimeExpression(), lifeCycle.getStart(), lifeCycle.getEnd());
            wf.setNextTriggerTime(nextValidTime);
        }
        // 新增工作流，需要先 save 一下获取 ID
        if (wfId == null) {
            wf = workflowInfoRepository.saveAndFlush(wf);
            wfId = wf.getId();
        }
        wf.setPeDAG(validateAndConvert2String(wfId, req.getDag()));
        workflowInfoRepository.saveAndFlush(wf);
        return wfId;
    }

    /**
     * 保存 DAG 信息
     * 这里会物理删除游离的节点信息
     */
    private String validateAndConvert2String(Long wfId, PEWorkflowDAG dag) {
        if (dag == null || !WorkflowDAGUtils.valid(dag)) {
            throw new PowerJobException("illegal DAG");
        }
        // 注意：这里只会保存图相关的基础信息，nodeId,jobId,jobName(nodeAlias)
        // 其中 jobId，jobName 均以节点中的信息为准
        List<Long> nodeIdList = Lists.newArrayList();
        List<PEWorkflowDAG.Node> newNodes = Lists.newArrayList();
        WorkflowDAG complexDag = WorkflowDAGUtils.convert(dag);
        for (PEWorkflowDAG.Node node : dag.getNodes()) {
            WorkflowNodeInfoDO nodeInfo = workflowNodeInfoRepository.findById(node.getNodeId()).orElseThrow(() -> new PowerJobException("can't find node info by id :" + node.getNodeId()));
            // 更新工作流 ID
            if (nodeInfo.getWorkflowId() == null) {
                nodeInfo.setWorkflowId(wfId);
                nodeInfo.setGmtModified(new Date());
                workflowNodeInfoRepository.saveAndFlush(nodeInfo);
            }
            if (!wfId.equals(nodeInfo.getWorkflowId())) {
                throw new PowerJobException("can't use another workflow's node");
            }
            nodeValidateService.complexValidate(nodeInfo, complexDag);
            // 只保存节点的 ID 信息，清空其他信息
            newNodes.add(new PEWorkflowDAG.Node(node.getNodeId()));
            nodeIdList.add(node.getNodeId());
        }
        dag.setNodes(newNodes);
        int deleteCount = workflowNodeInfoRepository.deleteByWorkflowIdAndIdNotIn(wfId, nodeIdList);
        log.warn("[WorkflowService-{}] delete {} dissociative nodes of workflow", wfId, deleteCount);
        return JSON.toJSONString(dag);
    }


    /**
     * 深度复制工作流
     *
     * @param wfId  工作流 ID
     * @param appId APP ID
     * @return 生成的工作流 ID
     */
    @Transactional(rollbackOn = Exception.class)
    public long copyWorkflow(Long wfId, Long appId) {

        WorkflowInfoDO originWorkflow = permissionCheck(wfId, appId);
        if (originWorkflow.getStatus() == SwitchableStatus.DELETED.getV()) {
            throw new IllegalStateException("can't copy the workflow which has been deleted!");
        }
        // 拷贝基础信息
        WorkflowInfoDO copyWorkflow = new WorkflowInfoDO();
        BeanUtils.copyProperties(originWorkflow, copyWorkflow);
        copyWorkflow.setId(null);
        copyWorkflow.setGmtCreate(new Date());
        copyWorkflow.setGmtModified(new Date());
        copyWorkflow.setWfName(copyWorkflow.getWfName() + "_COPY");
        // 先 save 获取 id
        copyWorkflow = workflowInfoRepository.saveAndFlush(copyWorkflow);

        if (StringUtils.isEmpty(copyWorkflow.getPeDAG())) {
            return copyWorkflow.getId();
        }

        PEWorkflowDAG dag = JSON.parseObject(copyWorkflow.getPeDAG(), PEWorkflowDAG.class);

        // 拷贝节点信息，并且更新 DAG 中的节点信息
        if (!CollectionUtils.isEmpty(dag.getNodes())) {
            // originNodeId => copyNodeId
            HashMap<Long, Long> nodeIdMap = new HashMap<>(dag.getNodes().size(), 1);
            // 校正 节点信息
            for (PEWorkflowDAG.Node node : dag.getNodes()) {

                WorkflowNodeInfoDO originNode = workflowNodeInfoRepository.findById(node.getNodeId()).orElseThrow(() -> new IllegalArgumentException("can't find workflow Node by id: " + node.getNodeId()));

                WorkflowNodeInfoDO copyNode = new WorkflowNodeInfoDO();
                BeanUtils.copyProperties(originNode, copyNode);
                copyNode.setId(null);
                copyNode.setWorkflowId(copyWorkflow.getId());
                copyNode.setGmtCreate(new Date());
                copyNode.setGmtModified(new Date());

                copyNode = workflowNodeInfoRepository.saveAndFlush(copyNode);
                nodeIdMap.put(originNode.getId(), copyNode.getId());

                node.setNodeId(copyNode.getId());
            }
            // 校正 边信息
            for (PEWorkflowDAG.Edge edge : dag.getEdges()) {
                edge.setFrom(nodeIdMap.get(edge.getFrom()));
                edge.setTo(nodeIdMap.get(edge.getTo()));
            }
        }
        copyWorkflow.setPeDAG(JSON.toJSONString(dag));
        workflowInfoRepository.saveAndFlush(copyWorkflow);
        return copyWorkflow.getId();
    }


    /**
     * 获取工作流元信息，这里获取到的 DAG 包含节点的完整信息（是否启用、是否允许失败跳过）
     *
     * @param wfId  工作流ID
     * @param appId 应用ID
     * @return 对外输出对象
     */
    public WorkflowInfoDO fetchWorkflow(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = permissionCheck(wfId, appId);
        fillWorkflow(wfInfo);
        return wfInfo;
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
    @DesignateServer
    public Long runWorkflow(Long wfId, Long appId, String initParams, Long delay) {

        delay = delay == null ? 0 : delay;
        WorkflowInfoDO wfInfo = permissionCheck(wfId, appId);

        log.info("[WorkflowService-{}] try to run workflow, initParams={},delay={} ms.", wfInfo.getId(), initParams, delay);
        Long wfInstanceId = workflowInstanceManager.create(wfInfo, initParams, System.currentTimeMillis() + delay, null);
        if (delay <= 0) {
            workflowInstanceManager.start(wfInfo, wfInstanceId);
        } else {
            InstanceTimeWheelService.schedule(wfInstanceId, delay, () -> workflowInstanceManager.start(wfInfo, wfInstanceId));
        }
        return wfInstanceId;
    }


    /**
     * 保存工作流节点（新增 或者 保存）
     *
     * @param workflowNodeRequestList 工作流节点
     * @return 更新 或者 创建后的工作流节点信息
     */
    @Transactional(rollbackOn = Exception.class)
    public List<WorkflowNodeInfoDO> saveWorkflowNode(List<SaveWorkflowNodeRequest> workflowNodeRequestList) {
        if (CollectionUtils.isEmpty(workflowNodeRequestList)) {
            return Collections.emptyList();
        }
        final Long appId = workflowNodeRequestList.get(0).getAppId();
        List<WorkflowNodeInfoDO> res = new ArrayList<>(workflowNodeRequestList.size());
        for (SaveWorkflowNodeRequest req : workflowNodeRequestList) {
            req.valid();
            // 必须位于同一个 APP 下
            if (!appId.equals(req.getAppId())) {
                throw new PowerJobException("node list must are in the same app");
            }
            WorkflowNodeInfoDO workflowNodeInfo;
            if (req.getId() != null) {
                workflowNodeInfo = workflowNodeInfoRepository.findById(req.getId()).orElseThrow(() -> new IllegalArgumentException("can't find workflow Node by id: " + req.getId()));
            } else {
                workflowNodeInfo = new WorkflowNodeInfoDO();
                workflowNodeInfo.setGmtCreate(new Date());
            }
            BeanUtils.copyProperties(req, workflowNodeInfo);
            workflowNodeInfo.setType(req.getType());
            nodeValidateService.simpleValidate(workflowNodeInfo);
            workflowNodeInfo.setGmtModified(new Date());
            workflowNodeInfo = workflowNodeInfoRepository.saveAndFlush(workflowNodeInfo);
            res.add(workflowNodeInfo);
        }
        return res;
    }


    private void fillWorkflow(WorkflowInfoDO wfInfo) {

        PEWorkflowDAG dagInfo = null;
        try {
            dagInfo = JSON.parseObject(wfInfo.getPeDAG(), PEWorkflowDAG.class);
        } catch (Exception e) {
            log.warn("[WorkflowService-{}]illegal DAG : {}", wfInfo.getId(), wfInfo.getPeDAG());
        }
        if (dagInfo == null) {
            return;
        }

        Map<Long, WorkflowNodeInfoDO> nodeIdNodInfoMap = Maps.newHashMap();

        workflowNodeInfoRepository.findByWorkflowId(wfInfo.getId()).forEach(
                e -> nodeIdNodInfoMap.put(e.getId(), e)
        );
        // 填充节点信息
        if (!CollectionUtils.isEmpty(dagInfo.getNodes())) {
            for (PEWorkflowDAG.Node node : dagInfo.getNodes()) {
                WorkflowNodeInfoDO nodeInfo = nodeIdNodInfoMap.get(node.getNodeId());
                if (nodeInfo != null) {
                    node.setNodeType(nodeInfo.getType())
                            .setJobId(nodeInfo.getJobId())
                            .setEnable(nodeInfo.getEnable())
                            .setSkipWhenFailed(nodeInfo.getSkipWhenFailed())
                            .setNodeName(nodeInfo.getNodeName())
                            .setNodeParams(nodeInfo.getNodeParams());
                }
            }
        }
        wfInfo.setPeDAG(JSON.toJSONString(dagInfo));
    }


    private WorkflowInfoDO permissionCheck(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = workflowInfoRepository.findById(wfId).orElseThrow(() -> new IllegalArgumentException("can't find workflow by id: " + wfId));
        if (!wfInfo.getAppId().equals(appId)) {
            throw new PowerJobException("Permission Denied! can't operate other app's workflow!");
        }
        return wfInfo;
    }
}
