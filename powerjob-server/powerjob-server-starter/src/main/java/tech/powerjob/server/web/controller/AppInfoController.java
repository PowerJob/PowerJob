package tech.powerjob.server.web.controller;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.persistence.PageResult;
import tech.powerjob.server.persistence.QueryConvertUtils;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.web.request.AppAssertRequest;
import tech.powerjob.server.web.request.ModifyAppInfoRequest;
import tech.powerjob.server.web.request.QueryAppInfoRequest;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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

    private final AppInfoService appInfoService;

    private final AppInfoRepository appInfoRepository;

    private static final int MAX_APP_NUM = 200;

    @PostMapping("/save")
    public ResultDTO<AppInfoVO> saveAppInfo(@RequestBody ModifyAppInfoRequest req) {

        req.valid();
        AppInfoDO appInfoDO;

        Long id = req.getId();
        if (id == null) {
            appInfoDO = new AppInfoDO();
            appInfoDO.setGmtCreate(new Date());
        }else {
            appInfoDO = appInfoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("can't find appInfo by id:" + id));

            // 对比密码
            if (!Objects.equals(req.getOldPassword(), appInfoDO.getPassword())) {
                throw new PowerJobException("The password is incorrect.");
            }
        }
        BeanUtils.copyProperties(req, appInfoDO);
        appInfoDO.setGmtModified(new Date());

        AppInfoDO savedAppInfo = appInfoRepository.saveAndFlush(appInfoDO);
        return ResultDTO.success(convert(Lists.newArrayList(savedAppInfo), false).get(0));
    }

    @PostMapping("/assert")
    public ResultDTO<Long> assertApp(@RequestBody AppAssertRequest request) {
        return ResultDTO.success(appInfoService.assertApp(request.getAppName(), request.getPassword()));
    }

    @GetMapping("/delete")
    public ResultDTO<Void> deleteAppInfo(Long appId) {
        appInfoRepository.deleteById(appId);
        return ResultDTO.success(null);
    }

    @GetMapping("/list")
    public ResultDTO<List<AppInfoVO>> listAppInfo(@RequestParam(required = false) String condition) {
        List<AppInfoDO> result;
        Pageable limit = PageRequest.of(0, MAX_APP_NUM);
        if (StringUtils.isEmpty(condition)) {
            result = appInfoRepository.findAll(limit).getContent();
        }else {
            result = appInfoRepository.findByAppNameLike("%" + condition + "%", limit).getContent();
        }
        return ResultDTO.success(convert(result, false));
    }

    @PostMapping("/listByQuery")
    public ResultDTO<PageResult<AppInfoVO>> listAppInfoByQuery(QueryAppInfoRequest queryAppInfoRequest) {

        Pageable pageable = PageRequest.of(queryAppInfoRequest.getIndex(), queryAppInfoRequest.getPageSize());

        // TODO: 我有权限的列表
        Specification<AppInfoDO> specification = new Specification<AppInfoDO>() {
            @Override
            public Predicate toPredicate(Root<AppInfoDO> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = Lists.newArrayList();


                Long appId = queryAppInfoRequest.getAppId();
                Long namespaceId = queryAppInfoRequest.getNamespaceId();

                if (appId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("id"), appId));
                }

                if (namespaceId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("namespaceId"), namespaceId));
                }

                if (StringUtils.isNotEmpty(queryAppInfoRequest.getAppName())) {
                    predicates.add(criteriaBuilder.like(root.get("appName"), QueryConvertUtils.convertLikeParams(queryAppInfoRequest.getAppName())));
                }

                return query.where(predicates.toArray(new Predicate[0])).getRestriction();
            }
        };

        Page<AppInfoDO> pageAppInfoResult = appInfoRepository.findAll(specification, pageable);

        PageResult<AppInfoVO> pageRet = new PageResult<>(pageAppInfoResult);

        List<AppInfoDO> appinfoDos = pageAppInfoResult.get().collect(Collectors.toList());
        pageRet.setData(convert(appinfoDos, true));

        return ResultDTO.success(pageRet);
    }


    private static List<AppInfoVO> convert(List<AppInfoDO> data, boolean fillDetail) {
        if (CollectionUtils.isEmpty(data)) {
            return Lists.newLinkedList();
        }
        List<AppInfoVO> appInfoVOList = data.stream().map(appInfoDO -> {
            AppInfoVO appInfoVO = new AppInfoVO();
            BeanUtils.copyProperties(appInfoDO, appInfoVO);
            return appInfoVO;
        }).collect(Collectors.toList());

        if (fillDetail) {
            // TODO: 补全权限等额外信息
        }

        return appInfoVOList;
    }

    @Data
    private static class AppInfoVO {
        private Long id;
        private String appName;
    }

}
