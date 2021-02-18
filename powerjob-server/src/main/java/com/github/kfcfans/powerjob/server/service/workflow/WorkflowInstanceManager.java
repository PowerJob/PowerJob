package com.github.kfcfans.powerjob.server.service.workflow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.kfcfans.powerjob.common.*;
import com.github.kfcfans.powerjob.common.model.PEWorkflowDAG;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.common.utils.SegmentLock;
import com.github.kfcfans.powerjob.server.common.constans.SwitchableStatus;
import com.github.kfcfans.powerjob.server.common.utils.WorkflowDAGUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.UserInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.WorkflowInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.WorkflowInstanceInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.WorkflowInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.WorkflowInstanceInfoRepository;
import com.github.kfcfans.powerjob.server.service.DispatchService;
import com.github.kfcfans.powerjob.server.service.UserService;
import com.github.kfcfans.powerjob.server.service.alarm.AlarmCenter;
import com.github.kfcfans.powerjob.server.service.alarm.WorkflowInstanceAlarm;
import com.github.kfcfans.powerjob.server.service.id.IdGenerateService;
import com.github.kfcfans.powerjob.server.service.instance.InstanceService;
import com.github.kfcfans.powerjob.server.service.lock.local.UseSegmentLock;
import com.google.common.collect.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 管理运行中的工作流实例
 *
 * @author tjq
 * @since 2020/5/26
 */
@Slf4j
@Service
@SuppressWarnings("squid:S1192")
public class WorkflowInstanceManager {

    @Resource
    private InstanceService instanceService;
    @Resource
    private DispatchService dispatchService;
    @Resource
    private IdGenerateService idGenerateService;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private UserService userService;
    @Resource
    private WorkflowInfoRepository workflowInfoRepository;
    @Resource
    private WorkflowInstanceInfoRepository workflowInstanceInfoRepository;

    private final SegmentLock segmentLock = new SegmentLock(16);

    /**
     * 创建工作流任务实例
     * ********************************************
     * 2021-02-03 modify by Echo009
     * 通过 initParams 初始化工作流上下文（wfContext）
     * ********************************************
     *
     * @param wfInfo            工作流任务元数据（描述信息）
     * @param initParams        启动参数
     * @param expectTriggerTime 预计执行时间
     * @return wfInstanceId
     */
    public Long create(WorkflowInfoDO wfInfo, String initParams, Long expectTriggerTime) {

        Long wfId = wfInfo.getId();
        Long wfInstanceId = idGenerateService.allocate();

        // 仅创建，不写入 DAG 图信息
        Date now = new Date();
        WorkflowInstanceInfoDO newWfInstance = new WorkflowInstanceInfoDO();
        newWfInstance.setAppId(wfInfo.getAppId());
        newWfInstance.setWfInstanceId(wfInstanceId);
        newWfInstance.setWorkflowId(wfId);
        newWfInstance.setStatus(WorkflowInstanceStatus.WAITING.getV());
        newWfInstance.setExpectedTriggerTime(expectTriggerTime);
        newWfInstance.setActualTriggerTime(System.currentTimeMillis());
        newWfInstance.setWfInitParams(initParams);
        // 初始化上下文
        Map<String, String> wfContextMap = Maps.newHashMap();
        wfContextMap.put(WorkflowContextConstant.CONTEXT_INIT_PARAMS_KEY, initParams);
        newWfInstance.setWfContext(JsonUtils.toJSONString(wfContextMap));

        newWfInstance.setGmtCreate(now);
        newWfInstance.setGmtModified(now);

        // 校验合法性（工作是否存在且启用）
        Set<Long> allJobIds = Sets.newHashSet();
        PEWorkflowDAG dag = JSON.parseObject(wfInfo.getPeDAG(), PEWorkflowDAG.class);
        dag.getNodes().forEach(node -> allJobIds.add(node.getJobId()));
        int needNum = allJobIds.size();
        long dbNum = jobInfoRepository.countByAppIdAndStatusAndIdIn(wfInfo.getAppId(), SwitchableStatus.ENABLE.getV(), allJobIds);
        log.debug("[Workflow-{}|{}] contains {} jobs, find {} jobs in database.", wfId, wfInstanceId, needNum, dbNum);

        if (dbNum < allJobIds.size()) {
            log.warn("[Workflow-{}|{}] this workflow need {} jobs, but just find {} jobs in database, maybe you delete or disable some job!", wfId, wfInstanceId, needNum, dbNum);
            onWorkflowInstanceFailed(SystemInstanceResult.CAN_NOT_FIND_JOB, newWfInstance);
        } else {
            workflowInstanceInfoRepository.saveAndFlush(newWfInstance);
        }
        return wfInstanceId;
    }

