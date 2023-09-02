package tech.powerjob.server.web.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.powerjob.common.model.WorkerAppInfo;
import tech.powerjob.common.request.ServerDiscoveryRequest;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.server.common.aware.ServerInfoAware;
import tech.powerjob.server.common.module.ServerInfo;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.remote.server.election.ServerElectionService;
import tech.powerjob.server.remote.transporter.TransportService;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;

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
@RequiredArgsConstructor
public class ServerController implements ServerInfoAware {

    private ServerInfo serverInfo;
    private final TransportService transportService;

    private final ServerElectionService serverElectionService;

    private final AppInfoRepository appInfoRepository;

    private final WorkerClusterQueryService workerClusterQueryService;

    @GetMapping("/assert")
    public ResultDTO<Long> assertAppName(String appName) {
        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findByAppName(appName);
        return appInfoOpt.map(appInfoDO -> ResultDTO.success(appInfoDO.getId())).
                orElseGet(() -> ResultDTO.failed(String.format("app(%s) is not registered! Please register the app in oms-console first.", appName)));
    }

    @GetMapping("/assertV2")
    public ResultDTO<WorkerAppInfo> assertAppNameV2(String appName) {
        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findByAppName(appName);
        return appInfoOpt.map(appInfoDO -> {
                    WorkerAppInfo workerAppInfo = new WorkerAppInfo().setAppId(appInfoDO.getId());
                    return ResultDTO.success(workerAppInfo);
                }).
                orElseGet(() -> ResultDTO.failed(String.format("app(%s) is not registered! Please register the app in oms-console first.", appName)));
    }

    @GetMapping("/acquire")
    public ResultDTO<String> acquireServer(ServerDiscoveryRequest request) {
        return ResultDTO.success(serverElectionService.elect(request));
    }

    @GetMapping("/hello")
    public ResultDTO<JSONObject> ping(@RequestParam(required = false) boolean debug) {
        JSONObject res = new JSONObject();
        res.put("localHost", NetUtils.getLocalHost());
        res.put("serverInfo", serverInfo);
        res.put("serverTime", CommonUtils.formatTime(System.currentTimeMillis()));
        res.put("serverTimeTs", System.currentTimeMillis());
        res.put("serverTimeZone", TimeZone.getDefault().getDisplayName());
        res.put("appIds", workerClusterQueryService.getAppId2ClusterStatus().keySet());
        if (debug) {
            res.put("appId2ClusterInfo", JSON.parseObject(JSON.toJSONString(workerClusterQueryService.getAppId2ClusterStatus())));
        }

        try {
            res.put("defaultAddress", JSONObject.toJSON(transportService.defaultProtocol()));
        } catch (Exception ignore) {
        }

        return ResultDTO.success(res);
    }

    @Override
    public void setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }
}
