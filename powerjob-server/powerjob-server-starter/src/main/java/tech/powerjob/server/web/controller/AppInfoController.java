package tech.powerjob.server.web.controller;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.auth.LoginUserHolder;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.auth.plugin.ModifyOrCreateDynamicPermission;
import tech.powerjob.server.auth.plugin.SaveAppGrantPermissionPlugin;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.persistence.PageResult;
import tech.powerjob.server.persistence.QueryConvertUtils;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;
import tech.powerjob.server.web.request.ModifyAppInfoRequest;
import tech.powerjob.server.web.request.QueryAppInfoRequest;
import tech.powerjob.server.web.response.AppInfoVO;

import javax.persistence.criteria.Predicate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AppName Controller
 * vue axios 的POST请求必须使用 @RequestBody 接收
 *
 * @author tjq
 * @since 2020/4/1
 */
@RestController
@RequestMapping("/appInfo")
@RequiredArgsConstructor
public class AppInfoController {

    private final WebAuthService webAuthService;

    private final AppInfoRepository appInfoRepository;

    @PostMapping("/save")
    @ApiPermission(name = "App-Save", roleScope = RoleScope.APP, dynamicPermissionPlugin = ModifyOrCreateDynamicPermission.class, grandPermissionPlugin = SaveAppGrantPermissionPlugin.class)
    public ResultDTO<AppInfoVO> saveAppInfo(@RequestBody ModifyAppInfoRequest req) {

        req.valid();
        AppInfoDO appInfoDO;

        Long id = req.getId();
        if (id == null) {
            appInfoDO = new AppInfoDO();
            appInfoDO.setGmtCreate(new Date());
            appInfoDO.setCreator(LoginUserHolder.getUserName());
        } else {
            appInfoDO = appInfoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("can't find appInfo by id:" + id));

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
        appInfoDO.setModifier(LoginUserHolder.getUserName());

        AppInfoDO savedAppInfo = appInfoRepository.saveAndFlush(appInfoDO);

        // 重现授权
        webAuthService.processPermissionOnSave(RoleScope.APP, savedAppInfo.getId(), req.getComponentUserRoleInfo());

        return ResultDTO.success(convert(Lists.newArrayList(savedAppInfo), false).get(0));
    }

    @GetMapping("/delete")
    @ApiPermission(name = "App-Delete", roleScope = RoleScope.APP, requiredPermission = Permission.SU)
    public ResultDTO<Void> deleteAppInfo(Long appId) {
        appInfoRepository.deleteById(appId);
        return ResultDTO.success(null);
    }

    @PostMapping("/list")
    @ApiPermission(name = "Namespace-List", roleScope = RoleScope.APP, requiredPermission = Permission.NONE)
    public ResultDTO<PageResult<AppInfoVO>> listAppInfoByQuery(@RequestBody QueryAppInfoRequest queryAppInfoRequest) {

        Pageable pageable = PageRequest.of(queryAppInfoRequest.getIndex(), queryAppInfoRequest.getPageSize());

        // TODO: 我有权限的列表
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

            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };

        Page<AppInfoDO> pageAppInfoResult = appInfoRepository.findAll(specification, pageable);

        PageResult<AppInfoVO> pageRet = new PageResult<>(pageAppInfoResult);

        List<AppInfoDO> appinfoDos = pageAppInfoResult.get().collect(Collectors.toList());
        pageRet.setData(convert(appinfoDos, true));

        return ResultDTO.success(pageRet);
    }

    private List<AppInfoVO> convert(List<AppInfoDO> data, boolean fillDetail) {
        if (CollectionUtils.isEmpty(data)) {
            return Lists.newLinkedList();
        }

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
                appInfoVO.setPassword(hasPermission ? appInfoDO.getPassword() : AuthConstants.TIPS_NO_PERMISSION_TO_SEE);
            }

            return appInfoVO;
        }).collect(Collectors.toList());
    }

}