    /**
     * 开始任务
     * ********************************************
     * 2021-02-03 modify by Echo009
     * 1、工作流支持配置重复的任务节点
     * 2、移除参数 initParams，改为统一从工作流实例中获取
     * 传递工作流实例的 wfContext 作为 初始启动参数
     * ********************************************
     *
     * @param wfInfo       工作流任务信息
     * @param wfInstanceId 工作流任务实例ID
     */
    public void start(WorkflowInfoDO wfInfo, Long wfInstanceId) {

        Optional<WorkflowInstanceInfoDO> wfInstanceInfoOpt = workflowInstanceInfoRepository.findByWfInstanceId(wfInstanceId);
        if (!wfInstanceInfoOpt.isPresent()) {
            log.error("[WorkflowInstanceManager] can't find metadata by workflowInstanceId({}).", wfInstanceId);
            return;
        }
        WorkflowInstanceInfoDO wfInstanceInfo = wfInstanceInfoOpt.get();

        // 不是等待中，不再继续执行（可能上一流程已经失败）
        if (wfInstanceInfo.getStatus() != WorkflowInstanceStatus.WAITING.getV()) {
            log.info("[Workflow-{}|{}] workflowInstance({}) needn't running any more.", wfInfo.getId(), wfInstanceId, wfInstanceInfo);
            return;
        }

        // 并发度控制
        int instanceConcurrency = workflowInstanceInfoRepository.countByWorkflowIdAndStatusIn(wfInfo.getId(), WorkflowInstanceStatus.generalizedRunningStatus);
        if (instanceConcurrency > wfInfo.getMaxWfInstanceNum()) {
            onWorkflowInstanceFailed(String.format(SystemInstanceResult.TOO_MANY_INSTANCES, instanceConcurrency, wfInfo.getMaxWfInstanceNum()), wfInstanceInfo);
            return;
        }

        try {

            PEWorkflowDAG peWorkflowDAG = JSON.parseObject(wfInfo.getPeDAG(), PEWorkflowDAG.class);
            List<PEWorkflowDAG.Node> roots = WorkflowDAGUtils.listRoots(peWorkflowDAG);

            peWorkflowDAG.getNodes().forEach(node -> node.setStatus(InstanceStatus.WAITING_DISPATCH.getV()));
            Map<Long, JobInfoDO> nodeId2JobInfoMap = Maps.newHashMap();
            // 创建所有的根任务
            roots.forEach(root -> {
                // 注意：这里必须保证任务实例全部创建成功，如果在这里创建实例部分失败，会导致 DAG 信息不会更新，已经生成的实例节点在工作流日志中没法展示
                // 如果 job 信息缺失，在 dispatch 的时候会失败，继而使整个工作流失败
                JobInfoDO jobInfo = jobInfoRepository.findById(root.getJobId()).orElseGet(JobInfoDO::new);
                if (jobInfo.getId() == null) {
                    // 在创建工作流实例到当前的这段时间内 job 信息被物理删除了
                    log.error("[Workflow-{}|{}]job info of current node(nodeId={},jobId={}) is not present! maybe you have deleted the job!", wfInfo.getId(), wfInstanceId, root.getNodeId(), root.getJobId());
                }
                nodeId2JobInfoMap.put(root.getNodeId(), jobInfo);
                // instanceParam 传递的是工作流实例的 wfContext
                Long instanceId = instanceService.create(root.getJobId(), wfInfo.getAppId(), jobInfo.getJobParams(), wfInstanceInfo.getWfContext(), wfInstanceId, System.currentTimeMillis());
                root.setInstanceId(instanceId);
                root.setStatus(InstanceStatus.RUNNING.getV());

                log.info("[Workflow-{}|{}] create root instance(nodeId={},jobId={},instanceId={}) successfully~", wfInfo.getId(), wfInstanceId,root.getNodeId(), root.getJobId(), instanceId);
            });

            // 持久化
            wfInstanceInfo.setStatus(WorkflowInstanceStatus.RUNNING.getV());
            wfInstanceInfo.setDag(JSON.toJSONString(peWorkflowDAG));
            workflowInstanceInfoRepository.saveAndFlush(wfInstanceInfo);
            log.info("[Workflow-{}|{}] start workflow successfully", wfInfo.getId(), wfInstanceId);

            // 真正开始执行根任务
            roots.forEach(root -> runInstance(nodeId2JobInfoMap.get(root.getNodeId()), root.getInstanceId()));
        } catch (Exception e) {

            log.error("[Workflow-{}|{}] submit workflow: {} failed.", wfInfo.getId(), wfInstanceId, wfInfo, e);
            onWorkflowInstanceFailed(e.getMessage(), wfInstanceInfo);
        }
    }

