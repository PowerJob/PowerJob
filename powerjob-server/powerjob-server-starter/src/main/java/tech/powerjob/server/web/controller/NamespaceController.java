package tech.powerjob.server.web.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.auth.interceptor.dp.ModifyOrCreateDynamicPermission;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.model.UserRoleDO;
import tech.powerjob.server.persistence.remote.repository.NamespaceRepository;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.persistence.remote.repository.UserRoleRepository;
import tech.powerjob.server.web.converter.NamespaceConverter;
import tech.powerjob.server.web.converter.UserConverter;
import tech.powerjob.server.web.request.ModifyNamespaceRequest;
import tech.powerjob.server.web.response.NamespaceBaseVO;
import tech.powerjob.server.web.response.NamespaceDetailVO;
import tech.powerjob.server.web.response.UserBaseVO;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 命名空间 Controller
 *
 * @author tjq
 * @since 2023/9/3
 */
@Slf4j
@RestController
@RequestMapping("/namespace")
public class NamespaceController {

    @Resource
    private NamespaceRepository namespaceRepository;
    @Resource
    private UserInfoRepository userInfoRepository;
    @Resource
    private UserRoleRepository userRoleRepository;

    @PostMapping("/save")
    @ApiPermission(name = "Namespace-Save", dynamicPermissionPlugin = ModifyOrCreateDynamicPermission.class)
    public ResultDTO<Void> save(ModifyNamespaceRequest req) {

        req.valid();

        Long id = req.getId();
        NamespaceDO namespaceDO;
        if (id == null) {
            namespaceDO = new NamespaceDO();
            namespaceDO.setGmtCreate(new Date());

            // code 单独拷贝
            namespaceDO.setCode(req.getCode());
            // 创建时生成 token
            namespaceDO.setToken(UUID.randomUUID().toString());

        } else {
            namespaceDO = fetchById(id);
        }

        // 拷贝通用变更属性（code 不允许更改）
        namespaceDO.setName(req.getName());
        namespaceDO.setExtra(req.getExtra());
        namespaceDO.setStatus(req.getStatus());

        namespaceDO.setGmtModified(new Date());
        namespaceRepository.save(namespaceDO);
        return ResultDTO.success(null);
    }

    @GetMapping("/listAll")
    public ResultDTO<List<NamespaceBaseVO>> listAllNamespace() {
        List<NamespaceDO> allDos = namespaceRepository.findAll();
        return ResultDTO.success(allDos.stream().map(NamespaceConverter::do2BaseVo).collect(Collectors.toList()));
    }

    @GetMapping("/detail")
    @ApiPermission(name = "Namespace-DetailInfo", requiredPermission = Permission.READ)
    public ResultDTO<NamespaceDetailVO> queryNamespaceDetail(Long id) {

        NamespaceDO namespaceDO = fetchById(id);
        NamespaceDetailVO namespaceDetailVO = new NamespaceDetailVO();

        // 拷贝基础字段
        NamespaceBaseVO namespaceBaseVO = NamespaceConverter.do2BaseVo(namespaceDO);
        BeanUtils.copyProperties(namespaceBaseVO, namespaceDetailVO);

        // 处理 token
        namespaceDetailVO.setToken(namespaceDO.getToken());

        // 处理权限视图
        Map<String, List<UserBaseVO>> privilegedUsers = Maps.newHashMap();
        namespaceDetailVO.setPrivilegedUsers(privilegedUsers);
        List<UserRoleDO> permissionUserList = userRoleRepository.findAllByScopeAndTarget(RoleScope.NAMESPACE.getV(), namespaceDO.getId());
        permissionUserList.forEach(r -> {
            Role role = Role.of(r.getRole());
            List<UserBaseVO> userBaseVOList = privilegedUsers.computeIfAbsent(role.name(), ignore -> Lists.newArrayList());

            Optional<UserInfoDO> userInfoDoOpt = userInfoRepository.findById(r.getUserId());
            userInfoDoOpt.ifPresent(userInfoDO -> userBaseVOList.add(UserConverter.do2BaseVo(userInfoDO)));
        });

        return ResultDTO.success(namespaceDetailVO);
    }

    private NamespaceDO fetchById(Long id) {
        Optional<NamespaceDO> namespaceDoOpt = namespaceRepository.findById(id);
        if (!namespaceDoOpt.isPresent()) {
            throw new IllegalArgumentException("can't find namespace by id: " + id);
        }
        return namespaceDoOpt.get();
    }

}
