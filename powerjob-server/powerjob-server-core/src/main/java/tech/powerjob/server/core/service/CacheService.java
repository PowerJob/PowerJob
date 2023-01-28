package tech.powerjob.server.core.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.repository.InstanceInfoRepository;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;
import tech.powerjob.server.persistence.remote.repository.WorkflowInfoRepository;

import java.time.Duration;
import java.util.Optional;

/**
 * 本地缓存常用数据查询操作
 *
 * @author tjq
 * @since 2020/4/14
 */
@Slf4j
@Service
public class CacheService {

    private final JobInfoRepository jobInfoRepository;

    private final WorkflowInfoRepository workflowInfoRepository;

    private final InstanceInfoRepository instanceInfoRepository;

    private final Cache<Long, String> jobId2JobNameCache;
    private final Cache<Long, String> workflowId2WorkflowNameCache;
    private final Cache<Long, Long> instanceId2AppId;
    private final Cache<Long, Long> jobId2AppId;

    public CacheService(JobInfoRepository jobInfoRepository, WorkflowInfoRepository workflowInfoRepository, InstanceInfoRepository instanceInfoRepository) {

        this.jobInfoRepository = jobInfoRepository;
        this.workflowInfoRepository = workflowInfoRepository;
        this.instanceInfoRepository = instanceInfoRepository;

        jobId2JobNameCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(512)
                .softValues()
                .build();

        workflowId2WorkflowNameCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(512)
                .softValues()
                .build();

        instanceId2AppId = CacheBuilder.newBuilder()
                .maximumSize(1024)
                .softValues()
                .build();
        jobId2AppId = CacheBuilder.newBuilder()
                .maximumSize(1024)
                .softValues()
                .build();
    }

    /**
     * 根据 jobId 查询 jobName（不保证数据一致性，或者说只要改了数据必不一致hhh）
     * @param jobId 任务ID
     * @return 任务名称
     */
    public String getJobName(Long jobId) {
        try {
            return jobId2JobNameCache.get(jobId, () -> {
                Optional<JobInfoDO> jobInfoDOOptional = jobInfoRepository.findById(jobId);
                // 防止缓存穿透 hhh（但是一开始没有，后来创建的情况下会有问题，不过问题不大，这里就不管了）
                return jobInfoDOOptional.map(JobInfoDO::getJobName).orElse("");
            });
        }catch (Exception e) {
            log.error("[CacheService] getJobName for {} failed.", jobId, e);
        }
        return null;
    }

    /**
     * 根据 workflowId 查询 工作流名称
     * @param workflowId 工作流ID
     * @return 工作流名称
     */
    public String getWorkflowName(Long workflowId) {
        try {
            return workflowId2WorkflowNameCache.get(workflowId, () -> {
                Optional<WorkflowInfoDO> jobInfoDOOptional = workflowInfoRepository.findById(workflowId);
                // 防止缓存穿透 hhh（但是一开始没有，后来创建的情况下会有问题，不过问题不大，这里就不管了）
                return jobInfoDOOptional.map(WorkflowInfoDO::getWfName).orElse("");
            });
        }catch (Exception e) {
            log.error("[CacheService] getWorkflowName for {} failed.", workflowId, e);
        }
        return null;
    }

    public Long getAppIdByInstanceId(Long instanceId) {

        try {
            return instanceId2AppId.get(instanceId, () -> {
                // 内部记录数据库异常
                try {
                    InstanceInfoDO instanceLog = instanceInfoRepository.findByInstanceId(instanceId);
                    if (instanceLog != null) {
                        return instanceLog.getAppId();
                    }
                }catch (Exception e) {
                    log.error("[CacheService] getAppId for instanceId:{} failed.", instanceId, e);
                }
                return null;
            });
        }catch (Exception ignore) {
            // 忽略缓存 load 失败的异常
        }
        return null;
    }

    public Long getAppIdByJobId(Long jobId) {
        try {
            return jobId2AppId.get(jobId, () -> {
                try {
                    Optional<JobInfoDO> jobInfoDOOptional = jobInfoRepository.findById(jobId);
                    return jobInfoDOOptional.map(JobInfoDO::getAppId).orElse(null);
                }catch (Exception e) {
                    log.error("[CacheService] getAppId for job:{} failed.", jobId, e);
                }
                return null;
            });
        } catch (Exception ignore) {
        }
        return null;
    }
}
