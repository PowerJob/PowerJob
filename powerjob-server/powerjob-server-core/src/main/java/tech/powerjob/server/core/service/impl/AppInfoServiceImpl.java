package tech.powerjob.server.core.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * AppInfoServiceImpl
 *
 * @author tjq
 * @since 2023/3/4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppInfoServiceImpl implements AppInfoService {

    private final Cache<Long, AppInfoDO> appId2AppInfoDO = CacheBuilder.newBuilder()
            .softValues()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .maximumSize(1024)
            .build();

    private final AppInfoRepository appInfoRepository;

    /**
     * 验证应用访问权限
     * @param appName 应用名称
     * @param password 密码
     * @return 应用ID
     */
    @Override
    public Long assertApp(String appName, String password) {

        AppInfoDO appInfo = appInfoRepository.findByAppName(appName).orElseThrow(() -> new PowerJobException("can't find appInfo by appName: " + appName));
        if (Objects.equals(appInfo.getPassword(), password)) {
            return appInfo.getId();
        }
        throw new PowerJobException("password error!");
    }

    @Override
    public Optional<AppInfoDO> findByAppName(String appName) {
        return appInfoRepository.findByAppName(appName);
    }

    @Override
    public Optional<AppInfoDO> findByIdWithCache(Long appId) {
        try {
            AppInfoDO appInfoDO = appId2AppInfoDO.get(appId, () -> {
                Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(appId);
                if (appInfoOpt.isPresent()) {
                    return appInfoOpt.get();
                }
                throw new IllegalArgumentException("can't find appInfo by appId:" + appId);
            });
            return Optional.of(appInfoDO);
        } catch (Exception e) {
            log.warn("[AppInfoService] findByIdWithCache failed,appId={}", appId, e);
        }
        return Optional.empty();
    }

}