    /**
     * 下一步（当工作流的某个任务完成时调用该方法）
     * ********************************************
     * 2021-02-03 modify by Echo009
     * 1、工作流支持配置重复的任务节点
     * 2、不再获取上游任务的结果作为实例参数而是传递工作流
     * 实例的 wfContext 作为 实例参数
     * ********************************************
     *
     * @param wfInstanceId 工作流任务实例ID
     * @param instanceId   具体完成任务的某个任务实例ID
     * @param status       完成任务的任务实例状态（SUCCEED/FAILED/STOPPED）
     * @param result       完成任务的任务实例结果
     */
    @SuppressWarnings({"squid:S3776", "squid:S2142", "squid:S1141"})
    public void move(Long wfInstanceId, Long instanceId, InstanceStatus status, String result) {

        int lockId = wfInstanceId.hashCode();
        try {
            segmentLock.lockInterruptible(lockId);

            Optional<WorkflowInstanceInfoDO> wfInstanceInfoOpt = workflowInstanceInfoRepository.findByWfInstanceId(wfInstanceId);
            if (!wfInstanceInfoOpt.isPresent()) {
                log.error("[WorkflowInstanceManager] can't find metadata by workflowInstanceId({}).", wfInstanceId);
                return;
            }
            WorkflowInstanceInfoDO wfInstance = wfInstanceInfoOpt.get();
            Long wfId = wfInstance.getWorkflowId();

            // 特殊处理手动终止 且 工作流实例已经不在运行状态的情况
            if (status == InstanceStatus.STOPPED && !WorkflowInstanceStatus.generalizedRunningStatus.contains(wfInstance.getStatus())) {
                // 由用户手动停止工作流实例导致，不需要任何操作
                return;
            }

            try {
                PEWorkflowDAG dag = JSON.parseObject(wfInstance.getDag(), PEWorkflowDAG.class);
                // 保存 nodeId -> Node 的映射关系
                Map<Long, PEWorkflowDAG.Node> nodeId2Node = Maps.newHashMap();

                // 更新完成节点状态
                boolean allFinished = true;
                for (PEWorkflowDAG.Node node : dag.getNodes()) {
                    if (instanceId.equals(node.getInstanceId())) {
                        node.setStatus(status.getV());
                        node.setResult(result);

                        log.info("[Workflow-{}|{}] node(nodeId={},jobId={},instanceId={}) finished in workflowInstance, status={},result={}", wfId, wfInstanceId,node.getNodeId(), node.getJobId(), instanceId, status.name(), result);
                    }

                    if (InstanceStatus.generalizedRunningStatus.contains(node.getStatus())) {
                        allFinished = false;
                    }
                    nodeId2Node.put(node.getNodeId(), node);
                }

                wfInstance.setGmtModified(new Date());
                wfInstance.setDag(JSON.toJSONString(dag));
                // 工作流已经结束（某个节点失败导致工作流整体已经失败），仅更新最新的DAG图
                if (!WorkflowInstanceStatus.generalizedRunningStatus.contains(wfInstance.getStatus())) {
                    workflowInstanceInfoRepository.saveAndFlush(wfInstance);
                    log.info("[Workflow-{}|{}] workflow already finished(status={}), just update the dag info.", wfId, wfInstanceId, wfInstance.getStatus());
                    return;
                }

                // 任务失败，DAG流程被打断，整体失败
                if (status == InstanceStatus.FAILED) {
                    log.warn("[Workflow-{}|{}] workflow instance process failed because middle task(instanceId={}) failed", wfId, wfInstanceId, instanceId);
                    onWorkflowInstanceFailed(SystemInstanceResult.MIDDLE_JOB_FAILED, wfInstance);
                    return;
                }

                // 子任务被手动停止
                if (status == InstanceStatus.STOPPED) {
                    wfInstance.setStatus(WorkflowInstanceStatus.STOPPED.getV());
                    wfInstance.setResult(SystemInstanceResult.MIDDLE_JOB_STOPPED);
                    wfInstance.setFinishedTime(System.currentTimeMillis());
                    workflowInstanceInfoRepository.saveAndFlush(wfInstance);

                    log.warn("[Workflow-{}|{}] workflow instance stopped because middle task(instanceId={}) stopped by user", wfId, wfInstanceId, instanceId);
                    return;
                }

                // 工作流执行完毕（能执行到这里代表该工作流内所有子任务都执行成功了）
                if (allFinished) {
                    wfInstance.setStatus(WorkflowInstanceStatus.SUCCEED.getV());
                    // 最终任务的结果作为整个 workflow 的结果
                    wfInstance.setResult(result);
                    wfInstance.setFinishedTime(System.currentTimeMillis());
                    workflowInstanceInfoRepository.saveAndFlush(wfInstance);

                    log.info("[Workflow-{}|{}] process successfully.", wfId, wfInstanceId);
                    return;
                }

                // 构建依赖树（下游任务需要哪些上游任务完成才能执行）
                Multimap<Long, Long> relyMap = LinkedListMultimap.create();
                dag.getEdges().forEach(edge -> relyMap.put(edge.getTo(), edge.getFrom()));

                // 重新计算需要派发的任务
                List<PEWorkflowDAG.Node> readyNodes = Lists.newArrayList();
                Map<Long, JobInfoDO> nodeId2JobInfoMap = Maps.newHashMap();
                relyMap.keySet().forEach(nodeId -> {
                    PEWorkflowDAG.Node currentNode = nodeId2Node.get(nodeId);
                    // 跳过已完成节点（理论上此处不可能出现 FAILED 的情况）和已派发节点（存在 InstanceId）
                    if (currentNode.getStatus() == InstanceStatus.SUCCEED.getV() || currentNode.getInstanceId() != null) {
                        return;
                    }
                    // 判断某个任务所有依赖的完成情况，只要有一个未成功，即无法执行
                    for (Long reliedJobId : relyMap.get(nodeId)) {
                        if (nodeId2Node.get(reliedJobId).getStatus() != InstanceStatus.SUCCEED.getV()) {
                            return;
                        }
                    }
                    // 同理：这里必须保证任务实例全部创建成功，避免部分失败导致已经生成的实例节点在工作流日志中没法展示
                    JobInfoDO jobInfo = jobInfoRepository.findById(currentNode.getJobId()).orElseGet(JobInfoDO::new);
                    if (jobInfo.getId() == null) {
                        // 在创建工作流实例到当前的这段时间内 job 信息被物理删除了
                        log.error("[Workflow-{}|{}]job info of current node(nodeId={},jobId={}) is not present! maybe you have deleted the job!", wfId, wfInstanceId, currentNode.getNodeId(), currentNode.getJobId());
                    }
                    nodeId2JobInfoMap.put(nodeId, jobInfo);
                    // instanceParam 传递的是工作流实例的 wfContext
                    Long newInstanceId = instanceService.create(jobInfo.getId(), wfInstance.getAppId(), jobInfo.getJobParams(), wfInstance.getWfContext(), wfInstanceId, System.currentTimeMillis());
                    currentNode.setInstanceId(newInstanceId);
                    currentNode.setStatus(InstanceStatus.RUNNING.getV());
                    readyNodes.add(currentNode);
                    log.debug("[Workflow-{}|{}] workflowInstance start to process new node(nodeId={},jobId={},instanceId={})", wfId, wfInstanceId, currentNode.getNodeId(), currentNode.getJobId(), newInstanceId);
                });

                wfInstance.setDag(JSON.toJSONString(dag));
                workflowInstanceInfoRepository.saveAndFlush(wfInstance);
                // 持久化结束后，开始调度执行所有的任务
                readyNodes.forEach(node -> runInstance(nodeId2JobInfoMap.get(node.getNodeId()), node.getInstanceId()));

            } catch (Exception e) {
                onWorkflowInstanceFailed("MOVE NEXT STEP FAILED: " + e.getMessage(), wfInstance);
                log.error("[Workflow-{}|{}] update failed.", wfId, wfInstanceId, e);
            }

        } catch (InterruptedException ignore) {
            // ignore
        } finally {
            segmentLock.unlock(lockId);
        }
    }

