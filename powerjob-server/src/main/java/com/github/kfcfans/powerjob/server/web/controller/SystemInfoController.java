package com.github.kfcfans.powerjob.server.web.controller;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.OmsConstant;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.server.akka.OhMyServer;
import com.github.kfcfans.powerjob.server.akka.requests.FriendQueryWorkerClusterStatusReq;
import com.github.kfcfans.powerjob.server.common.constans.SwitchableStatus;
import com.github.kfcfans.powerjob.server.persistence.core.model.AppInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.AppInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.powerjob.server.web.response.SystemOverviewVO;
import com.github.kfcfans.powerjob.server.web.response.WorkerStatusVO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
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
@Slf4j
@RestController
@RequestMapping("/system")
public class SystemInfoController {

    @Resource
    private AppInfoRepository appInfoRepository;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    @GetMapping("/listWorker")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ResultDTO<List<WorkerStatusVO>> listWorker(Long appId) {
        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(appId);
        if (!appInfoOpt.isPresent()) {
            return ResultDTO.failed("unknown appId of " +appId);
        }
        String server =appInfoOpt.get().getCurrentServer();

        // 没有 Server，说明从来没有该 appId 的 worker 集群连接过
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
                Map address2Info = askResponse.getData(Map.class);
                List<WorkerStatusVO> result = Lists.newLinkedList();
                address2Info.forEach((address, m) -> {
                    try {
                        SystemMetrics metrics = JsonUtils.parseObject(JsonUtils.toJSONString(m), SystemMetrics.class);
                        WorkerStatusVO info = new WorkerStatusVO(String.valueOf(address), metrics);
                        result.add(info);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                return ResultDTO.success(result);
            }
            return ResultDTO.failed(askResponse.getMessage());
        }catch (Exception e) {
            log.error("[SystemInfoController] listWorker for appId:{} failed, exception is {}", appId, e.toString());
            return ResultDTO.failed("no worker or server available");
        }
    }

    @GetMapping("/overview")
    public ResultDTO<SystemOverviewVO> getSystemOverview(Long appId) {

        SystemOverviewVO overview = new SystemOverviewVO();

        // 总任务数量
        overview.setJobCount(jobInfoRepository.countByAppIdAndStatusNot(appId, SwitchableStatus.DELETED.getV()));
        // 运行任务数
        overview.setRunningInstanceCount(instanceInfoRepository.countByAppIdAndStatus(appId, InstanceStatus.RUNNING.getV()));
        // 近期失败任务数（24H内）
        Date date = DateUtils.addDays(new Date(), -1);
        overview.setFailedInstanceCount(instanceInfoRepository.countByAppIdAndStatusAndGmtCreateAfter(appId, InstanceStatus.FAILED.getV(), date));

        // 服务器时区
        overview.setTimezone(TimeZone.getDefault().getDisplayName());
        // 服务器时间
        overview.setServerTime(DateFormatUtils.format(new Date(), OmsConstant.TIME_PATTERN));

        return ResultDTO.success(overview);
    }

}
