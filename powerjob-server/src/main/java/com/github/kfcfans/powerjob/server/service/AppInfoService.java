package com.github.kfcfans.powerjob.server.service;

import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.server.persistence.core.model.AppInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.AppInfoRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 应用信息服务
 *
 * @author tjq
 * @since 2020/6/20
 */
@Service
public class AppInfoService {

    @Resource
    private AppInfoRepository appInfoRepository;

    /**
     * 验证应用访问权限
     * @param appName 应用名称
     * @param password 密码
     * @return 应用ID
     */
    public Long assertApp(String appName, String password) {

        AppInfoDO appInfo = appInfoRepository.findByAppName(appName).orElseThrow(() -> new PowerJobException("can't find appInfo by appName: " + appName));
        if (Objects.equals(appInfo.getPassword(), password)) {
            return appInfo.getId();
        }
        throw new PowerJobException("password error!");
    }

}
