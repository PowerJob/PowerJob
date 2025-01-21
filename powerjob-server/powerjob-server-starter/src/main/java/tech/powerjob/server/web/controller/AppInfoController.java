package tech.powerjob.server.web.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.MapUtils;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.common.utils.HttpHeaderUtils;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.auth.plugin.ModifyOrCreateDynamicPermission;
import tech.powerjob.server.auth.plugin.SaveAppGrantPermissionPlugin;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.common.constants.ExtensionKey;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.persistence.PageResult;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.web.converter.NamespaceConverter;
import tech.powerjob.server.web.request.AppAssertRequest;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;
import tech.powerjob.server.web.request.ModifyAppInfoRequest;
import tech.powerjob.server.web.request.QueryAppInfoRequest;
import tech.powerjob.server.web.response.AppInfoVO;
import tech.powerjob.server.web.response.NamespaceBaseVO;
import tech.powerjob.server.web.response.UserBaseVO;
import tech.powerjob.server.web.service.AppWebService;
import tech.powerjob.server.web.service.NamespaceWebService;
import tech.powerjob.server.web.service.UserWebService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AppName Controller
 * vue axios 的POST请求必须使用 @RequestBody 接收
 *
 * @author tjq
 * @since 2020/4/1
 */
@Slf4j
@RestController
@RequestMapping("/appInfo")
@RequiredArgsConstructor
public class AppInfoController {

    private final AppWebService appWebService;
    private final WebAuthService webAuthService;

    private final UserWebService userWebService;

    private final AppInfoService appInfoService;

    private final NamespaceWebService namespaceWebService;

    @PostMapping("/save")
    @ApiPermission(name = "App-Save", roleScope = RoleScope.APP, dynamicPermissionPlugin = ModifyOrCreateDynamicPermission.class, grandPermissionPlugin = SaveAppGrantPermissionPlugin.class)
    public ResultDTO<AppInfoVO> saveAppInfo(@RequestBody ModifyAppInfoRequest req) {

        AppInfoDO savedAppInfo = appWebService.save(req);

        return ResultDTO.success(convert(Lists.newArrayList(savedAppInfo), false).get(0));
    }

    @PostMapping("/delete")
    @ApiPermission(name = "App-Delete", roleScope = RoleScope.APP, requiredPermission = Permission.SU)
    public ResultDTO<Void> deleteApp(HttpServletRequest hsr) {

        Long appId = Long.valueOf(HttpHeaderUtils.fetchAppId(hsr));
        appWebService.delete(appId);

        return ResultDTO.success(null);
    }

    @PostMapping("/list")
    @ApiPermission(name = "App-List", roleScope = RoleScope.APP, requiredPermission = Permission.NONE)
    public ResultDTO<PageResult<AppInfoVO>> listAppInfoByQuery(@RequestBody QueryAppInfoRequest queryAppInfoRequest) {

        Page<AppInfoDO> pageAppInfoResult = appWebService.list(queryAppInfoRequest);

        PageResult<AppInfoVO> pageRet = new PageResult<>(pageAppInfoResult);

        List<AppInfoDO> appInfoDos = pageAppInfoResult.get().collect(Collectors.toList());
        pageRet.setData(convert(appInfoDos, true));

        return ResultDTO.success(pageRet);
    }

    @PostMapping("/becomeAdmin")
    @ApiPermission(name = "App-BecomeAdmin", roleScope = RoleScope.GLOBAL, requiredPermission = Permission.NONE)
    public ResultDTO<Void> becomeAdminByAppNameAndPassword(@RequestBody AppAssertRequest appAssertRequest) {
        String appName = appAssertRequest.getAppName();

        Optional<AppInfoDO> appOpt = appWebService.findByAppName(appName);
        if (!appOpt.isPresent()) {
            throw new PowerJobException(ErrorCodes.ILLEGAL_ARGS_ERROR, "can't find appInfo by appName: " + appName);
        }

        String appExtra = appOpt.get().getExtra();
        if (StringUtils.isNotBlank(appExtra)) {
            Map<String, Object> appExtraMap = JsonUtils.parseMap(appExtra);
            Boolean allowedBecomeAdminByPassword = MapUtils.getBoolean(appExtraMap, ExtensionKey.App.allowedBecomeAdminByPassword, true);
            if (!allowedBecomeAdminByPassword) {
                throw new PowerJobException(ErrorCodes.OPERATION_NOT_PERMITTED, "allowedBecomeAdminByPassword=false");
            }
        }

        Long appId = appInfoService.assertApp(appName, appAssertRequest.getPassword(), appAssertRequest.getEncryptType());

        Map<String, Object> extra = Maps.newHashMap();
        extra.put("source", "becomeAdminByAppNameAndPassword");

        webAuthService.grantRole2LoginUser(RoleScope.APP, appId, Role.ADMIN, JsonUtils.toJSONString(extra));

        return ResultDTO.success(null);
    }

    private List<AppInfoVO> convert(List<AppInfoDO> data, boolean fillDetail) {
        if (CollectionUtils.isEmpty(data)) {
            return Lists.newLinkedList();
        }

        // app 界面使用频率不高，数据库操作 rt 也不会太长，展示不考虑性能问题，简单期间串行补全
        return data.stream().map(appInfoDO -> {
            AppInfoVO appInfoVO = new AppInfoVO();
            BeanUtils.copyProperties(appInfoDO, appInfoVO);

            appInfoVO.setGmtCreateStr(CommonUtils.formatTime(appInfoDO.getGmtCreate()));
            appInfoVO.setGmtModifiedStr(CommonUtils.formatTime(appInfoDO.getGmtModified()));

            if (fillDetail) {
                // 人员面板
                ComponentUserRoleInfo componentUserRoleInfo = webAuthService.fetchComponentUserRoleInfo(RoleScope.APP, appInfoDO.getId());
                appInfoVO.setComponentUserRoleInfo(componentUserRoleInfo);

                // 密码
                boolean hasPermission = webAuthService.hasPermission(RoleScope.APP, appInfoDO.getId(), Permission.READ);
                String originPassword = appInfoService.fetchOriginAppPassword(appInfoDO);
                appInfoVO.setPassword(hasPermission ? originPassword : AuthConstants.TIPS_NO_PERMISSION_TO_SEE);

                // namespace
                Optional<NamespaceDO> namespaceOpt = namespaceWebService.findById(appInfoDO.getNamespaceId());
                if (namespaceOpt.isPresent()) {
                    NamespaceBaseVO baseNamespace = NamespaceConverter.do2BaseVo(namespaceOpt.get());
                    appInfoVO.setNamespace(baseNamespace);
                    appInfoVO.setNamespaceName(baseNamespace.getName());
                }

                // user 信息
                appInfoVO.setCreatorShowName(userWebService.fetchBaseUserInfo(appInfoDO.getCreator()).map(UserBaseVO::getShowName).orElse(null));
                appInfoVO.setModifierShowName(userWebService.fetchBaseUserInfo(appInfoDO.getModifier()).map(UserBaseVO::getShowName).orElse(null));

            }

            return appInfoVO;
        }).collect(Collectors.toList());
    }

}
