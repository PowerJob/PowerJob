package com.github.kfcfans.oms.server.service;

import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.JobInfoRepository;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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

    @Resource
    private JobInfoRepository jobInfoRepository;

    private final Cache<Long, String> jobId2JobNameCache;

    public CacheService() {
        jobId2JobNameCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(1024)
                .build();
    }

    /**
     * 根据 jobId 查询 jobName（不保证数据一致性，或者说只要改了数据必不一致hhh）
     */
    public String getJobName(Long jobId) {
        try {
            return jobId2JobNameCache.get(jobId, () -> {
                Optional<JobInfoDO> jobInfoDOOptional = jobInfoRepository.findById(jobId);
                // 防止缓存穿透 hhh
                return jobInfoDOOptional.map(JobInfoDO::getJobName).orElse("");
            });
        }catch (Exception e) {
            log.error("[CacheService] getJobName for {} failed.", jobId, e);
        }
        return null;
    }
}
