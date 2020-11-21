package com.github.kfcfans.powerjob.server.web.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.common.utils.CommonUtils;
import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.server.akka.OhMyServer;
import com.github.kfcfans.powerjob.server.persistence.core.model.AppInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.AppInfoRepository;
import com.github.kfcfans.powerjob.server.service.ha.ServerSelectService;
import com.github.kfcfans.powerjob.server.service.ha.WorkerManagerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.TimeZone;

/**
 * 处理Worker请求的 Controller
 * Worker启动时，先请求assert验证appName的可用性，再根据得到的appId获取Server地址
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

    @GetMapping("/assert")
    public ResultDTO<Long> assertAppName(String appName) {
        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findByAppName(appName);
        return appInfoOpt.map(appInfoDO -> ResultDTO.success(appInfoDO.getId())).
                orElseGet(() -> ResultDTO.failed(String.format("app(%s) is not registered! Please register the app in oms-console first.", appName)));
    }

    @GetMapping("/acquire")
    public ResultDTO<String> acquireServer(Long appId, String currentServer) {
        return ResultDTO.success(serverSelectService.getServer(appId, currentServer));
    }

    @GetMapping("/hello")
    public ResultDTO<JSONObject> ping(@RequestParam(required = false) boolean debug) {
        JSONObject res = new JSONObject();
        res.put("localHost", NetUtils.getLocalHost());
        res.put("actorSystemAddress", OhMyServer.getActorSystemAddress());
        res.put("serverTime", CommonUtils.formatTime(System.currentTimeMillis()));
        res.put("serverTimeZone", TimeZone.getDefault().getDisplayName());
        res.put("appIds", WorkerManagerService.getAppId2ClusterStatus().keySet());
        if (debug) {
            res.put("appId2ClusterInfo", JSON.parseObject(JSON.toJSONString(WorkerManagerService.getAppId2ClusterStatus())));

        }
        return ResultDTO.success(res);
    }

}
