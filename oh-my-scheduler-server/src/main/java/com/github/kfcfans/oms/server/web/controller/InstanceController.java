package com.github.kfcfans.oms.server.web.controller;

import akka.actor.ActorSelection;
import com.github.kfcfans.common.request.ServerStopInstanceReq;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.akka.requests.RedirectServerStopInstanceReq;
import com.github.kfcfans.oms.server.persistence.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.model.ExecuteLogDO;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.persistence.repository.ExecuteLogRepository;
import com.github.kfcfans.oms.server.persistence.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.service.ha.ServerSelectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 任务实例 Controller
 *
 * @author tjq
 * @since 2020/4/9
 */
@RestController
@RequestMapping("/instance")
public class InstanceController {

    @Resource
    private ExecuteLogRepository executeLogRepository;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private AppInfoRepository appInfoRepository;

    @GetMapping("/stop")
    public ResultDTO<Void> stopInstance(Long instanceId) throws Exception {

        // 联级查询：instanceId -> jobId -> appId -> serverAddress
        ExecuteLogDO executeLogDO = executeLogRepository.findByInstanceId(instanceId);
        if (executeLogDO == null) {
            return ResultDTO.failed("invalid instanceId: " + instanceId);
        }

        JobInfoDO jobInfoDO = jobInfoRepository.findById(executeLogDO.getJobId()).orElseThrow(() -> {
            throw new RuntimeException("impossible");
        });

        AppInfoDO appInfoDO = appInfoRepository.findById(jobInfoDO.getAppId()).orElseThrow(() -> {
            throw new RuntimeException("impossible");
        });

        String serverAddress = appInfoDO.getCurrentServer();

        // 将请求转发给目标Server（HTTP -> AKKA）
        ActorSelection serverActor = OhMyServer.getServerActor(serverAddress);
        RedirectServerStopInstanceReq req = new RedirectServerStopInstanceReq();
        req.setServerStopInstanceReq(new ServerStopInstanceReq(instanceId));
        serverActor.tell(req, null);

        return ResultDTO.success(null);
    }
}
