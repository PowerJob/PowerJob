package com.github.kfcfans.oms.server.web.controller;

import akka.actor.ActorSelection;
import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.request.ServerStopInstanceReq;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.akka.requests.RedirectServerStopInstanceReq;
import com.github.kfcfans.oms.server.persistence.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.model.ExecuteLogDO;
import com.github.kfcfans.oms.server.persistence.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.persistence.repository.ExecuteLogRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import java.util.Date;

import static com.github.kfcfans.common.InstanceStatus.*;

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
    private AppInfoRepository appInfoRepository;

    @GetMapping("/stop")
    public ResultDTO<Void> stopInstance(Long instanceId) {

        ExecuteLogDO executeLogDO = executeLogRepository.findByInstanceId(instanceId);
        if (executeLogDO == null) {
            return ResultDTO.failed("invalid instanceId: " + instanceId);
        }
        // 更新数据库，将状态置为停止
        executeLogDO.setStatus(STOPPED.getV());
        executeLogDO.setGmtModified(new Date());
        executeLogDO.setFinishedTime(System.currentTimeMillis());
        executeLogDO.setResult("STOPPED_BY_USER");
        executeLogRepository.saveAndFlush(executeLogDO);

        // 获取Server地址，准备转发请求
        AppInfoDO appInfoDO = appInfoRepository.findById(executeLogDO.getAppId()).orElse(new AppInfoDO());
        if (StringUtils.isEmpty(appInfoDO.getCurrentServer())) {
            return ResultDTO.failed("can't find server");
        }

        // 将请求转发给目标Server（HTTP -> AKKA）
        ActorSelection serverActor = OhMyServer.getServerActor(appInfoDO.getCurrentServer());
        RedirectServerStopInstanceReq req = new RedirectServerStopInstanceReq();
        req.setServerStopInstanceReq(new ServerStopInstanceReq(instanceId));
        serverActor.tell(req, null);
        return ResultDTO.success(null);
    }

    @GetMapping("/status")
    public ResultDTO<Void> getRunningStatus(Long instanceId) {

        ExecuteLogDO executeLogDO = executeLogRepository.findByInstanceId(instanceId);
        if (executeLogDO == null) {
            return ResultDTO.failed("invalid instanceId: " + instanceId);
        }

        InstanceStatus status = InstanceStatus.of(executeLogDO.getStatus());
        if (status == FAILED || status == SUCCEED || status == STOPPED) {

        }

        return null;
    }
}
