package tech.powerjob.server.web.controller;

import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.common.utils.RSAUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private AppInfoService appInfoService;
    @Resource
    private AppInfoRepository appInfoRepository;

    private static final int MAX_APP_NUM = 200;

    // 解密密码密钥
    private final static String PRIVATE_KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIaAjV3H+yKtYJKQ/YKBzzfsclTryhj/sxM3zEw3vXKc5/z3GJTEAkzkdg2B7ZdkE0qPSpDP1VMi1qx47NN2b0GxWimMRmMQmz3cqu2/eF+Ru9+PRWQSxfzTzDgloCWa8Y+VWLaWG55Et1xaVOmPD8nhxhf6qV4347ir/PV+fJl5AgMBAAECgYAoH+uMYZ9i3fQkZUqrh0wpM8l72gelY4qpngi9aBeFPJfcmF5l6v+Artsk9nDJrBoxMQQepVHPhmIie8Sy5O8VZ+3yDqqq3ITOg5AZOZUFH0NgxEq4Mk1PUJCDEW8ZKUC7OAlm+EaV1yZNeMKJTO2U6+F8QlVY/aOyKTtAiobPDQJBAPUAT7gX19fmi7xrQXke9+PPqHBGHJB+8shHV5b6g8YPn1gWsvxztT+l5mv3geoh95iXNXq/vItWyDcB9qvpYhcCQQCMilMs9mPYdNzBS+EZmp3l087K8NE0Zq3kL+Ob20vnh5+LVDpI6UlvIJ2Gw+4Ff5yOKCO5GV4swfI9GmXPN+rvAkBq6drf9A+t2J6L96YXq+rzD/BqJj5a0/swaOmRKfsNGE4py6YJwpiKkOPvo4+e03nPrdSZn0gw6gru7j1toae7AkBkFd5GFvHkJNVRkwgrg8EO+1g5jmZuOvF7n98oD3Ru3lbwknsvkXOKgF+uqbnIkKidVFicaUR3+2bXvQTkHZ5hAkEAnirWQToc3RTaMTJCG00tJg2AN9IUQ30nvCAJXFR+Dj6FrQUdkId/yjVThyBQcd5GYnJgP+iLPomUJaTzzmEGUQ==";

    // 密码格式
    private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9]).{8,16}$";

    @PostMapping("/save")
    public ResultDTO<Void> saveAppInfo(@RequestBody ModifyAppInfoRequest req) {

        req.valid();
        AppInfoDO appInfoDO;
        String password = req.getPassword();

        Long id = req.getId();
        if (id == null) {
            appInfoDO = new AppInfoDO();
            appInfoDO.setGmtCreate(new Date());
        }else {
            appInfoDO = appInfoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("can't find appInfo by id:" + id));
            String oldPassword = req.getOldPassword();
            try {
                oldPassword = StringUtils.isNotBlank(oldPassword) ? RSAUtils.decryptStr(oldPassword, PRIVATE_KEY) : "";
            } catch (Exception e) {
                e.printStackTrace();
                throw new PowerJobException("oldPassword解析错误");
            }
            // 对比密码
            if (!Objects.equals(oldPassword, appInfoDO.getPassword())) {
                throw new PowerJobException("The password is incorrect.");
            }
        }

        try {
            password = StringUtils.isNotBlank(password) ? RSAUtils.decryptStr(password, PRIVATE_KEY) : "";
        } catch (Exception e) {
            e.printStackTrace();
            throw new PowerJobException("password解析错误");
        }

        Pattern pattern = Pattern.compile(PASSWORD_REGEX);
        Matcher matcher = pattern.matcher(password);
        if (!matcher.find()) {
            throw new PowerJobException("密码格式不符合要求！");
        }

        BeanUtils.copyProperties(req, appInfoDO);
        appInfoDO.setPassword(password);
        appInfoDO.setGmtModified(new Date());

        appInfoRepository.saveAndFlush(appInfoDO);
        return ResultDTO.success(null);
    }

    @PostMapping("/assert")
    public ResultDTO<Long> assertApp(@RequestBody AppAssertRequest request) {
        String password = request.getPassword();
        try {
            password = StringUtils.isNotBlank(password) ? RSAUtils.decryptStr(password, PRIVATE_KEY) : "";
        } catch (Exception e) {
            e.printStackTrace();
            throw new PowerJobException("The password is incorrect.");
        }
        return ResultDTO.success(appInfoService.assertApp(request.getAppName(), password));
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
