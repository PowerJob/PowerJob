package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.oms.server.persistence.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.AppInfoRepository;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.oms.server.web.request.ModifyAppInfoRequest;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AppName Controller
 *
 * @author tjq
 * @since 2020/4/1
 */
@RestController
@RequestMapping("/appInfo")
public class AppInfoController {

    @Resource
    private AppInfoRepository appInfoRepository;

    @GetMapping("/save")
    public ResultDTO<Void> saveAppInfo(ModifyAppInfoRequest appInfoRequest) {

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
    public ResultDTO<List<AppInfoVO>> listAppInfo() {
        List<AppInfoVO> result = appInfoRepository.findAll().stream().map(appInfoDO -> {
            AppInfoVO appInfoVO = new AppInfoVO();
            BeanUtils.copyProperties(appInfoDO, appInfoVO);
            return appInfoVO;
        }).collect(Collectors.toList());
        return ResultDTO.success(result);
    }

    @Data
    private static class AppInfoVO {
        private Long id;
        private String appName;
    }

}
