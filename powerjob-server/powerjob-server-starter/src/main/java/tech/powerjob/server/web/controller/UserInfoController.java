package tech.powerjob.server.web.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.persistence.remote.repository.NamespaceRepository;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.web.converter.NamespaceConverter;
import tech.powerjob.server.web.converter.UserConverter;
import tech.powerjob.server.web.request.ModifyUserInfoRequest;
import tech.powerjob.server.web.request.QueryUserRequest;
import tech.powerjob.server.web.response.AppBaseVO;
import tech.powerjob.server.web.response.NamespaceBaseVO;
import tech.powerjob.server.web.response.UserBaseVO;
import tech.powerjob.server.web.response.UserDetailVO;
import tech.powerjob.server.web.service.UserWebService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户信息控制层
 *
 * @author tjq
 * @since 2020/4/12
 */

@Slf4j
@RestController
@RequestMapping("/user")
public class UserInfoController {

    @Resource
    private UserWebService userWebService;
    @Resource
    private UserInfoRepository userInfoRepository;
    @Resource
    private PowerJobLoginService powerJobLoginService;
    @Resource
    private WebAuthService webAuthService;
    @Resource
    private NamespaceRepository namespaceRepository;
    @Resource
    private AppInfoRepository appInfoRepository;

    @SneakyThrows
    @PostMapping("/modify")
    public ResultDTO<Void> modifyUser(@RequestBody ModifyUserInfoRequest modifyUserInfoRequest, HttpServletRequest httpServletRequest) {

        Long userId = modifyUserInfoRequest.getId();
        checkModifyUserPermission(userId, httpServletRequest);

        Optional<UserInfoDO> userOpt = userInfoRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("can't find user by userId:" + userId);
        }

        UserInfoDO dbUser = userOpt.get();

        // 拷入允许修改的内容
        if (StringUtils.isNotEmpty(modifyUserInfoRequest.getNick())) {
            dbUser.setNick(modifyUserInfoRequest.getNick());
        }
        if (StringUtils.isNotEmpty(modifyUserInfoRequest.getPhone())) {
            dbUser.setPhone(modifyUserInfoRequest.getPhone());
        }
        if (StringUtils.isNotEmpty(modifyUserInfoRequest.getEmail())) {
            dbUser.setEmail(modifyUserInfoRequest.getEmail());
        }
        if (StringUtils.isNotEmpty(modifyUserInfoRequest.getWebHook())) {
            dbUser.setWebHook(modifyUserInfoRequest.getWebHook());
        }
        if (StringUtils.isNotEmpty(modifyUserInfoRequest.getExtra())) {
            dbUser.setExtra(modifyUserInfoRequest.getExtra());
        }

        dbUser.setGmtModified(new Date());
        userInfoRepository.saveAndFlush(dbUser);

