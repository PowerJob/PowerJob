package com.github.kfcfans.oms.server.service.instance;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.common.SystemInstanceResult;
import com.github.kfcfans.common.model.InstanceDetail;
import com.github.kfcfans.common.request.ServerQueryInstanceStatusReq;
import com.github.kfcfans.common.request.ServerStopInstanceReq;
import com.github.kfcfans.common.response.AskResponse;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.persistence.model.InstanceLogDO;
import com.github.kfcfans.oms.server.persistence.repository.InstanceLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static com.github.kfcfans.common.InstanceStatus.RUNNING;
import static com.github.kfcfans.common.InstanceStatus.STOPPED;

/**
 * 任务运行实例服务
 *
 * @author tjq
 * @since 2020/4/11
 */
@Slf4j
@Service
public class InstanceService {

    @Resource
    private InstanceLogRepository instanceLogRepository;

    /**
     * 停止任务实例
     * @param instanceId 任务实例ID
     */
    public void stopInstance(Long instanceId) {

        InstanceLogDO instanceLogDO = instanceLogRepository.findByInstanceId(instanceId);
        if (instanceLogDO == null) {
            log.warn("[InstanceService] can't find execute log for instanceId: {}.", instanceId);
            throw new IllegalArgumentException("invalid instanceId: " + instanceId);
        }

        // 判断状态，只有运行中才能停止
        if (!InstanceStatus.generalizedRunningStatus.contains(instanceLogDO.getStatus())) {
            throw new IllegalArgumentException("can't stop finished instance!");
        }

        // 更新数据库，将状态置为停止
        instanceLogDO.setStatus(STOPPED.getV());
        instanceLogDO.setGmtModified(new Date());
        instanceLogDO.setFinishedTime(System.currentTimeMillis());
        instanceLogDO.setResult(SystemInstanceResult.STOPPED_BY_USER);
        instanceLogRepository.saveAndFlush(instanceLogDO);

        // 停止 TaskTracker
        ActorSelection taskTrackerActor = OhMyServer.getTaskTrackerActor(instanceLogDO.getTaskTrackerAddress());
        ServerStopInstanceReq req = new ServerStopInstanceReq(instanceId);
        taskTrackerActor.tell(req, null);
    }

    /**
     * 获取任务实例的详细运行详细
     * @param instanceId 任务实例ID
     * @return 详细运行状态
     */
    public InstanceDetail getInstanceDetail(Long instanceId) {

        InstanceLogDO instanceLogDO = instanceLogRepository.findByInstanceId(instanceId);
        if (instanceLogDO == null) {
            log.warn("[InstanceService] can't find execute log for instanceId: {}.", instanceId);
            throw new IllegalArgumentException("invalid instanceId: " + instanceId);
        }

        InstanceStatus instanceStatus = InstanceStatus.of(instanceLogDO.getStatus());

        InstanceDetail detail = new InstanceDetail();
        detail.setStatus(instanceStatus.getDes());

        // 只要不是运行状态，只需要返回简要信息
        if (instanceStatus != RUNNING) {
            BeanUtils.copyProperties(instanceLogDO, detail);
            return detail;
        }

        // 运行状态下，交由 TaskTracker 返回相关信息
        try {
            ServerQueryInstanceStatusReq req = new ServerQueryInstanceStatusReq(instanceId);
            ActorSelection taskTrackerActor = OhMyServer.getTaskTrackerActor(instanceLogDO.getTaskTrackerAddress());
            CompletionStage<Object> askCS = Patterns.ask(taskTrackerActor, req, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
            AskResponse askResponse = (AskResponse) askCS.toCompletableFuture().get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (askResponse.isSuccess()) {
                return (InstanceDetail) askResponse.getExtra();
            }else {
                log.warn("[InstanceService] ask InstanceStatus from TaskTracker failed, the message is {}.", askResponse.getExtra());
            }

        }catch (Exception e) {
            log.error("[InstanceService] ask InstanceStatus from TaskTracker failed.", e);
        }

        // 失败则返回基础版信息
        BeanUtils.copyProperties(instanceLogDO, detail);
        return detail;
    }

}
