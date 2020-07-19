package com.github.kfcfans.powerjob.server.service.instance;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.common.SystemInstanceResult;
import com.github.kfcfans.powerjob.common.model.InstanceDetail;
import com.github.kfcfans.powerjob.common.request.ServerQueryInstanceStatusReq;
import com.github.kfcfans.powerjob.common.request.ServerStopInstanceReq;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.github.kfcfans.powerjob.common.response.InstanceInfoDTO;
import com.github.kfcfans.powerjob.server.akka.OhMyServer;
import com.github.kfcfans.powerjob.server.common.constans.InstanceType;
import com.github.kfcfans.powerjob.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.powerjob.server.service.id.IdGenerateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static com.github.kfcfans.powerjob.common.InstanceStatus.RUNNING;
import static com.github.kfcfans.powerjob.common.InstanceStatus.STOPPED;

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
    private IdGenerateService idGenerateService;
    @Resource
    private InstanceManager instanceManager;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    /**
     * 创建任务实例（注意，该方法并不调用 saveAndFlush，如果有需要立即同步到DB的需求，请在方法结束后手动调用 flush）
     * @param jobId 任务ID
     * @param appId 所属应用ID
     * @param instanceParams 任务实例参数，仅 OpenAPI 创建时存在
     * @param wfInstanceId 工作流任务实例ID，仅工作流下的任务实例存在
     * @param expectTriggerTime 预期执行时间
     * @return 任务实例ID
     */
    public Long create(Long jobId, Long appId, String instanceParams, Long wfInstanceId, Long expectTriggerTime) {

        Long instanceId = idGenerateService.allocate();
        Date now = new Date();

        InstanceInfoDO newInstanceInfo = new InstanceInfoDO();
        newInstanceInfo.setJobId(jobId);
        newInstanceInfo.setAppId(appId);
        newInstanceInfo.setInstanceId(instanceId);
        newInstanceInfo.setInstanceParams(instanceParams);
        newInstanceInfo.setType(wfInstanceId == null ? InstanceType.NORMAL.getV() : InstanceType.WORKFLOW.getV());
        newInstanceInfo.setWfInstanceId(wfInstanceId);

        newInstanceInfo.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
        newInstanceInfo.setExpectedTriggerTime(expectTriggerTime);
        newInstanceInfo.setLastReportTime(-1L);
        newInstanceInfo.setGmtCreate(now);
        newInstanceInfo.setGmtModified(now);

        instanceInfoRepository.save(newInstanceInfo);
        return instanceId;
    }

    /**
     * 停止任务实例
     * @param instanceId 任务实例ID
     */
    public void stopInstance(Long instanceId) {

        log.info("[Instance-{}] try to stop the instance.", instanceId);
        try {

            InstanceInfoDO instanceInfo = instanceInfoRepository.findByInstanceId(instanceId);
            if (instanceInfo == null) {
                log.warn("[Instance-{}] can't find instanceInfo by instanceId.", instanceId);
                throw new IllegalArgumentException("invalid instanceId: " + instanceId);
            }

            // 判断状态，只有运行中才能停止
            if (!InstanceStatus.generalizedRunningStatus.contains(instanceInfo.getStatus())) {
                throw new IllegalArgumentException("can't stop finished instance!");
            }

            // 更新数据库，将状态置为停止
            instanceInfo.setStatus(STOPPED.getV());
            instanceInfo.setGmtModified(new Date());
            instanceInfo.setFinishedTime(System.currentTimeMillis());
            instanceInfo.setResult(SystemInstanceResult.STOPPED_BY_USER);
            instanceInfoRepository.saveAndFlush(instanceInfo);

            instanceManager.processFinishedInstance(instanceId, instanceInfo.getWfInstanceId(), STOPPED, SystemInstanceResult.STOPPED_BY_USER);

            /*
            不可靠通知停止 TaskTracker
            假如没有成功关闭，之后 TaskTracker 会再次 reportStatus，按照流程，instanceLog 会被更新为 RUNNING，开发者可以再次手动关闭
             */
            ActorSelection taskTrackerActor = OhMyServer.getTaskTrackerActor(instanceInfo.getTaskTrackerAddress());
            ServerStopInstanceReq req = new ServerStopInstanceReq(instanceId);
            taskTrackerActor.tell(req, null);

            log.info("[Instance-{}] update instanceInfo and send request succeed.", instanceId);

        }catch (IllegalArgumentException ie) {
            throw ie;
        }catch (Exception e) {
            log.error("[Instance-{}] stopInstance failed.", instanceId, e);
            throw e;
        }
    }

    /**
     * 获取任务实例的信息
     * @param instanceId 任务实例ID
     * @return 任务实例的信息
     */
    public InstanceInfoDTO getInstanceInfo(Long instanceId) {
        InstanceInfoDO instanceInfoDO = instanceInfoRepository.findByInstanceId(instanceId);
        if (instanceInfoDO == null) {
            log.warn("[Instance-{}] can't find InstanceInfo by instanceId.", instanceId);
            throw new IllegalArgumentException("invalid instanceId: " + instanceId);
        }
        InstanceInfoDTO instanceInfoDTO = new InstanceInfoDTO();
        BeanUtils.copyProperties(instanceInfoDO, instanceInfoDTO);
        return instanceInfoDTO;
    }

    /**
     * 获取任务实例的状态
     * @param instanceId 任务实例ID
     * @return 任务实例的状态
     */
    public InstanceStatus getInstanceStatus(Long instanceId) {
        InstanceInfoDO instanceInfoDO = instanceInfoRepository.findByInstanceId(instanceId);
        if (instanceInfoDO == null) {
            log.warn("[Instance-{}] can't find InstanceInfo by instanceId.", instanceId);
            throw new IllegalArgumentException("invalid instanceId: " + instanceId);
        }
        return InstanceStatus.of(instanceInfoDO.getStatus());
    }

    /**
     * 获取任务实例的详细运行详细
     * @param instanceId 任务实例ID
     * @return 详细运行状态
     */
    public InstanceDetail getInstanceDetail(Long instanceId) {

        InstanceInfoDO instanceInfoDO = instanceInfoRepository.findByInstanceId(instanceId);
        if (instanceInfoDO == null) {
            log.warn("[Instance-{}] can't find InstanceInfo by instanceId", instanceId);
            throw new IllegalArgumentException("invalid instanceId: " + instanceId);
        }

        InstanceStatus instanceStatus = InstanceStatus.of(instanceInfoDO.getStatus());

        InstanceDetail detail = new InstanceDetail();
        detail.setStatus(instanceStatus.getV());

        // 只要不是运行状态，只需要返回简要信息
        if (instanceStatus != RUNNING) {
            BeanUtils.copyProperties(instanceInfoDO, detail);
            return detail;
        }

        // 运行状态下，交由 TaskTracker 返回相关信息
        try {
            ServerQueryInstanceStatusReq req = new ServerQueryInstanceStatusReq(instanceId);
            ActorSelection taskTrackerActor = OhMyServer.getTaskTrackerActor(instanceInfoDO.getTaskTrackerAddress());
            CompletionStage<Object> askCS = Patterns.ask(taskTrackerActor, req, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
            AskResponse askResponse = (AskResponse) askCS.toCompletableFuture().get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (askResponse.isSuccess()) {
                InstanceDetail instanceDetail = askResponse.getData(InstanceDetail.class);
                instanceDetail.setRunningTimes(instanceInfoDO.getRunningTimes());
                return instanceDetail;
            }else {
                log.warn("[Instance-{}] ask InstanceStatus from TaskTracker failed, the message is {}.", instanceId, askResponse.getMessage());
            }

        }catch (Exception e) {
            log.warn("[Instance-{}] ask InstanceStatus from TaskTracker failed, exception is {}", instanceId, e.toString());
        }

        // 失败则返回基础版信息
        BeanUtils.copyProperties(instanceInfoDO, detail);
        return detail;
    }

}
