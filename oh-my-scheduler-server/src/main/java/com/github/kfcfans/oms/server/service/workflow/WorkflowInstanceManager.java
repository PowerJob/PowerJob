package com.github.kfcfans.oms.server.service.workflow;

import com.github.kfcfans.oms.common.InstanceStatus;
import com.github.kfcfans.oms.common.SystemInstanceResult;
import com.github.kfcfans.oms.common.TimeExpressionType;
import com.github.kfcfans.oms.common.WorkflowInstanceStatus;
import com.github.kfcfans.oms.common.model.PEWorkflowDAG;
import com.github.kfcfans.oms.common.model.WorkflowDAG;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.common.utils.WorkflowDAGUtils;
import com.github.kfcfans.oms.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.WorkflowInstanceInfoRepository;
import com.github.kfcfans.oms.server.service.DispatchService;
import com.github.kfcfans.oms.server.service.id.IdGenerateService;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private DispatchService dispatchService;
    @Resource
    private IdGenerateService idGenerateService;

    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;
    @Resource
    private WorkflowInstanceInfoRepository workflowInstanceInfoRepository;

    /**
     * 提交运行 Workflow 工作流
     * @param wfInfo workflow 工作流数据库对象
     */
    public void submit(WorkflowInfoDO wfInfo) {

        Long wfInstanceId = idGenerateService.allocate();

        Date now = new Date();
        WorkflowInstanceInfoDO newWfInstance = new WorkflowInstanceInfoDO();
        newWfInstance.setId(wfInstanceId);
        newWfInstance.setWorkflowId(wfInfo.getId());
        newWfInstance.setGmtCreate(now);
        newWfInstance.setGmtModified(now);

        try {
            WorkflowDAG workflowDAG = WorkflowDAGUtils.convert(JsonUtils.parseObject(wfInfo.getPeDAG(), PEWorkflowDAG.class));

            // 运行根任务，无法找到根任务则直接失败
            WorkflowDAG.Node root = workflowDAG.getRoot();

            // 调度执行根任务
            Long instanceId = runJob(root.getJobId(), wfInstanceId, null);
            root.setInstanceId(instanceId);

            // 持久化
            newWfInstance.setStatus(WorkflowInstanceStatus.RUNNING.getV());
            newWfInstance.setDag(JsonUtils.toJSONStringUnsafe(workflowDAG));

        }catch (Exception e) {

            newWfInstance.setStatus(WorkflowInstanceStatus.FAILED.getV());
            newWfInstance.setResult(e.getMessage());

            log.error("[WorkflowInstanceManager] submit workflow: {} failed.", wfInfo, e);
        }
        workflowInstanceInfoRepository.saveAndFlush(newWfInstance);
    }

    /**
     * 下一步（当工作流的某个任务完成时调用该方法）
     * @param wfInstanceId 工作流任务实例ID
     * @param instanceId 具体完成任务的某个任务实例ID
     * @param success 完成任务的任务实例是否成功
     * @param result 完成任务的任务实例结果
     */
    public void move(Long wfInstanceId, Long instanceId, boolean success, String result) {

        Optional<WorkflowInstanceInfoDO> wfInstanceInfoOpt = workflowInstanceInfoRepository.findById(wfInstanceId);
        if (!wfInstanceInfoOpt.isPresent()) {
            log.error("[WorkflowInstanceManager] can't find workflowInstance({}).", wfInstanceInfoOpt);
            return;
        }
        WorkflowInstanceInfoDO wfInstance = wfInstanceInfoOpt.get();

        try {
            WorkflowDAG dag = JsonUtils.parseObject(wfInstance.getDag(), WorkflowDAG.class);

            // 计算是否有新的节点需要派发执行（relyMap 为 自底向上 的映射，用来判断所有父节点是否都已经完成）
            Map<Long, WorkflowDAG.Node> jobId2Node = Maps.newHashMap();
            Multimap<Long, Long> relyMap = LinkedListMultimap.create();

            // 层序遍历 DAG，更新完成节点的状态
            Queue<WorkflowDAG.Node> queue = Queues.newLinkedBlockingQueue();
            queue.add(dag.getRoot());
            while (!queue.isEmpty()) {
                WorkflowDAG.Node head = queue.poll();
                if (instanceId.equals(head.getInstanceId())) {
                    head.setFinished(true);
                    head.setResult(result);
                }
                queue.addAll(head.getSuccessors());

                jobId2Node.put(head.getJobId(), head);
                head.getSuccessors().forEach(n -> relyMap.put(n.getJobId(), head.getJobId()));
            }

            // 任务失败，DAG流程被打断，整体失败
            if (!success) {
                wfInstance.setDag(JsonUtils.toJSONStringUnsafe(dag));
                wfInstance.setStatus(WorkflowInstanceStatus.FAILED.getV());
                wfInstance.setResult(SystemInstanceResult.ONE_JOB_FAILED);
                workflowInstanceInfoRepository.saveAndFlush(wfInstance);
                return;
            }

            // 重新计算需要派发的任务
            AtomicBoolean allFinished = new AtomicBoolean(true);
            relyMap.keySet().forEach(jobId -> {

                // 如果该任务本身已经完成，不需要再计算，直接跳过
                if (jobId2Node.get(jobId).isFinished()) {
                    return;
                }

                allFinished.set(false);
                // 判断某个任务所有依赖的完成情况，只要有一个未完成，即无法执行
                for (Long reliedJobId : relyMap.get(jobId)) {
                    if (!jobId2Node.get(reliedJobId).isFinished()) {
                        return;
                    }
                }

                // 所有依赖已经执行完毕，可以执行该任务
                Map<Long, String> jobId2Result = Maps.newHashMap();
                relyMap.get(jobId).forEach(jid -> jobId2Result.put(jid, jobId2Node.get(jid).getResult()));

                Long newInstanceId = runJob(jobId, wfInstanceId, JsonUtils.toJSONString(jobId2Result));
                jobId2Node.get(jobId).setInstanceId(newInstanceId);
            });

            wfInstance.setDag(JsonUtils.toJSONStringUnsafe(dag));
            workflowInstanceInfoRepository.saveAndFlush(wfInstance);

        }catch (Exception e) {
            log.error("[WorkflowInstanceManager] update failed for wfInstanceId: {}.", wfInstanceId, e);

            wfInstance.setStatus(WorkflowInstanceStatus.FAILED.getV());
            wfInstance.setResult("MOVE NEXT STEP FAILED: " + e.getMessage());
            workflowInstanceInfoRepository.saveAndFlush(wfInstance);
        }
    }


    private Long runJob(Long jobId, Long wfInstanceId, String instanceParams) {

        JobInfoDO jobInfo = jobInfoRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("can't find job by id:" + jobId));

        Long instanceId = idGenerateService.allocate();

        InstanceInfoDO instanceInfo = new InstanceInfoDO();
        instanceInfo.setJobId(jobInfo.getId());
        instanceInfo.setAppId(jobInfo.getAppId());
        instanceInfo.setInstanceId(instanceId);
        instanceInfo.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
        instanceInfo.setGmtCreate(new Date());
        instanceInfo.setGmtModified(instanceInfo.getGmtCreate());
        // 预期直接触发
        instanceInfo.setExpectedTriggerTime(System.currentTimeMillis());
        // 设置 wfInstanceId，用于重试
        instanceInfo.setWfInstanceId(wfInstanceId);
        // 设置 instanceParams
        instanceInfo.setInstanceParams(instanceParams);

        // 先持久化
        instanceInfoRepository.saveAndFlush(instanceInfo);
        // 再调度执行，注意，需要洗去原来的 TimeExpressionType 和 TimeExpressionInfo
        JobInfoDO newJobInfo = new JobInfoDO();
        BeanUtils.copyProperties(jobInfo, newJobInfo);
        newJobInfo.setTimeExpressionType(TimeExpressionType.API.getV());

        dispatchService.dispatch(newJobInfo, instanceId, 0, instanceParams, wfInstanceId);

        return instanceId;
    }
}
