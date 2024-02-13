package tech.powerjob.server.web.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.common.AuthErrorCode;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.core.service.UserService;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.persistence.remote.repository.NamespaceRepository;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.web.converter.NamespaceConverter;
import tech.powerjob.server.web.converter.UserConverter;
import tech.powerjob.server.web.response.AppBaseVO;
import tech.powerjob.server.web.response.NamespaceBaseVO;
import tech.powerjob.server.web.response.UserBaseVO;
import tech.powerjob.server.web.response.UserDetailVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户信息控制层
 *
 * @author tjq
 * @since 2020/4/12
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {
    @Resource
    private UserService userService;
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

    @GetMapping("/detail")
    public ResultDTO<UserDetailVO> getUserDetail(HttpServletRequest httpServletRequest) {
        Optional<PowerJobUser> powerJobUserOpt = powerJobLoginService.ifLogin(httpServletRequest);
        if (!powerJobUserOpt.isPresent()) {
            throw new PowerJobAuthException(AuthErrorCode.USER_NOT_LOGIN);
        }
        Optional<UserInfoDO> userinfoDoOpt = userInfoRepository.findById(powerJobUserOpt.get().getId());
        if (!userinfoDoOpt.isPresent()) {
            throw new IllegalArgumentException("can't find user by id: " + powerJobUserOpt.get().getId());
        }
        UserDetailVO userDetailVO = new UserDetailVO();
        BeanUtils.copyProperties(userinfoDoOpt.get(), userDetailVO);

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
                NamespaceBaseVO namespaceBaseVO = JsonUtils.parseObjectIgnoreException(JsonUtils.toJSONString(NamespaceConverter.do2BaseVo(namespaceDO)), NamespaceBaseVO.class);
                namespaceBaseVO.genFrontName();
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

    private static List<UserBaseVO> convert(List<UserInfoDO> data) {
        if (CollectionUtils.isEmpty(data)) {
            return Lists.newLinkedList();
        }
        return data.stream().map(UserConverter::do2BaseVo).collect(Collectors.toList());
    }

    private static Set<Long> mergeIds(Map<?, List<Long>> map) {
        Set<Long> ids = Sets.newHashSet();
        map.values().forEach(ids::addAll);
        return ids;
    }
}
