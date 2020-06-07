package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.oms.common.response.ResultDTO;
import com.github.kfcfans.oms.server.persistence.core.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.web.request.ModifyAppInfoRequest;
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

    private static final int MAX_APP_NUM = 50;

    @PostMapping("/save")
    public ResultDTO<Void> saveAppInfo(@RequestBody ModifyAppInfoRequest req) {

        AppInfoDO appInfoDO;

        Long id = req.getId();
        if (id == null) {
            appInfoDO = new AppInfoDO();
            appInfoDO.setGmtCreate(new Date());
        }else {
            appInfoDO = appInfoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("can't find appInfo by id:" + id));
        }
        BeanUtils.copyProperties(req, appInfoDO);
        appInfoDO.setGmtModified(new Date());

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
