package tech.powerjob.server.web.controller;

import lombok.RequiredArgsConstructor;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.web.request.AppAssertRequest;
import tech.powerjob.server.web.request.ModifyAppInfoRequest;
import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
    public ResultDTO<Void> saveAppInfo(@RequestBody ModifyAppInfoRequest req) {

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

        appInfoRepository.saveAndFlush(appInfoDO);
        return ResultDTO.success(null);
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
        return ResultDTO.success(convert(result));
    }

    private static List<AppInfoVO> convert(List<AppInfoDO> data) {
        if (CollectionUtils.isEmpty(data)) {
            return Lists.newLinkedList();
        }
        return data.stream().map(appInfoDO -> {
            AppInfoVO appInfoVO = new AppInfoVO();
            BeanUtils.copyProperties(appInfoDO, appInfoVO);
            return appInfoVO;
        }).collect(Collectors.toList());
    }

    @Data
    private static class AppInfoVO {
        private Long id;
        private String appName;
    }

}