        return ResultDTO.success(null);
    }

    @GetMapping("/list")
    public ResultDTO<List<UserBaseVO>> list(@RequestParam(required = false) String name) {

        List<UserInfoDO> result;
        if (StringUtils.isEmpty(name)) {
            result = userInfoRepository.findAll();
        }else {
            result = userInfoRepository.findByUsernameLike("%" + name + "%");
        }
        return ResultDTO.success(convert(result));
    }

    /**
     * 查询用户信息（用于管理员操作，会返回敏感信息）
     * @param queryUserRequest 查询请求
     * @return 响应
     */
    @PostMapping("/query")
    @ApiPermission(name = "User-Query", roleScope = RoleScope.GLOBAL, requiredPermission = Permission.SU)
    public ResultDTO<List<UserBaseVO>> query(@RequestBody QueryUserRequest queryUserRequest) {
        List<UserInfoDO> userInfoDos = userWebService.list(queryUserRequest);
        List<UserBaseVO> userBaseVOS = userInfoDos.stream().map(x -> UserConverter.do2BaseVo(x, true)).collect(Collectors.toList());
        return ResultDTO.success(userBaseVOS);
    }

    @GetMapping("/detail")
    public ResultDTO<UserDetailVO> getUserDetail(HttpServletRequest httpServletRequest) {
        Optional<PowerJobUser> powerJobUserOpt = powerJobLoginService.ifLogin(httpServletRequest);
        if (!powerJobUserOpt.isPresent()) {
            throw new PowerJobAuthException(ErrorCodes.USER_NOT_LOGIN);
        }
        Optional<UserInfoDO> userinfoDoOpt = userInfoRepository.findById(powerJobUserOpt.get().getId());
        if (!userinfoDoOpt.isPresent()) {
            throw new IllegalArgumentException("can't find user by id: " + powerJobUserOpt.get().getId());
        }
        UserDetailVO userDetailVO = new UserDetailVO();
        BeanUtils.copyProperties(userinfoDoOpt.get(), userDetailVO);
        userDetailVO.genShowName();

        // 权限信息
        Map<Role, List<Long>> globalPermissions = webAuthService.fetchMyPermissionTargets(RoleScope.GLOBAL);
        userDetailVO.setGlobalRoles(globalPermissions.keySet().stream().map(Enum::name).collect(Collectors.toList()));

        Map<Role, List<Long>> namespacePermissions = webAuthService.fetchMyPermissionTargets(RoleScope.NAMESPACE);
        List<NamespaceDO> nsList = namespaceRepository.findAllByIdIn(mergeIds(namespacePermissions));
        Map<Long, NamespaceDO> id2NamespaceDo = Maps.newHashMap();
        nsList.forEach(x -> id2NamespaceDo.put(x.getId(), x));
        Map<String, List<NamespaceBaseVO>> role2NamespaceBaseVo = Maps.newHashMap();
        namespacePermissions.forEach((k, v) -> {
            List<NamespaceBaseVO> namespaceBaseVOS = Lists.newArrayList();
            role2NamespaceBaseVo.put(k.name(), namespaceBaseVOS);
            v.forEach(nId -> {
                NamespaceDO namespaceDO = id2NamespaceDo.get(nId);
                if (namespaceDO == null) {
                    return;
                }
                NamespaceBaseVO namespaceBaseVO = NamespaceConverter.do2BaseVo(namespaceDO);
                namespaceBaseVOS.add(namespaceBaseVO);
            });
        });
        userDetailVO.setRole2NamespaceList(role2NamespaceBaseVo);

        Map<Role, List<Long>> appPermissions = webAuthService.fetchMyPermissionTargets(RoleScope.APP);
        List<AppInfoDO> appList = appInfoRepository.findAllByIdIn(mergeIds(appPermissions));
        Map<Long, AppInfoDO> id2AppInfo = Maps.newHashMap();
        appList.forEach(x -> id2AppInfo.put(x.getId(), x));
        Map<String, List<AppBaseVO>> role2AppBaseVo = Maps.newHashMap();
        appPermissions.forEach((k, v) -> {
            List<AppBaseVO> appBaseVOS = Lists.newArrayList();
            role2AppBaseVo.put(k.name(), appBaseVOS);
            v.forEach(nId -> {
                AppInfoDO appInfoDO = id2AppInfo.get(nId);
                if (appInfoDO == null) {
                    return;
                }
                AppBaseVO appBaseVO = new AppBaseVO();
                BeanUtils.copyProperties(appInfoDO, appBaseVO);
                appBaseVOS.add(appBaseVO);
            });
        });
        userDetailVO.setRole2AppList(role2AppBaseVo);

        return ResultDTO.success(userDetailVO);
    }

    @PostMapping("/disable")
    public ResultDTO<Void> disableUser(Long uid, HttpServletRequest httpServletRequest) {
        changeAccountStatus(uid, SwitchableStatus.DISABLE, httpServletRequest);
        return ResultDTO.success(null);
    }

    @PostMapping("/enable")
    public ResultDTO<Void> enableUser(Long uid, HttpServletRequest httpServletRequest) {
        changeAccountStatus(uid, SwitchableStatus.ENABLE, httpServletRequest);
        return ResultDTO.success(null);
    }

    private void changeAccountStatus(Long uid, SwitchableStatus targetStatus, HttpServletRequest httpServletRequest) {
        checkModifyUserPermission(uid, httpServletRequest);

        Optional<UserInfoDO> userOpt = userInfoRepository.findById(uid);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("can't find user by userId:" + uid);
        }

        UserInfoDO dbUser = userOpt.get();

        dbUser.setStatus(targetStatus.getV());
        dbUser.setGmtModified(new Date());

        userInfoRepository.saveAndFlush(dbUser);
        log.info("[UserInfoController] changeAccountStatus, userId={},targetStatus={}", uid, targetStatus);
    }

    /**
     * 检查针对 user 处理的权限
     * @param uid 目标 userId
     * @param httpServletRequest http 上下文请求
     */
    private void checkModifyUserPermission(Long uid, HttpServletRequest httpServletRequest) {
        Optional<PowerJobUser> powerJobUserOpt = powerJobLoginService.ifLogin(httpServletRequest);
        if (!powerJobUserOpt.isPresent()) {
            throw new PowerJobAuthException(ErrorCodes.USER_NOT_LOGIN);
        }
        PowerJobUser currentLoginUser = powerJobUserOpt.get();

        boolean myself = uid.equals(currentLoginUser.getId());
        boolean globalAdmin = webAuthService.isGlobalAdmin();

        if (myself || globalAdmin) {
            return;
        }

        throw new PowerJobException("Only the administrator and account owner can modify the account");
    }

    private static List<UserBaseVO> convert(List<UserInfoDO> data) {
        if (CollectionUtils.isEmpty(data)) {
            return Lists.newLinkedList();
        }
        return data.stream().map(x -> UserConverter.do2BaseVo(x, false)).collect(Collectors.toList());
    }

    private static Set<Long> mergeIds(Map<?, List<Long>> map) {
        Set<Long> ids = Sets.newHashSet();
        map.values().forEach(ids::addAll);
        return ids;
    }
}
