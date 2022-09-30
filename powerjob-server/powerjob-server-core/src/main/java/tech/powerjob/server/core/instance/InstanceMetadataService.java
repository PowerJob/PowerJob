package tech.powerjob.server.core.instance;

import lombok.RequiredArgsConstructor;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.repository.InstanceInfoRepository;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
public class InstanceMetadataService implements InitializingBean {

    private final JobInfoRepository jobInfoRepository;

    private final InstanceInfoRepository instanceInfoRepository;

    /**
     * 缓存，一旦生成任务实例，其对应的 JobInfo 不应该再改变（即使源数据改变）
     */
    private Cache<Long, JobInfoDO> instanceId2JobInfoCache;

    @Value("${oms.instance.metadata.cache.size}")
    private int instanceMetadataCacheSize;
    private static final int CACHE_CONCURRENCY_LEVEL = 16;

    @Override
    public void afterPropertiesSet() throws Exception {
        instanceId2JobInfoCache = CacheBuilder.newBuilder()
                .concurrencyLevel(CACHE_CONCURRENCY_LEVEL)
                .maximumSize(instanceMetadataCacheSize)
                .softValues()
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

    /**
     * 失效缓存
     * @param instanceId instanceId
     */
    public void invalidateJobInfo(Long instanceId) {
        instanceId2JobInfoCache.invalidate(instanceId);
    }

}
