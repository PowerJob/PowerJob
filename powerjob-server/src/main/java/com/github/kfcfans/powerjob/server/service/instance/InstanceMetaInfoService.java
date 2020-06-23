package com.github.kfcfans.powerjob.server.service.instance;

import com.github.kfcfans.powerjob.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.JobInfoRepository;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * 存储 instance 对应的 JobInfo 信息
 *
 * @author tjq
 * @since 2020/6/23
 */
@Service
public class InstanceMetaInfoService {

    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    // 缓存，一旦生成任务实例，其对应的 JobInfo 不应该再改变（即使源数据改变）
    private Cache<Long, JobInfoDO> instanceId2JobInfoCache;

    private static final int CACHE_CONCURRENCY_LEVEL = 8;
    private static final int CACHE_MAX_SIZE = 4096;

    public InstanceMetaInfoService() {
        instanceId2JobInfoCache = CacheBuilder.newBuilder()
                .concurrencyLevel(CACHE_CONCURRENCY_LEVEL)
                .maximumSize(CACHE_MAX_SIZE)
                .build();
    }

    /**
     * 根据 instanceId 获取 JobInfo
     * @param instanceId instanceId
     * @return JobInfoDO
     * @throws ExecutionException 异常
     */
    public JobInfoDO fetchJobInfoByInstanceId(Long instanceId) throws ExecutionException {
        return instanceId2JobInfoCache.get(instanceId, () -> {
            InstanceInfoDO instanceInfo = instanceInfoRepository.findByInstanceId(instanceId);
            if (instanceInfo != null) {
                Optional<JobInfoDO> jobInfoOpt = jobInfoRepository.findById(instanceInfo.getJobId());
                return jobInfoOpt.orElseThrow(() -> new IllegalArgumentException("can't find JobInfo by jobId: " + instanceInfo.getJobId()));
            }
            throw new IllegalArgumentException("can't find Instance by instanceId: " + instanceId);
        });
    }

    /**
     * 装载缓存
     * @param instanceId instanceId
     * @param jobInfoDO 原始的任务数据
     */
    public void loadJobInfo(Long instanceId, JobInfoDO jobInfoDO) {
        instanceId2JobInfoCache.put(instanceId, jobInfoDO);
    }
}
