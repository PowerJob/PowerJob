package com.github.kfcfans.oms.server.service.instance;

import akka.actor.ActorSelection;
import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.SystemInstanceResult;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.common.request.ServerStopInstanceReq;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.persistence.model.ExecuteLogDO;
import com.github.kfcfans.oms.server.persistence.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.persistence.repository.ExecuteLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

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
    private AppInfoRepository appInfoRepository;
    @Resource
    private ExecuteLogRepository executeLogRepository;

    /**
     * 停止任务实例
     * @param instanceId 任务实例ID
     */
    public void stopInstance(Long instanceId) {

        ExecuteLogDO executeLogDO = executeLogRepository.findByInstanceId(instanceId);
        if (executeLogDO == null) {
            log.warn("[InstanceService] can't find execute log for instanceId: {}.", instanceId);
            throw new IllegalArgumentException("invalid instanceId: " + instanceId);
        }
        // 更新数据库，将状态置为停止
        executeLogDO.setStatus(STOPPED.getV());
        executeLogDO.setGmtModified(new Date());
        executeLogDO.setFinishedTime(System.currentTimeMillis());
        executeLogDO.setResult(SystemInstanceResult.STOPPED_BY_USER);
        executeLogRepository.saveAndFlush(executeLogDO);

        // 停止 TaskTracker
        ActorSelection taskTrackerActor = OhMyServer.getTaskTrackerActor(executeLogDO.getTaskTrackerAddress());
        ServerStopInstanceReq req = new ServerStopInstanceReq(instanceId);
        taskTrackerActor.tell(req, null);
    }

    public InstanceDetail getInstanceDetail(Long instanceId) {

        ExecuteLogDO executeLogDO = executeLogRepository.findByInstanceId(instanceId);
        if (executeLogDO == null) {
            log.warn("[InstanceService] can't find execute log for instanceId: {}.", instanceId);
            throw new IllegalArgumentException("invalid instanceId: " + instanceId);
        }

        InstanceStatus instanceStatus = InstanceStatus.of(executeLogDO.getStatus());

        InstanceDetail detail = new InstanceDetail();
        detail.setStatus(instanceStatus.getDes());

        // 只要不是运行状态，只需要返回简要信息
        if (instanceStatus != RUNNING) {
            BeanUtils.copyProperties(executeLogDO, detail);
            return detail;
        }

        // 运行状态下，需要分别考虑MapReduce、Broadcast和秒级任务的详细信息



        return null;
    }

}