    /**
     * 更新工作流上下文
     * @since 2021/02/05
     * @param wfInstanceId          工作流实例
     * @param appendedWfContextData 追加的上下文数据
     */
    @UseSegmentLock(type = "updateWfContext", key = "#wfInstanceId.intValue()", concurrencyLevel = 1024)
    public void updateWorkflowContext(Long wfInstanceId, Map<String, String> appendedWfContextData) {

        try {
            Optional<WorkflowInstanceInfoDO> wfInstanceInfoOpt = workflowInstanceInfoRepository.findByWfInstanceId(wfInstanceId);
            if (!wfInstanceInfoOpt.isPresent()) {
                log.error("[WorkflowInstanceManager] can't find metadata by workflowInstanceId({}).", wfInstanceId);
                return;
            }
            WorkflowInstanceInfoDO wfInstance = wfInstanceInfoOpt.get();
            HashMap<String, String> wfContext = JSON.parseObject(wfInstance.getWfContext(), new TypeReference<HashMap<String, String>>() {
            });
            for (Map.Entry<String, String> entry : appendedWfContextData.entrySet()) {
                String key = entry.getKey();
                String originValue = wfContext.put(key, entry.getValue());
                log.info("[Workflow-{}|{}] update workflow context {} : {} -> {}", wfInstance.getWorkflowId(), wfInstance.getWfInstanceId(), key, originValue, entry.getValue());
            }
            wfInstance.setWfContext(JSON.toJSONString(wfContext));
            workflowInstanceInfoRepository.saveAndFlush(wfInstance);

        } catch (Exception e) {
            log.error("[WorkflowInstanceManager] update workflow(workflowInstanceId={}) context failed.", wfInstanceId, e);
        }

    }

