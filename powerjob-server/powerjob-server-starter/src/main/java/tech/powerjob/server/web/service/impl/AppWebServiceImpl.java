package tech.powerjob.server.web.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.exception.PowerJobExceptionLauncher;
import tech.powerjob.server.auth.LoginUserHolder;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.common.module.WorkerInfo;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.persistence.QueryConvertUtils;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;
import tech.powerjob.server.web.request.ModifyAppInfoRequest;
import tech.powerjob.server.web.request.QueryAppInfoRequest;
import tech.powerjob.server.web.service.AppWebService;
import tech.powerjob.server.web.service.NamespaceWebService;

import javax.persistence.criteria.Predicate;
import java.util.*;

/**
 * AppWebService
 *
 * @author tjq
 * @since 2024/12/8
 */
@Slf4j
@Service
@AllArgsConstructor
public class AppWebServiceImpl implements AppWebService {

    private final WebAuthService webAuthService;

    private final AppInfoService appInfoService;
    private final AppInfoRepository appInfoRepository;

    private final NamespaceWebService namespaceWebService;

    private final WorkerClusterQueryService workerClusterQueryService;

    @Override
    public AppInfoDO save(ModifyAppInfoRequest req) {
        // 根据 ns code 填充 namespaceId（自动化创建过程中，固定的 namespace-code 对用户更友好）
        if (StringUtils.isNotEmpty(req.getNamespaceCode())) {
            namespaceWebService.findByCode(req.getNamespaceCode()).ifPresent(x -> req.setNamespaceId(x.getId()));
        }

        req.valid();
        AppInfoDO appInfoDO;

        Long id = req.getId();
        if (id == null) {

            // 前置校验，防止部分没加唯一索引的 DB 重复创建记录导致异常
            appInfoRepository.findByAppName(req.getAppName()).ifPresent(x -> new PowerJobExceptionLauncher(ErrorCodes.ILLEGAL_ARGS_ERROR, String.format("App[%s] already exists", req.getAppName())));

            appInfoDO = new AppInfoDO();
            appInfoDO.setGmtCreate(new Date());
            appInfoDO.setCreator(LoginUserHolder.getUserId());

        } else {
            appInfoDO = appInfoService.findById(id, false).orElseThrow(() -> new IllegalArgumentException("can't find appInfo by id:" + id));

            // 不允许修改 appName
            if (!appInfoDO.getAppName().equalsIgnoreCase(req.getAppName())) {
                throw new IllegalArgumentException("NOT_ALLOW_CHANGE_THE_APP_NAME");
            }
        }

        appInfoDO.setAppName(req.getAppName());
        appInfoDO.setTitle(req.getTitle());
        appInfoDO.setPassword(req.getPassword());
        appInfoDO.setNamespaceId(req.getNamespaceId());
        appInfoDO.setTags(req.getTags());
        appInfoDO.setExtra(req.getExtra());

        appInfoDO.setGmtModified(new Date());
        appInfoDO.setModifier(LoginUserHolder.getUserId());

        AppInfoDO savedAppInfo = appInfoService.save(appInfoDO);

        // 重现授权
        webAuthService.processPermissionOnSave(RoleScope.APP, savedAppInfo.getId(), req.getComponentUserRoleInfo());
        return savedAppInfo;
    }

    @Override
    public void delete(Long appId) {
        log.warn("[AppInfoController] try to delete app: {}", appId);

        List<WorkerInfo> allAliveWorkers = workerClusterQueryService.getAllAliveWorkers(appId);
        if (CollectionUtils.isNotEmpty(allAliveWorkers)) {
            throw new PowerJobException(ErrorCodes.OPERATION_NOT_PERMITTED, "Unable to delete apps with live workers, Please remove the worker dependency first!");
        }

        appInfoService.deleteById(appId);
        log.warn("[AppInfoController] delete app[id={}] successfully!", appId);
    }

    @Override
    public Optional<AppInfoDO> findByAppName(String appName) {
        return appInfoRepository.findByAppName(appName);
    }

    @Override
    public Page<AppInfoDO> list(QueryAppInfoRequest queryAppInfoRequest) {
        Pageable pageable = PageRequest.of(queryAppInfoRequest.getIndex(), queryAppInfoRequest.getPageSize());

        // 相关权限（先查处关联 ids）
        Set<Long> queryAppIds;
        Boolean showMyRelated = queryAppInfoRequest.getShowMyRelated();
        if (BooleanUtils.isTrue(showMyRelated)) {
            Set<Long> targetIds = Sets.newHashSet();
            webAuthService.fetchMyPermissionTargets(RoleScope.APP).values().forEach(targetIds::addAll);
            queryAppIds = targetIds;

            if (CollectionUtils.isEmpty(queryAppIds)) {
                return Page.empty();
            }

        } else {
            queryAppIds = Collections.emptySet();
        }

        Specification<AppInfoDO> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = Lists.newArrayList();

            Long appId = queryAppInfoRequest.getAppId();
            Long namespaceId = queryAppInfoRequest.getNamespaceId();

            if (appId != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), appId));
            }

            if (namespaceId != null) {
                predicates.add(criteriaBuilder.equal(root.get("namespaceId"), namespaceId));
            }

            if (StringUtils.isNotEmpty(queryAppInfoRequest.getAppNameLike())) {
                predicates.add(criteriaBuilder.like(root.get("appName"), QueryConvertUtils.convertLikeParams(queryAppInfoRequest.getAppNameLike())));
            }

            if (StringUtils.isNotEmpty(queryAppInfoRequest.getTagLike())) {
                predicates.add(criteriaBuilder.like(root.get("tags"), QueryConvertUtils.convertLikeParams(queryAppInfoRequest.getTagLike())));
            }

            if (!queryAppIds.isEmpty()) {
                predicates.add(criteriaBuilder.in(root.get("id")).value(queryAppIds));
            }

            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };

        return appInfoRepository.findAll(specification, pageable);
    }
}
