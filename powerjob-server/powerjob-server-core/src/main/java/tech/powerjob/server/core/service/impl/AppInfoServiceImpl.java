package tech.powerjob.server.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;

import java.util.Objects;

/**
 * AppInfoServiceImpl
 *
 * @author tjq
 * @since 2023/3/4
 */
@Service
@RequiredArgsConstructor
public class AppInfoServiceImpl implements AppInfoService {

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
}
