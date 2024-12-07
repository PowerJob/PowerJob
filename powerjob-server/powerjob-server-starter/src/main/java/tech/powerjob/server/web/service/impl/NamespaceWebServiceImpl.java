package tech.powerjob.server.web.service.impl;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.exception.PowerJobExceptionLauncher;
import tech.powerjob.server.auth.LoginUserHolder;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.persistence.QueryConvertUtils;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.persistence.remote.repository.NamespaceRepository;
import tech.powerjob.server.web.request.ModifyNamespaceRequest;
import tech.powerjob.server.web.request.QueryNamespaceRequest;
import tech.powerjob.server.web.service.NamespaceWebService;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * NamespaceWebService
 *
 * @author tjq
 * @since 2024/2/15
 */
@Service
public class NamespaceWebServiceImpl implements NamespaceWebService {

    @Resource
    private WebAuthService webAuthService;
    @Resource
    private AppInfoRepository appInfoRepository;
    @Resource
    private NamespaceRepository namespaceRepository;

    @Override
    public NamespaceDO save(ModifyNamespaceRequest req) {
        req.valid();

        Long id = req.getId();
        NamespaceDO namespaceDO;

        boolean isCreate = id == null;

        if (isCreate) {

            namespaceRepository.findByCode(req.getCode()).ifPresent(x -> new PowerJobExceptionLauncher(ErrorCodes.ILLEGAL_ARGS_ERROR, String.format("namespace[%s] already exists", req.getCode())));

            namespaceDO = new NamespaceDO();
            namespaceDO.setGmtCreate(new Date());

            // code 单独拷贝
            namespaceDO.setCode(req.getCode());
            // 创建时生成 token
            namespaceDO.setToken(UUID.randomUUID().toString());
            namespaceDO.setCreator(LoginUserHolder.getUserId());

        } else {
            namespaceDO = fetchById(id);
            namespaceDO.setModifier(LoginUserHolder.getUserId());

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

        return savedNamespace;
    }

    @Override
    public void delete(Long id) {
        List<AppInfoDO> appInfosInNamespace = appInfoRepository.findAllByNamespaceId(id);
        if (CollectionUtils.isNotEmpty(appInfosInNamespace)) {
            List<String> relatedApps = appInfosInNamespace.stream().map(AppInfoDO::getAppName).collect(Collectors.toList());
            throw new PowerJobException("Unable to delete due to associated apps: " + SJ.COMMA_JOINER.join(relatedApps));
        }

        namespaceRepository.deleteById(id);
    }

    @Override
    public Optional<NamespaceDO> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return namespaceRepository.findById(id);
    }

    @Override
    public Optional<NamespaceDO> findByCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return Optional.empty();
        }
        return namespaceRepository.findByCode(code);
    }

    @Override
    public Page<NamespaceDO> list(QueryNamespaceRequest queryNamespaceRequest) {
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

        return namespaceRepository.findAll(specification, pageable);
    }

    @Override
    public List<NamespaceDO> listAll() {
        return namespaceRepository.findAll();
    }

    private NamespaceDO fetchById(Long id) {
        Optional<NamespaceDO> namespaceDoOpt = namespaceRepository.findById(id);
        if (!namespaceDoOpt.isPresent()) {
            throw new IllegalArgumentException("can't find namespace by id: " + id);
        }
        return namespaceDoOpt.get();
    }
}
