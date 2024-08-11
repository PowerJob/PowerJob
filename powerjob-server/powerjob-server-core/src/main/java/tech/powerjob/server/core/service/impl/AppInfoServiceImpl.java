package tech.powerjob.server.core.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.common.enums.EncryptType;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.utils.DigestUtils;
import tech.powerjob.server.common.utils.AESUtil;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;

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

    private static final String ENCRYPT_KEY = "ChinaNo.1_ChinaNo.1_ChinaNo.1AAA";

    private static final String ENCRYPT_PWD_PREFIX = "sys_encrypt_aes:";

    @Override
    public Optional<AppInfoDO> findByAppName(String appName) {
        return appInfoRepository.findByAppName(appName);
    }

    @Override
    public Optional<AppInfoDO> findById(Long appId, boolean useCache) {
        if (!useCache) {
            Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(appId);
            appInfoOpt.ifPresent(appInfo -> appId2AppInfoDO.put(appId, appInfo));
            return appInfoOpt;
        }
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

    @Override
    public void deleteById(Long appId) {
        appInfoRepository.deleteById(appId);
    }

    @Override
    public AppInfoDO save(AppInfoDO appInfo) {

        String originPassword = appInfo.getPassword();
        String encryptPassword = AESUtil.encrypt(originPassword, ENCRYPT_KEY);
        String finalPassword = ENCRYPT_PWD_PREFIX.concat(encryptPassword);
        appInfo.setPassword(finalPassword);

        return appInfoRepository.saveAndFlush(appInfo);
    }

    @Override
    public Long assertApp(String appName, String password, String encryptType) {
        AppInfoDO appInfo = appInfoRepository.findByAppName(appName).orElseThrow(() -> new PowerJobException(ErrorCodes.INVALID_APP, appName));
        return assertApp(appInfo, password, encryptType);
    }

    @Override
    public Long assertApp(AppInfoDO appInfo, String password, String encryptType) {
        boolean checkPass = checkPassword(appInfo, password, encryptType);
        if (!checkPass) {
            throw new PowerJobException(ErrorCodes.INCORRECT_PASSWORD, null);
        }
        return appInfo.getId();
    }

    private boolean checkPassword(AppInfoDO appInfo, String password, String encryptType) {
        String originPwd = fetchOriginAppPassword(appInfo);
        if (StringUtils.isEmpty(encryptType) || EncryptType.NONE.getCode().equalsIgnoreCase(encryptType)) {
            return password.equals(originPwd);
        }
        if (EncryptType.MD5.getCode().equalsIgnoreCase(encryptType)) {
            return password.equalsIgnoreCase(DigestUtils.md5(originPwd));
        }
        throw new PowerJobException(ErrorCodes.INVALID_REQUEST, "unknown_encryptType:" + encryptType);
    }

    @Override
    public String fetchOriginAppPassword(AppInfoDO appInfo) {
        String dbPwd = appInfo.getPassword();
        if (StringUtils.isEmpty(dbPwd)) {
            return dbPwd;
        }

        if (dbPwd.startsWith(ENCRYPT_PWD_PREFIX)) {
            String encryptPassword = dbPwd.replaceFirst(ENCRYPT_PWD_PREFIX, StringUtils.EMPTY);
            return AESUtil.decrypt(encryptPassword, ENCRYPT_KEY);
        }

        return dbPwd;
    }

}
