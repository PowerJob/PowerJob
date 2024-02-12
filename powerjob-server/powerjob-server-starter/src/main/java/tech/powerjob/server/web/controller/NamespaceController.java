package tech.powerjob.server.web.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.LoginUserHolder;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.dp.ModifyOrCreateDynamicPermission;
import tech.powerjob.server.auth.gp.SaveNamespaceGrantPermissionPlugin;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.persistence.PageResult;
import tech.powerjob.server.persistence.QueryConvertUtils;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.model.UserRoleDO;
import tech.powerjob.server.persistence.remote.repository.NamespaceRepository;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.persistence.remote.repository.UserRoleRepository;
import tech.powerjob.server.web.converter.NamespaceConverter;
import tech.powerjob.server.web.converter.UserConverter;
import tech.powerjob.server.web.request.ModifyNamespaceRequest;
import tech.powerjob.server.web.request.QueryNamespaceRequest;
import tech.powerjob.server.web.response.NamespaceBaseVO;
import tech.powerjob.server.web.response.NamespaceDetailVO;
import tech.powerjob.server.web.response.UserBaseVO;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
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

    @ResponseBody
    @PostMapping("/save")
    @ApiPermission(name = "Namespace-Save", roleScope = RoleScope.NAMESPACE, dynamicPermissionPlugin = ModifyOrCreateDynamicPermission.class, grandPermissionPlugin = SaveNamespaceGrantPermissionPlugin.class)
    public ResultDTO<NamespaceBaseVO> save(@RequestBody ModifyNamespaceRequest req) {

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
            namespaceDO.setCreator(LoginUserHolder.getUserName());

        } else {
            namespaceDO = fetchById(id);
            namespaceDO.setModifier(LoginUserHolder.getUserName());
        }

        // 拷贝通用变更属性（code 不允许更改）
        namespaceDO.setName(req.getName());
        namespaceDO.setExtra(req.getExtra());
        namespaceDO.setStatus(Optional.ofNullable(req.getStatus()).orElse(SwitchableStatus.ENABLE.getV()));

        namespaceDO.setGmtModified(new Date());
        NamespaceDO savedNamespace = namespaceRepository.save(namespaceDO);
        return ResultDTO.success(NamespaceConverter.do2BaseVo(savedNamespace));
    }

    @PostMapping("/list")
    public ResultDTO<PageResult<NamespaceBaseVO>> listNamespace(@RequestBody QueryNamespaceRequest queryNamespaceRequest) {

        String codeLike = queryNamespaceRequest.getCodeLike();
        String nameLike = queryNamespaceRequest.getNameLike();
        String tagLike = queryNamespaceRequest.getTagLike();

        Pageable pageable = PageRequest.of(queryNamespaceRequest.getIndex(), queryNamespaceRequest.getPageSize());
        Specification<NamespaceDO> specification = (root, query, cb) -> {

            List<Predicate> predicates = Lists.newArrayList();

            if (StringUtils.isNotEmpty(codeLike)) {
                predicates.add(cb.like(root.get("code"), QueryConvertUtils.convertLikeParams(codeLike)));
            }

            if (StringUtils.isNotEmpty(nameLike)) {
                predicates.add(cb.like(root.get("name"), QueryConvertUtils.convertLikeParams(nameLike)));
            }
            if (StringUtils.isNotEmpty(tagLike)) {
                predicates.add(cb.like(root.get("tags"), QueryConvertUtils.convertLikeParams(tagLike)));
            }

            if (predicates.isEmpty()) {
                return null;
            }
            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };

        Page<NamespaceDO> namespacePageResult = namespaceRepository.findAll(specification, pageable);

        PageResult<NamespaceBaseVO> ret = new PageResult<>(namespacePageResult);
        ret.setData(namespacePageResult.get().map(NamespaceConverter::do2BaseVo).collect(Collectors.toList()));

        return ResultDTO.success(ret);
    }

    @GetMapping("/detail")
    @ApiPermission(name = "Namespace-GetDetail", roleScope = RoleScope.NAMESPACE, requiredPermission = Permission.READ)
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
