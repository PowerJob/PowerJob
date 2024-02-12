package tech.powerjob.server.web.controller;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.LoginUserHolder;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.auth.plugin.ModifyOrCreateDynamicPermission;
import tech.powerjob.server.auth.plugin.SaveNamespaceGrantPermissionPlugin;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.persistence.PageResult;
import tech.powerjob.server.persistence.QueryConvertUtils;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.persistence.remote.repository.NamespaceRepository;
import tech.powerjob.server.web.converter.NamespaceConverter;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;
import tech.powerjob.server.web.request.ModifyNamespaceRequest;
import tech.powerjob.server.web.request.QueryNamespaceRequest;
import tech.powerjob.server.web.response.NamespaceVO;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    private WebAuthService webAuthService;
    @Resource
    private NamespaceRepository namespaceRepository;

    @ResponseBody
    @PostMapping("/save")
    @ApiPermission(name = "Namespace-Save", roleScope = RoleScope.NAMESPACE, dynamicPermissionPlugin = ModifyOrCreateDynamicPermission.class, grandPermissionPlugin = SaveNamespaceGrantPermissionPlugin.class)
    public ResultDTO<NamespaceVO> save(@RequestBody ModifyNamespaceRequest req) {

        req.valid();

        Long id = req.getId();
        NamespaceDO namespaceDO;

        boolean isCreate = id == null;

        if (isCreate) {
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

            if (!namespaceDO.getCode().equalsIgnoreCase(req.getCode())) {
                throw new IllegalArgumentException("NOT_ALLOW_CHANGE_THE_NAMESPACE_CODE");
            }
        }

        // 拷贝通用变更属性（code 不允许更改）
        namespaceDO.setTags(req.getTags());
        namespaceDO.setName(req.getName());
        namespaceDO.setExtra(req.getExtra());
        namespaceDO.setStatus(Optional.ofNullable(req.getStatus()).orElse(SwitchableStatus.ENABLE.getV()));

        namespaceDO.setGmtModified(new Date());
        NamespaceDO savedNamespace = namespaceRepository.save(namespaceDO);

        // 授权
        webAuthService.processPermissionOnSave(RoleScope.NAMESPACE, savedNamespace.getId(), req.getComponentUserRoleInfo());

        return ResultDTO.success(NamespaceConverter.do2BaseVo(savedNamespace));
    }

    @DeleteMapping("/delete")
    @ApiPermission(name = "Namespace-Delete", roleScope = RoleScope.NAMESPACE, requiredPermission = Permission.SU)
    public ResultDTO<Void> deleteNamespace(Long id) {
        namespaceRepository.deleteById(id);
        return ResultDTO.success(null);
    }

    @PostMapping("/list")
    @ApiPermission(name = "Namespace-List", roleScope = RoleScope.NAMESPACE, requiredPermission = Permission.NONE)
    public ResultDTO<PageResult<NamespaceVO>> listNamespace(@RequestBody QueryNamespaceRequest queryNamespaceRequest) {

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

        PageResult<NamespaceVO> ret = new PageResult<>(namespacePageResult);
        ret.setData(namespacePageResult.get().map(x -> {
            NamespaceVO namespaceVO = NamespaceConverter.do2BaseVo(x);
            fillPermissionInfo(x, namespaceVO);
            return namespaceVO;
        }).collect(Collectors.toList()));

        return ResultDTO.success(ret);
    }

    private void fillPermissionInfo(NamespaceDO namespaceDO, NamespaceVO namespaceVO) {

        Long namespaceId = namespaceVO.getId();

        // 权限用户关系
        ComponentUserRoleInfo componentUserRoleInfo = webAuthService.fetchComponentUserRoleInfo(RoleScope.NAMESPACE, namespaceId);
        namespaceVO.setComponentUserRoleInfo(componentUserRoleInfo);

        // 有权限用户填充 token
        boolean hasPermission = webAuthService.hasPermission(RoleScope.NAMESPACE, namespaceId, Permission.READ);
        namespaceVO.setToken(hasPermission ? namespaceDO.getToken() : AuthConstants.TIPS_NO_PERMISSION_TO_SEE);
    }

    private NamespaceDO fetchById(Long id) {
        Optional<NamespaceDO> namespaceDoOpt = namespaceRepository.findById(id);
        if (!namespaceDoOpt.isPresent()) {
            throw new IllegalArgumentException("can't find namespace by id: " + id);
        }
        return namespaceDoOpt.get();
    }

}
