package tech.powerjob.server.core.workflow;

import com.alibaba.fastjson.JSON;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.enums.TimeExpressionType;
import com.github.kfcfans.powerjob.common.model.PEWorkflowDAG;
import com.github.kfcfans.powerjob.common.request.http.SaveWorkflowNodeRequest;
import com.github.kfcfans.powerjob.common.request.http.SaveWorkflowRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.common.timewheel.holder.InstanceTimeWheelService;
import tech.powerjob.server.common.utils.CronExpression;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAGUtils;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowNodeInfoDO;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;
import tech.powerjob.server.persistence.remote.repository.WorkflowInfoRepository;
import tech.powerjob.server.persistence.remote.repository.WorkflowNodeInfoRepository;
import tech.powerjob.server.remote.server.redirector.DesignateServer;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

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
     */
    @Transactional(rollbackOn = Exception.class)
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
            wf.setNotifyUserIds(SJ.COMMA_JOINER.join(req.getNotifyUserIds()));
        }

        // 计算 NextTriggerTime
        if (req.getTimeExpressionType() == TimeExpressionType.CRON) {
            CronExpression cronExpression = new CronExpression(req.getTimeExpression());
            Date nextValidTime = cronExpression.getNextValidTimeAfter(new Date());
            wf.setNextTriggerTime(nextValidTime.getTime());
        } else {
            wf.setTimeExpression(null);
        }
        // 在当前的交互设计下首次创建一定不会有 DAG 信息
        if (req.getId() != null) {
            wf.setPeDAG(validateDAGAndConvert2String(req));
        }

        WorkflowInfoDO newEntity = workflowInfoRepository.saveAndFlush(wf);

        return newEntity.getId();
    }

    /**
     * 保存 DAG 信息
     * 这里会物理删除游离的节点信息
     */
    private String validateDAGAndConvert2String(SaveWorkflowRequest req) {
        if (req.getDag() == null || CollectionUtils.isEmpty(req.getDag().getNodes())) {
            return "{}";
        }
        PEWorkflowDAG dag = req.getDag();
        if (!WorkflowDAGUtils.valid(dag)) {
            throw new PowerJobException("illegal DAG");
        }
        // 注意：这里只会保存图相关的基础信息，nodeId,jobId,jobName(nodeAlias)
        // 其中 jobId，jobName 均以节点中的信息为准
        List<Long> nodeIdList = Lists.newArrayList();
        Map<Long, WorkflowNodeInfoDO> nodeIdNodInfoMap = Maps.newHashMap();
        workflowNodeInfoRepository.findByWorkflowId(req.getId()).forEach(
                e -> nodeIdNodInfoMap.put(e.getId(), e)
        );
        for (PEWorkflowDAG.Node node : dag.getNodes()) {
            WorkflowNodeInfoDO nodeInfo = nodeIdNodInfoMap.get(node.getNodeId());
            if (nodeInfo == null) {
                throw new PowerJobException("can't find node info by id :" + node.getNodeId());
            }
            if (!req.getId().equals(nodeInfo.getWorkflowId())) {
                throw new PowerJobException("workflowId of current node must be same to workflowId");
            }
            // 节点中的名称信息一定是非空的
            node.setNodeName(nodeInfo.getNodeName()).setJobId(nodeInfo.getJobId());
            // 清空其他信息
            node.setEnable(null).setSkipWhenFailed(null).setInstanceId(null).setResult(null);
            nodeIdList.add(node.getNodeId());
        }

        int deleteCount = workflowNodeInfoRepository.deleteByWorkflowIdAndIdNotIn(req.getId(), nodeIdList);
        log.warn("[WorkflowService-{}]delete {} dissociative nodes of workflow", req.getId(), deleteCount);
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
        Long wfInstanceId = workflowInstanceManager.create(wfInfo, initParams, System.currentTimeMillis() + delay);
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
        List<WorkflowNodeInfoDO> res = new ArrayList<>(workflowNodeRequestList.size());
        // 记录变更过任务的节点
        List<WorkflowNodeInfoDO> changeJobNodeList = new ArrayList<>(workflowNodeRequestList.size());
        //
        WorkflowInfoDO workflowInfo = null;
        for (SaveWorkflowNodeRequest req : workflowNodeRequestList) {
            req.valid();
            permissionCheck(req.getWorkflowId(), req.getAppId());
            if (workflowInfo == null) {
                // check workflow info
                workflowInfo = workflowInfoRepository.findById(req.getWorkflowId()).orElseThrow(() -> new IllegalArgumentException("can't find workflow by id:" + req.getWorkflowId()));
            } else {
                // 每次只允许更改同一个工作流中的节点信息
                if (!workflowInfo.getId().equals(req.getWorkflowId())) {
                    throw new PowerJobException("changed node list must are in the same workflow");
                }
            }
            WorkflowNodeInfoDO workflowNodeInfo;
            if (req.getId() != null) {
                workflowNodeInfo = workflowNodeInfoRepository.findById(req.getId()).orElseThrow(() -> new IllegalArgumentException("can't find workflow Node by id: " + req.getId()));
            } else {
                workflowNodeInfo = new WorkflowNodeInfoDO();
                workflowNodeInfo.setGmtCreate(new Date());
            }
            JobInfoDO jobInfoDO = jobInfoRepository.findById(req.getJobId()).orElseThrow(() -> new IllegalArgumentException("can't find job by id: " + req.getJobId()));
            // 变更任务的节点
            if (workflowNodeInfo.getJobId() != null && !workflowNodeInfo.getJobId().equals(req.getJobId())) {
                changeJobNodeList.add(workflowNodeInfo);
            }
            BeanUtils.copyProperties(req, workflowNodeInfo);
            // 如果名称为空则默认取任务名称
            if (StringUtils.isEmpty(workflowNodeInfo.getNodeName())) {
                workflowNodeInfo.setNodeName(jobInfoDO.getJobName());
            }
            workflowNodeInfo.setGmtModified(new Date());
            workflowNodeInfo = workflowNodeInfoRepository.saveAndFlush(workflowNodeInfo);
            res.add(workflowNodeInfo);
        }
        // 同步变更 DAG 中的任务信息
        if (!changeJobNodeList.isEmpty()) {
            PEWorkflowDAG dag = JSON.parseObject(workflowInfo.getPeDAG(), PEWorkflowDAG.class);
            if (!CollectionUtils.isEmpty(dag.getNodes())) {
                Map<Long, PEWorkflowDAG.Node> nodeId2NodeMap = dag.getNodes().stream().collect(Collectors.toMap(PEWorkflowDAG.Node::getNodeId, e -> e));
                for (WorkflowNodeInfoDO nodeInfo : changeJobNodeList) {
                    PEWorkflowDAG.Node node = nodeId2NodeMap.get(nodeInfo.getId());
                    if (node != null) {
                        node.setJobId(nodeInfo.getJobId());
                    }
                }
            }
            workflowInfo.setPeDAG(JSON.toJSONString(dag));
            workflowInfo.setGmtModified(new Date());
            workflowInfoRepository.saveAndFlush(workflowInfo);
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
                    node.setEnable(nodeInfo.getEnable())
                            .setSkipWhenFailed(nodeInfo.getSkipWhenFailed())
                            .setNodeName(nodeInfo.getNodeName())
                            .setNodeParams(nodeInfo.getNodeParams());

                } else {
                    // 默认开启 并且 不允许失败跳过
                    node.setEnable(true)
                            .setSkipWhenFailed(false);
                }
            }
        }
        wfInfo.setPeDAG(JSON.toJSONString(dagInfo));
    }


    private WorkflowInfoDO permissionCheck(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = workflowInfoRepository.findById(wfId).orElseThrow(() -> new IllegalArgumentException("can't find workflow by id: " + wfId));
        if (!wfInfo.getAppId().equals(appId)) {
            throw new PowerJobException("Permission Denied!can't delete other appId's workflow!");
        }
        return wfInfo;
    }
}
