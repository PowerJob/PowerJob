package tech.powerjob.server.initializer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.powerjob.common.PowerJobDKey;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.service.login.LoginRequest;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionService;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.common.constants.ExtensionKey;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.persistence.remote.model.PwjbUserInfoDO;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;
import tech.powerjob.server.web.request.ModifyAppInfoRequest;
import tech.powerjob.server.web.request.ModifyNamespaceRequest;
import tech.powerjob.server.web.request.ModifyUserInfoRequest;
import tech.powerjob.server.web.service.AppWebService;
import tech.powerjob.server.web.service.NamespaceWebService;
import tech.powerjob.server.web.service.PwjbUserWebService;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private AppWebService appWebService;
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

        // 创建用户
        PowerJobUser powerJobUser = createUser(username, password, true);

        // 授予全局管理员权限
        powerJobPermissionService.grantRole(RoleScope.GLOBAL, AuthConstants.GLOBAL_ADMIN_TARGET_ID, powerJobUser.getId(), Role.ADMIN, null);
        log.info("[SystemInitializeService] GRANT ADMIN to user[{}] successfully!", powerJobUser);

        // 循环10遍，强提醒用户，第一次使用必须更改 admin 密码
        for (int i = 0; i < 10; i++) {
            log.warn("[SystemInitializeService] The system has automatically created a super administrator account[username={},password={}], please login and change the password immediately!", username, password);
        }
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void initNamespace() {

        Optional<NamespaceDO> namespaceDOOptional = namespaceWebService.findByCode(SYSTEM_DEFAULT_NAMESPACE);
        if (namespaceDOOptional.isPresent()) {
            log.info("[SystemInitializeService] namespace[{}] already exist", SYSTEM_DEFAULT_NAMESPACE);
            return;
        }

        ModifyNamespaceRequest saveNamespaceReq = new ModifyNamespaceRequest();
        saveNamespaceReq.setName(SYSTEM_DEFAULT_NAMESPACE);
        saveNamespaceReq.setCode(SYSTEM_DEFAULT_NAMESPACE);

        log.info("[SystemInitializeService] create default namespace by request: {}", saveNamespaceReq);
        NamespaceDO savedNamespaceDO = namespaceWebService.save(saveNamespaceReq);
        log.info("[SystemInitializeService] create default namespace successfully: {}", savedNamespaceDO);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void initTestEnvironment() {
        String testMode = System.getProperty(PowerJobDKey.SERVER_TEST_MODE);
        if (!Boolean.TRUE.toString().equalsIgnoreCase(testMode)) {
            return;
        }
        log.warn("[SystemInitializeService] [TestEnv] Detect test mode and start initializing the test environment");

        // 初始化测试账号
        String testAccountsStr = System.getProperty(PowerJobDKey.SERVER_TEST_ACCOUNT_USERNAME, "powerjob");
        String testAccountsPwd = System.getProperty(PowerJobDKey.SERVER_TEST_ACCOUNT_PASSWORD, "powerjob");
        List<String> testAccounts = Lists.newArrayList(SJ.COMMA_SPLITTER.split(testAccountsStr));

        List<PowerJobUser> testUsers = testAccounts.stream().map(un -> createUser(un, testAccountsPwd, false)).collect(Collectors.toList());
        List<Long> testUserIds = testUsers.stream().map(PowerJobUser::getId).collect(Collectors.toList());

        log.info("[SystemInitializeService] [TestEnv] test user: {}", testUsers);

        String testAppName = System.getProperty(PowerJobDKey.SERVER_TEST_APP_USERNAME, "powerjob-worker-samples");
        String testAppPassword = System.getProperty(PowerJobDKey.SERVER_TEST_APP_PASSWORD, "powerjob123");

        AppInfoDO testApp = createApp(testAppName, testAppPassword, SYSTEM_DEFAULT_NAMESPACE, testUserIds);
        log.info("[SystemInitializeService] [TestEnv] test app: {}", testApp);
    }

    private PowerJobUser createUser(String username, String password, boolean allowedChangePwd) {
        // STEP1: 创建 PWJB 用户

        Optional<PwjbUserInfoDO> pwjbUserOpt = pwjbUserWebService.findByUsername(username);
        PwjbUserInfoDO savedPwjbUser = pwjbUserOpt.orElseGet(() -> {
            ModifyUserInfoRequest createUser = new ModifyUserInfoRequest();
            createUser.setUsername(username);
            createUser.setNick(username);
            createUser.setPassword(password);

            if (!allowedChangePwd) {
                Map<String, Object> extra = Maps.newHashMap();
                extra.put(ExtensionKey.PwjbUser.allowedChangePwd, false);
                createUser.setExtra(JsonUtils.toJSONString(extra));
            }

            log.info("[SystemInitializeService] [username:{}] create PWJB user by request: {}", username, createUser);
            PwjbUserInfoDO nPwjbUser = pwjbUserWebService.save(createUser);
            log.info("[SystemInitializeService] [username:{}]  create PWJB user successfully: {}", username, nPwjbUser);
            return nPwjbUser;
        });
        log.info("[SystemInitializeService] [username:{}] => PwjbUser: {}", username, savedPwjbUser);

        // STEP2: 创建 USER 对象
        Map<String, Object> params = Maps.newHashMap();
        params.put(AuthConstants.PARAM_KEY_USERNAME, username);
        params.put(AuthConstants.PARAM_KEY_PASSWORD, password);

        LoginRequest loginRequest = new LoginRequest()
                .setLoginType(AuthConstants.ACCOUNT_TYPE_POWER_JOB)
                .setOriginParams(JsonUtils.toJSONString(params));
        log.info("[SystemInitializeService] [username:{}] create PowerJobUser by request: {}", username, loginRequest);
        PowerJobUser powerJobUser = powerJobLoginService.doLogin(loginRequest);
        log.info("[SystemInitializeService] [username:{}] create PowerJobUser successfully: {}", username, powerJobUser);
        return powerJobUser;
    }

    private AppInfoDO createApp(String appName, String password, String namespaceCode, List<Long> developers) {

        ModifyAppInfoRequest modifyAppInfoRequest = new ModifyAppInfoRequest();

        // 应用已存在则转为更新模式
        Optional<AppInfoDO> oldAppOpt = appWebService.findByAppName(appName);
        oldAppOpt.ifPresent(appInfoDO -> {
            BeanUtils.copyProperties(appInfoDO, modifyAppInfoRequest);
            modifyAppInfoRequest.setId(appInfoDO.getId());
        });

        modifyAppInfoRequest.setAppName(appName);
        modifyAppInfoRequest.setTitle(appName);

        modifyAppInfoRequest.setPassword(password);
        modifyAppInfoRequest.setNamespaceCode(namespaceCode);

        modifyAppInfoRequest.setTags("test_app");

        // 禁用靠密码成为管理员
        Map<String, Object> extra = Maps.newHashMap();
        extra.put(ExtensionKey.App.allowedBecomeAdminByPassword, false);
        modifyAppInfoRequest.setExtra(JsonUtils.toJSONString(extra));

        ComponentUserRoleInfo componentUserRoleInfo = new ComponentUserRoleInfo();
        modifyAppInfoRequest.setComponentUserRoleInfo(componentUserRoleInfo);

        componentUserRoleInfo.setDeveloper(developers);

        log.info("[SystemInitializeService] [app:{}] create App by request: {}", appName, modifyAppInfoRequest);
        AppInfoDO appInfoDO = appWebService.save(modifyAppInfoRequest);
        log.info("[SystemInitializeService] [app:{}] create App successfully: {}", appName, appInfoDO);

        return appInfoDO;
    }
}
