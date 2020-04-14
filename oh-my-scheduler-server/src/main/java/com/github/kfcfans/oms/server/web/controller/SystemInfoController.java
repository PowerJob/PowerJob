package com.github.kfcfans.oms.server.web.controller;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.common.model.SystemMetrics;
import com.github.kfcfans.common.response.AskResponse;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.akka.requests.FriendQueryWorkerClusterStatusReq;
import com.github.kfcfans.oms.server.persistence.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.persistence.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.web.response.SystemOverviewVO;
import com.github.kfcfans.oms.server.web.response.WorkerStatusVO;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * 系统信息控制器（服务于前端首页）
 *
 * @author tjq
 * @since 2020/4/14
 */
@RestController
@RequestMapping("/system")
public class SystemInfoController {

    @Resource
    private AppInfoRepository appInfoRepository;
    @Resource
    private JobInfoRepository jobInfoRepository;

    @GetMapping("/listWorker")
    @SuppressWarnings("unchecked")
    public ResultDTO<List<WorkerStatusVO>> listWorker(Long appId) {
        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(appId);
        if (!appInfoOpt.isPresent()) {
            return ResultDTO.failed("unknown appId of " +appId);
        }
        String server =appInfoOpt.get().getCurrentServer();

        // 没有Server
        if (StringUtils.isEmpty(server)) {
            return ResultDTO.success(Collections.emptyList());
        }

        // 重定向到指定 Server 获取集群信息
        FriendQueryWorkerClusterStatusReq req = new FriendQueryWorkerClusterStatusReq(appId);
        try {
            ActorSelection friendActor = OhMyServer.getFriendActor(server);
            CompletionStage<Object> askCS = Patterns.ask(friendActor, req, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
            AskResponse askResponse = (AskResponse) askCS.toCompletableFuture().get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (askResponse.isSuccess()) {
                Map<String, SystemMetrics> address2Info = (Map<String, SystemMetrics>) askResponse.getExtra();
                List<WorkerStatusVO> result = Lists.newLinkedList();
                address2Info.forEach((address, metrics) -> {
                    WorkerStatusVO info = new WorkerStatusVO(address, metrics);
                    result.add(info);
                });
                return ResultDTO.success(result);
            }
            return ResultDTO.failed(String.valueOf(askResponse.getExtra()));
        }catch (Exception e) {
            return ResultDTO.failed("no worker or server available");
        }
    }

    @GetMapping("/overview")
    public ResultDTO<SystemOverviewVO> getSystemOverview(Long appId) {

        SystemOverviewVO overview = new SystemOverviewVO();

        // 总任务数量
        overview.setJobCount(jobInfoRepository.countByAppId(appId));
        // 运行任务数
        overview.setRunningInstanceCount(jobInfoRepository.countByAppIdAndStatus(appId, InstanceStatus.RUNNING.getV()));
        // 近期失败任务数（24H内）
        Date date = DateUtils.addDays(new Date(), -1);
        overview.setFailedInstanceCount(jobInfoRepository.countByAppIdAndStatusAndGmtCreateAfter(appId, InstanceStatus.FAILED.getV(), date));

        return ResultDTO.success(overview);
    }

}