    /**
     * 运行任务实例
     * 需要将创建和运行任务实例分离，否则在秒失败情况下，会发生DAG覆盖更新的问题
     *
     * @param jobInfo    任务信息
     * @param instanceId 任务实例ID
     */
    private void runInstance(JobInfoDO jobInfo, Long instanceId) {
        // 洗去时间表达式类型
        jobInfo.setTimeExpressionType(TimeExpressionType.WORKFLOW.getV());
        dispatchService.dispatch(jobInfo, instanceId);
    }

    private void onWorkflowInstanceFailed(String result, WorkflowInstanceInfoDO wfInstance) {

        wfInstance.setStatus(WorkflowInstanceStatus.FAILED.getV());
        wfInstance.setResult(result);
        wfInstance.setFinishedTime(System.currentTimeMillis());
        wfInstance.setGmtModified(new Date());

        workflowInstanceInfoRepository.saveAndFlush(wfInstance);

        // 报警
        try {
            workflowInfoRepository.findById(wfInstance.getWorkflowId()).ifPresent(wfInfo -> {
                WorkflowInstanceAlarm content = new WorkflowInstanceAlarm();

                BeanUtils.copyProperties(wfInfo, content);
                BeanUtils.copyProperties(wfInstance, content);
                content.setResult(result);

                List<UserInfoDO> userList = userService.fetchNotifyUserList(wfInfo.getNotifyUserIds());
                AlarmCenter.alarmFailed(content, userList);
            });
        } catch (Exception ignore) {
            // ignore
        }
    }
}
