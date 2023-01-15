package tech.powerjob.server.migrate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.server.common.utils.SpringUtils;
import tech.powerjob.server.extension.LockService;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowNodeInfoDO;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;
import tech.powerjob.server.persistence.remote.repository.WorkflowInfoRepository;
import tech.powerjob.server.persistence.remote.repository.WorkflowNodeInfoRepository;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Help users upgrade from a low version of powerjob-server to a high version of powerjob-server
 *
 * @author tjq
 * @author Echo009
 * @since 2021/3/5
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class V3ToV4MigrateService {

    private static final String MIGRATE_LOCK_TEMPLATE = "v3to4MigrateLock-%s-%s";

    private final LockService lockService;

    private final JobInfoRepository jobInfoRepository;

    private final WorkflowInfoRepository workflowInfoRepository;

    private final WorkflowNodeInfoRepository workflowNodeInfoRepository;

    /* ********************** 3.x => 4.x ********************** */

    /**
     * 修复该 APP 下使用了弃用的处理器类型 {@link ProcessorType#SHELL} 以及 {@link ProcessorType#PYTHON} 的任务
     * 将其替换为官方提供的 Processor
     */
    @Transactional(rollbackOn = Exception.class)
    public JSONObject fixDeprecatedProcessType(Long appId) {

        final String lock = String.format(MIGRATE_LOCK_TEMPLATE, "fixDeprecatedProcessType", appId);
        // 120 s
        boolean getLock = lockService.tryLock(lock, 120000);
        if (!getLock) {
            throw new PowerJobException("get lock failed, maybe other migrate job is running");
        }
        try {
            JSONObject resultLog = new JSONObject();
            resultLog.put("docs", "https://www.yuque.com/powerjob/guidence/official_processor");
            resultLog.put("tips", "please add the maven dependency of 'powerjob-official-processors'");

            Set<Long> convertedJobIds = Sets.newHashSet();

            Specification<JobInfoDO> specification = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = Lists.newLinkedList();
                List<Integer> scriptJobTypes = Lists.newArrayList(ProcessorType.SHELL.getV(), ProcessorType.PYTHON.getV());
                predicates.add(criteriaBuilder.equal(root.get("appId"), appId));
                predicates.add(root.get("processorType").in(scriptJobTypes));
                return query.where(predicates.toArray(new Predicate[0])).getRestriction();
            };
            List<JobInfoDO> scriptJobs = jobInfoRepository.findAll(specification);
            resultLog.put("scriptJobsNum", scriptJobs.size());

            Stopwatch stopwatch = Stopwatch.createStarted();
            log.info("[FixDeprecatedProcessType] start to fix the job info whose processor type is deprecated,total number : {}", scriptJobs.size());
            scriptJobs.forEach(job -> {

                ProcessorType oldProcessorType = ProcessorType.of(job.getProcessorType());

                job.setJobParams(job.getProcessorInfo());
                job.setProcessorType(ProcessorType.BUILT_IN.getV());

                if (oldProcessorType == ProcessorType.PYTHON) {
                    job.setProcessorInfo("tech.powerjob.official.processors.impl.script.PythonProcessor");
                } else {
                    job.setProcessorInfo("tech.powerjob.official.processors.impl.script.ShellProcessor");
                }

                jobInfoRepository.saveAndFlush(job);
                convertedJobIds.add(job.getId());
            });
            resultLog.put("convertedJobIds", convertedJobIds);
            stopwatch.stop();
            log.info("[FixDeprecatedProcessType] fix the job info successfully,used time: {}s", stopwatch.elapsed(TimeUnit.SECONDS));
            return resultLog;
        } catch (Exception e) {
            // log
            log.error("[FixDeprecatedProcessType] fail to fix the job info of app {}", appId, e);
            // rethrow
            throw e;
        } finally {
            lockService.unlock(lock);
        }
    }


    /**
     * 修复该 APP 下的工作流信息，允许部分修复成功
     * 1、自动生成对应的节点信息 {@link WorkflowNodeInfoDO}
     * 2、修复 DAG 信息（边+节点ID）
     */
    @SuppressWarnings("squid:S1141")
    public JSONObject fixWorkflowInfoFromV3ToV4(Long appId) {

        final String lock = String.format(MIGRATE_LOCK_TEMPLATE, "fixWorkflowInfoFromV3ToV4", appId);
        // 180 s
        boolean getLock = lockService.tryLock(lock, 180000);
        if (!getLock) {
            throw new PowerJobException("get lock failed, maybe other migrate job is running");
        }

        try {
            JSONObject resultLog = new JSONObject();
            Set<Long> fixedWorkflowIds = Sets.newHashSet();

            List<WorkflowInfoDO> workflowInfoList = workflowInfoRepository.findByAppId(appId);
            resultLog.put("totalNum", workflowInfoList.size());
            Stopwatch stopwatch = Stopwatch.createStarted();
            log.info("[FixWorkflowInfoFromV3ToV4] start to fix the workflow info, total number : {}", workflowInfoList.size());


            HashMap<Long, Long> jobId2NodeIdMap = new HashMap<>(64);
            HashMap<Long, String> failureReasonMap = new HashMap<>(workflowInfoList.size() / 2 + 1);

            for (WorkflowInfoDO workflowInfo : workflowInfoList) {

                try {
                    boolean fixed = SpringUtils.getBean(this.getClass()).fixWorkflowInfoCoreFromV3ToV4(workflowInfo, jobId2NodeIdMap);
                    if (fixed) {
                        fixedWorkflowIds.add(workflowInfo.getId());
                    }
                } catch (Exception e) {
                    // 记录失败原因
                    failureReasonMap.put(workflowInfo.getId(), e.toString());
                }
                // 清空映射关系
                jobId2NodeIdMap.clear();
            }
            stopwatch.stop();
            log.info("[FixWorkflowInfoFromV3ToV4] fix the workflow info successfully, total number : {}, fixed number : {}, used time: {}s", workflowInfoList.size(), fixedWorkflowIds.size(), stopwatch.elapsed(TimeUnit.SECONDS));
            resultLog.put("fixedWorkflowIds", fixedWorkflowIds);
            resultLog.put("failureWorkflowInfo", failureReasonMap);
            return resultLog;
        } catch (Exception e) {
            // log
            log.error("[FixWorkflowInfoFromV3ToV4] fail to fix the workflow info of app {}", appId, e);
            // rethrow
            throw e;
        } finally {
            lockService.unlock(lock);
        }

    }

    /**
     * 有两种情况会修复失败
     * 1、节点对应 job 信息缺失（被物理删除）
     * 2、图中一部分节点有 nodeId，一部分没有
     */
    @Transactional(rollbackOn = Exception.class)
    public boolean fixWorkflowInfoCoreFromV3ToV4(WorkflowInfoDO workflowInfo, Map<Long, Long> jobId2NodeIdMap) {

        String dag = workflowInfo.getPeDAG();
        PEWorkflowDAG peDag;
        try {
            peDag = JSON.parseObject(dag, PEWorkflowDAG.class);
        } catch (Exception e) {
            throw new PowerJobException("invalid DAG!");
        }
        if (peDag == null || CollectionUtils.isEmpty(peDag.getNodes())) {
            // 不需要修复
            return false;
        }
        // 只要有任意一个节点中存在 nodeId ，那么就不需要修复
        // 如果没有直接在 DB 改过数据，那么不可能出现一部分节点有 id，一部分没有的情况
        boolean needFix = false;
        boolean existNodeId = false;
        for (PEWorkflowDAG.Node node : peDag.getNodes()) {
            if (node.getNodeId() == null) {
                needFix = true;
            } else {
                existNodeId = true;
            }
        }
        // 存在错误数据(一部分节点有 id，一部分没有)，这种情况下只能让用户手工修复数据了
        if (needFix && existNodeId) {
            throw new PowerJobException("sorry,we can't fix this workflow info automatically whose node info is wrong! you need to fix them by yourself.");
        }
        // 不需要修复，所有节点 id 均存在
        if (!needFix) {
            return false;
        }

        // 修复节点信息
        for (PEWorkflowDAG.Node node : peDag.getNodes()) {
            JobInfoDO jobInfo = jobInfoRepository.findById(node.getJobId()).orElseThrow(() -> new PowerJobException("can't find job by id " + node.getJobId()));

            WorkflowNodeInfoDO nodeInfo = new WorkflowNodeInfoDO();
            nodeInfo.setWorkflowId(workflowInfo.getId());
            nodeInfo.setAppId(workflowInfo.getAppId());
            nodeInfo.setJobId(jobInfo.getId());
            // 默认启用，不允许失败跳过，参数和 Job 保持一致
            nodeInfo.setNodeName(jobInfo.getJobName());
            nodeInfo.setNodeParams(jobInfo.getJobParams());
            nodeInfo.setEnable(true);
            nodeInfo.setSkipWhenFailed(false);
            nodeInfo.setGmtCreate(new Date());
            nodeInfo.setGmtModified(new Date());

            nodeInfo = workflowNodeInfoRepository.saveAndFlush(nodeInfo);
            // 更新节点 ID
            node.setNodeId(nodeInfo.getId());
            node.setNodeName(nodeInfo.getNodeName());

            jobId2NodeIdMap.put(node.getJobId(), node.getNodeId());
        }
        if (!CollectionUtils.isEmpty(peDag.getEdges())) {
            // 修复边信息
            for (PEWorkflowDAG.Edge edge : peDag.getEdges()) {
                // 转换为节点 ID
                edge.setFrom(jobId2NodeIdMap.get(edge.getFrom()));
                edge.setTo(jobId2NodeIdMap.get(edge.getTo()));
            }
        }
        workflowInfo.setPeDAG(JSON.toJSONString(peDag));
        workflowInfo.setGmtModified(new Date());
        workflowInfoRepository.saveAndFlush(workflowInfo);
        return true;

    }
}
