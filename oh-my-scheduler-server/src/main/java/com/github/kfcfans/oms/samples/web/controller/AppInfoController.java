package com.github.kfcfans.oms.samples.web.controller;

import com.github.kfcfans.oms.samples.persistence.core.model.AppInfoDO;
import com.github.kfcfans.oms.samples.persistence.core.repository.AppInfoRepository;
import com.github.kfcfans.oms.common.response.ResultDTO;
import com.github.kfcfans.oms.samples.web.request.ModifyAppInfoRequest;
import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
public class AppInfoController {

    @Resource
    private AppInfoRepository appInfoRepository;

    @PostMapping("/save")
    public ResultDTO<Void> saveAppInfo(@RequestBody ModifyAppInfoRequest appInfoRequest) {

        AppInfoDO appInfoDO = new AppInfoDO();
        BeanUtils.copyProperties(appInfoRequest, appInfoDO);
        Date now = new Date();
        if (appInfoRequest.getId() == null) {
            appInfoDO.setGmtCreate(now);
        }
        appInfoDO.setGmtModified(now);
        appInfoRepository.saveAndFlush(appInfoDO);
        return ResultDTO.success(null);
    }

    @GetMapping("/delete")
    public ResultDTO<Void> deleteAppInfo(Long appId) {
        appInfoRepository.deleteById(appId);
        return ResultDTO.success(null);
    }

    @GetMapping("/list")
    public ResultDTO<List<AppInfoVO>> listAppInfo(@RequestParam(required = false) String condition) {
        List<AppInfoDO> result;
        if (StringUtils.isEmpty(condition)) {
            result = appInfoRepository.findAll();
        }else {
            result = appInfoRepository.findByAppNameLike("%" + condition + "%");
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
