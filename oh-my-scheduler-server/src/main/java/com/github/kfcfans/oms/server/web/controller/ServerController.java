package com.github.kfcfans.oms.server.web.controller;

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

    @GetMapping("/acquire")
    public ResultDTO<String> acquireServer(String appName) {
        String server = serverSelectService.getServer(appName);
        return ResultDTO.success(server);
    }

}
