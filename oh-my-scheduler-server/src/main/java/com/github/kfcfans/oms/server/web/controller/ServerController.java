package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.oms.server.persistence.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.service.ha.ServerSelectService;
import com.github.kfcfans.oms.server.web.ResultDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 处理内部请求的 Controller
 *
 * @author tjq
 * @since 2020/4/4
 */
@RestController
@RequestMapping("/server")
public class ServerController {

    @Resource
    private ServerSelectService serverSelectService;
    @Resource
    private AppInfoRepository appInfoRepository;

    @GetMapping("assert")
    public ResultDTO<Long> assertAppName(String appName) {
        AppInfoDO appInfo = appInfoRepository.findByAppName(appName);
        if (appInfo == null) {
            return ResultDTO.failed(appName + " is not registered!");
        }
        return ResultDTO.success(appInfo.getId());
    }

    @GetMapping("/acquire")
    public ResultDTO<String> acquireServer(Long appId) {
        String server = serverSelectService.getServer(appId);
        return ResultDTO.success(server);
    }

}
