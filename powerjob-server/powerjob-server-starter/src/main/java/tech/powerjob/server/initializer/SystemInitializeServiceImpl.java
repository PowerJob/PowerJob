package tech.powerjob.server.initializer;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.service.login.LoginRequest;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionService;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.persistence.remote.model.PwjbUserInfoDO;
import tech.powerjob.server.web.request.ModifyNamespaceRequest;
import tech.powerjob.server.web.request.ModifyUserInfoRequest;
import tech.powerjob.server.web.service.NamespaceWebService;
import tech.powerjob.server.web.service.PwjbUserWebService;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.Map;

/**
 * 初始化 PowerJob 首次部署相关的内容
 * 为了可维护性足够高，统一使用 WEB 请求进行初始化，不直接操作底层，防止后续内部逻辑变更后出现问题
 *
 * @author tjq
 * @since 2024/2/15
 */
@Slf4j
@Service
public class SystemInitializeServiceImpl implements SystemInitializeService {

    @Value("${oms.auth.initiliaze.admin.password:#{null}}")
    private String defaultAdminPassword;
    @Resource
    private PwjbUserWebService pwjbUserWebService;
    @Resource
    private NamespaceWebService namespaceWebService;
    @Resource
    private PowerJobLoginService powerJobLoginService;
    @Resource
    private PowerJobPermissionService powerJobPermissionService;

    private static final String SYSTEM_ADMIN_NAME = "ADMIN";


    private static final String SYSTEM_DEFAULT_NAMESPACE = "default_namespace";

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void initAdmin() {

        String username = SYSTEM_ADMIN_NAME;
        String password = StringUtils.isEmpty(defaultAdminPassword) ? RandomStringUtils.randomAlphabetic(8) : defaultAdminPassword;

        // STEP1: 创建 PWJB 用户
        ModifyUserInfoRequest createUser = new ModifyUserInfoRequest();
        createUser.setUsername(username);
        createUser.setNick(username);
        createUser.setPassword(password);

        log.info("[SystemInitializeService] [S1] create default PWJB user by request: {}", createUser);
        PwjbUserInfoDO savedPwjbUser = pwjbUserWebService.save(createUser);
        log.info("[SystemInitializeService] [S1] create default PWJB user successfully: {}", savedPwjbUser);

        Map<String, Object> params = Maps.newHashMap();
        params.put(AuthConstants.PARAM_KEY_USERNAME, username);
        params.put(AuthConstants.PARAM_KEY_PASSWORD, password);

        // STEP2: 创建 USER 对象
        LoginRequest loginRequest = new LoginRequest()
                .setLoginType(AuthConstants.ACCOUNT_TYPE_POWER_JOB)
                .setOriginParams(JsonUtils.toJSONString(params));
        log.info("[SystemInitializeService] [S2] createPowerJobUser user by request: {}", loginRequest);
        PowerJobUser powerJobUser = powerJobLoginService.doLogin(loginRequest);
        log.info("[SystemInitializeService] [S2] createPowerJobUser successfully: {}", powerJobUser);

        // STEP3: 授予全局管理员权限
        powerJobPermissionService.grantRole(RoleScope.GLOBAL, AuthConstants.GLOBAL_ADMIN_TARGET_ID, powerJobUser.getId(), Role.ADMIN, null);
        log.info("[SystemInitializeService] [S3] GRANT ADMIN successfully!");

        // 循环10遍，强提醒用户，第一次使用必须更改 admin 密码
        for (int i = 0; i < 10; i++) {
            log.warn("[SystemInitializeService] The system has automatically created a super administrator account[username={},password={}], please log in and change the password immediately!", username, password);
        }
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void initNamespace() {
        ModifyNamespaceRequest saveNamespaceReq = new ModifyNamespaceRequest();
        saveNamespaceReq.setName(SYSTEM_DEFAULT_NAMESPACE);
        saveNamespaceReq.setCode(SYSTEM_DEFAULT_NAMESPACE);

        log.info("[SystemInitializeService] create default namespace by request: {}", saveNamespaceReq);
        NamespaceDO savedNamespaceDO = namespaceWebService.save(saveNamespaceReq);
        log.info("[SystemInitializeService] create default namespace successfully: {}", savedNamespaceDO);
    }
}
