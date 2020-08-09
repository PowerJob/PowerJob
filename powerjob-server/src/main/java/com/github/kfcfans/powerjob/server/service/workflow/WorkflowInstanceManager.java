package com.github.kfcfans.powerjob.server.service.workflow;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.SystemInstanceResult;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.WorkflowInstanceStatus;
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
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理运行中的工作流实例
 *
 * @author tjq
 * @since 2020/5/26
 */
@Slf4j
@Service
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
     * @param wfInfo 工作流任务元数据（描述信息）
     * @return wfInstanceId
     */
    public Long create(WorkflowInfoDO wfInfo) {

        Long wfId = wfInfo.getId();
        Long wfInstanceId = idGenerateService.allocate();

        // 仅创建，不写入 DAG 图信息
        Date now = new Date();
        WorkflowInstanceInfoDO newWfInstance = new WorkflowInstanceInfoDO();
        newWfInstance.setAppId(wfInfo.getAppId());
        newWfInstance.setWfInstanceId(wfInstanceId);
        newWfInstance.setWorkflowId(wfId);
        newWfInstance.setStatus(WorkflowInstanceStatus.WAITING.getV());
        newWfInstance.setActualTriggerTime(System.currentTimeMillis());

        newWfInstance.setGmtCreate(now);
        newWfInstance.setGmtModified(now);

        // 校验合法性（工作是否存在且启用）
        List<Long> allJobIds = Lists.newLinkedList();
        PEWorkflowDAG dag = JSONObject.parseObject(wfInfo.getPeDAG(), PEWorkflowDAG.class);
        dag.getNodes().forEach(node -> allJobIds.add(node.getJobId()));
        int needNum = allJobIds.size();
        long dbNum = jobInfoRepository.countByAppIdAndStatusAndIdIn(wfInfo.getAppId(), SwitchableStatus.ENABLE.getV(), allJobIds);
        log.debug("[Workflow-{}|{}] contains {} jobs, find {} jobs in database.", wfId, wfInstanceId, needNum, dbNum);

        if (dbNum < allJobIds.size()) {
            log.warn("[Workflow-{}|{}] this workflow need {} jobs, but just find {} jobs in database, maybe you delete or disable some job!", wfId, wfInstanceId, needNum, dbNum);
            onWorkflowInstanceFailed(SystemInstanceResult.CAN_NOT_FIND_JOB, newWfInstance);
        }else {
            workflowInstanceInfoRepository.save(newWfInstance);
        }
        return wfInstanceId;
    }

    /**
     * 开始任务
     * @param wfInfo 工作流任务信息
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
            log.info("[Workflow-{}|{}] workflowInstance({}) need't running any more.", wfInfo.getId(), wfInstanceId, wfInstanceInfo);
            return;
        }

        // 并发度控制
        int instanceConcurrency = workflowInstanceInfoRepository.countByWorkflowIdAndStatusIn(wfInfo.getId(), WorkflowInstanceStatus.generalizedRunningStatus);
        if (instanceConcurrency > wfInfo.getMaxWfInstanceNum()) {
            onWorkflowInstanceFailed(String.format(SystemInstanceResult.TOO_MUCH_INSTANCE, instanceConcurrency, wfInfo.getMaxWfInstanceNum()), wfInstanceInfo);
            return;
        }

        try {

            PEWorkflowDAG peWorkflowDAG = JSONObject.parseObject(wfInfo.getPeDAG(), PEWorkflowDAG.class);
            List<PEWorkflowDAG.Node> roots = WorkflowDAGUtils.listRoots(peWorkflowDAG);

            peWorkflowDAG.getNodes().forEach(node -> node.setStatus(InstanceStatus.WAITING_DISPATCH.getV()));
            // 创建所有的根任务
            roots.forEach(root -> {
                Long instanceId = instanceService.create(root.getJobId(), wfInfo.getAppId(), null, wfInstanceId, System.currentTimeMillis());
                root.setInstanceId(instanceId);
                root.setStatus(InstanceStatus.RUNNING.getV());

                log.info("[Workflow-{}|{}] create root instance(jobId={},instanceId={}) successfully~", wfInfo.getId(), wfInstanceId, root.getJobId(), instanceId);
            });

            // 持久化
            wfInstanceInfo.setStatus(WorkflowInstanceStatus.RUNNING.getV());
            wfInstanceInfo.setDag(JSONObject.toJSONString(peWorkflowDAG));
            workflowInstanceInfoRepository.saveAndFlush(wfInstanceInfo);
            log.info("[Workflow-{}|{}] start workflow successfully", wfInfo.getId(), wfInstanceId);

            // 真正开始执行根任务
            roots.forEach(root -> runInstance(root.getJobId(), root.getInstanceId(), wfInstanceId, null));
        }catch (Exception e) {

            log.error("[Workflow-{}|{}] submit workflow: {} failed.", wfInfo.getId(), wfInstanceId, wfInfo, e);
            onWorkflowInstanceFailed(e.getMessage(), wfInstanceInfo);
        }
    }

    /**
     * 下一步（当工作流的某个任务完成时调用该方法）
     * @param wfInstanceId 工作流任务实例ID
     * @param instanceId 具体完成任务的某个任务实例ID
     * @param status 完成任务的任务实例状态（SUCCEED/FAILED/STOPPED）
     * @param result 完成任务的任务实例结果
     */
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

            // 特殊处理手动终止的情况
            if (status == InstanceStatus.STOPPED) {
                // 工作流已经不在运行状态了（由用户手动停止工作流实例导致），不需要任何操作
                if (!WorkflowInstanceStatus.generalizedRunningStatus.contains(wfInstance.getStatus())) {
                    return;
                }
            }

            try {
                PEWorkflowDAG dag = JSONObject.parseObject(wfInstance.getDag(), PEWorkflowDAG.class);
                // 保存 jobId -> Node 的映射关系（一个job只能出现一次的原因）
                Map<Long, PEWorkflowDAG.Node> jobId2Node = Maps.newHashMap();

                // 更新完成节点状态
                boolean allFinished = true;
                for (PEWorkflowDAG.Node node : dag.getNodes()) {
                    if (instanceId.equals(node.getInstanceId())) {
                        node.setStatus(status.getV());
                        node.setResult(result);

                        log.info("[Workflow-{}|{}] node(jobId={},instanceId={}) finished in workflowInstance, status={},result={}", wfId, wfInstanceId, node.getJobId(), instanceId, status.name(), result);
                    }

                    if (InstanceStatus.generalizedRunningStatus.contains(node.getStatus())) {
                        allFinished = false;
                    }
                    jobId2Node.put(node.getJobId(), node);
                }

                wfInstance.setGmtModified(new Date());
                wfInstance.setDag(JSONObject.toJSONString(dag));
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
                Map<Long, Long> jobId2InstanceId = Maps.newHashMap();
                Map<Long, String> jobId2InstanceParams = Maps.newHashMap();

                relyMap.keySet().forEach(jobId -> {

                    // 跳过已完成节点（理论上此处不可能出现 FAILED 的情况）和已派发节点（存在 InstanceId）
                    if (jobId2Node.get(jobId).getStatus() == InstanceStatus.SUCCEED.getV() || jobId2Node.get(jobId).getInstanceId() != null) {
                        return;
                    }
                    // 判断某个任务所有依赖的完成情况，只要有一个未成功，即无法执行
                    for (Long reliedJobId : relyMap.get(jobId)) {
                        if (jobId2Node.get(reliedJobId).getStatus() != InstanceStatus.SUCCEED.getV()) {
                            return;
                        }
                    }

                    // 所有依赖已经执行完毕，可以执行该任务 （为什么是 Key 是 String？在 JSON 标准中，key必须由双引号引起来，Long会导致结果无法被反序列化）
                    Map<String, String> preJobId2Result = Maps.newHashMap();
                    // 构建下一个任务的入参 （前置任务 jobId -> result）
                    relyMap.get(jobId).forEach(jid -> preJobId2Result.put(String.valueOf(jid), jobId2Node.get(jid).getResult()));

                    Long newInstanceId = instanceService.create(jobId, wfInstance.getAppId(), JsonUtils.toJSONString(preJobId2Result), wfInstanceId, System.currentTimeMillis());
                    jobId2Node.get(jobId).setInstanceId(newInstanceId);
                    jobId2Node.get(jobId).setStatus(InstanceStatus.RUNNING.getV());

                    jobId2InstanceId.put(jobId, newInstanceId);
                    jobId2InstanceParams.put(jobId, JSONObject.toJSONString(preJobId2Result));

                    log.debug("[Workflow-{}|{}] workflowInstance start to process new node(jobId={},instanceId={})", wfId, wfInstanceId, jobId, newInstanceId);
                });

                wfInstance.setDag(JSONObject.toJSONString(dag));
                workflowInstanceInfoRepository.saveAndFlush(wfInstance);

                // 持久化结束后，开始调度执行所有的任务
                jobId2InstanceId.forEach((jobId, newInstanceId) -> runInstance(jobId, newInstanceId, wfInstanceId, jobId2InstanceParams.get(jobId)));

            }catch (Exception e) {
                onWorkflowInstanceFailed("MOVE NEXT STEP FAILED: " + e.getMessage(), wfInstance);
                log.error("[Workflow-{}|{}] update failed.", wfId, wfInstanceId, e);
            }

        } catch (InterruptedException ignore) {
        } finally {
            segmentLock.unlock(lockId);
        }
    }

    /**
     * 允许任务实例
     * 需要将创建和运行任务实例分离，否则在秒失败情况下，会发生DAG覆盖更新的问题
     * @param jobId 任务ID
     * @param instanceId 任务实例ID
     * @param wfInstanceId 工作流任务实例ID
     * @param instanceParams 任务实例参数，值为上游任务的执行结果： preJobId to preJobInstanceResult
     */
    private void runInstance(Long jobId, Long instanceId, Long wfInstanceId, String instanceParams) {
        JobInfoDO jobInfo = jobInfoRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("can't find job by id:" + jobId));
        // 洗去时间表达式类型
        jobInfo.setTimeExpressionType(TimeExpressionType.WORKFLOW.getV());
        dispatchService.dispatch(jobInfo, instanceId, 0, instanceParams, wfInstanceId);
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
        }catch (Exception ignore) {
        }
    }
}
