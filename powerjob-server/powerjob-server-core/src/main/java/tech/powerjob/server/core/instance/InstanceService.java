package tech.powerjob.server.core.instance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.common.PowerQuery;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.SystemInstanceResult;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.request.ServerQueryInstanceStatusReq;
import tech.powerjob.common.request.ServerStopInstanceReq;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.server.common.constants.InstanceType;
import tech.powerjob.server.common.module.WorkerInfo;
import tech.powerjob.server.common.timewheel.TimerFuture;
import tech.powerjob.server.common.timewheel.holder.InstanceTimeWheelService;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.uid.IdGenerateService;
import tech.powerjob.server.persistence.QueryConvertUtils;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.repository.InstanceInfoRepository;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;
import tech.powerjob.server.remote.server.redirector.DesignateServer;
import tech.powerjob.server.remote.transporter.impl.ServerURLFactory;
import tech.powerjob.server.remote.transporter.TransportService;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static tech.powerjob.common.enums.InstanceStatus.RUNNING;
import static tech.powerjob.common.enums.InstanceStatus.STOPPED;

/**
 * 任务运行实例服务
 *
 * @author tjq
 * @since 2020/4/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceService {

    private final TransportService transportService;

    private final DispatchService dispatchService;

    private final IdGenerateService idGenerateService;

    private final InstanceManager instanceManager;

    private final JobInfoRepository jobInfoRepository;

    private final InstanceInfoRepository instanceInfoRepository;

    private final WorkerClusterQueryService workerClusterQueryService;

    /**
     * 创建任务实例（注意，该方法并不调用 saveAndFlush，如果有需要立即同步到DB的需求，请在方法结束后手动调用 flush）
     * ********************************************
     * 2021-02-03 modify by Echo009
     * 新增 jobParams ，每次均记录任务静态参数
     * ********************************************
     *
     * @param jobId             任务ID
     * @param appId             所属应用ID
     * @param jobParams         任务静态参数
     * @param instanceParams    任务实例参数，仅 OpenAPI 创建 或者 工作流任务 时存在
     * @param wfInstanceId      工作流任务实例ID，仅工作流下的任务实例存在
     * @param expectTriggerTime 预期执行时间
     * @return 任务实例ID
     */
    public InstanceInfoDO create(Long jobId, Long appId, String jobParams, String instanceParams, Long wfInstanceId, Long expectTriggerTime) {

        Long instanceId = idGenerateService.allocate();
        Date now = new Date();

        InstanceInfoDO newInstanceInfo = new InstanceInfoDO();
        newInstanceInfo.setJobId(jobId);
        newInstanceInfo.setAppId(appId);
        newInstanceInfo.setInstanceId(instanceId);
        newInstanceInfo.setJobParams(jobParams);
        newInstanceInfo.setInstanceParams(instanceParams);
        newInstanceInfo.setType(wfInstanceId == null ? InstanceType.NORMAL.getV() : InstanceType.WORKFLOW.getV());
        newInstanceInfo.setWfInstanceId(wfInstanceId);

        newInstanceInfo.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
        newInstanceInfo.setRunningTimes(0L);
        newInstanceInfo.setExpectedTriggerTime(expectTriggerTime);
        newInstanceInfo.setLastReportTime(-1L);
        newInstanceInfo.setGmtCreate(now);
        newInstanceInfo.setGmtModified(now);

        instanceInfoRepository.save(newInstanceInfo);
        return newInstanceInfo;
    }

    /**
     * 停止任务实例
     *
     * @param instanceId 任务实例ID
     */
    @DesignateServer
    public void stopInstance(Long appId,Long instanceId) {

        log.info("[Instance-{}] try to stop the instance instance in appId: {}", instanceId,appId);
        try {

            InstanceInfoDO instanceInfo = fetchInstanceInfo(instanceId);
            // 判断状态，只有运行中才能停止
            if (!InstanceStatus.GENERALIZED_RUNNING_STATUS.contains(instanceInfo.getStatus())) {
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
            Optional<WorkerInfo> workerInfoOpt = workerClusterQueryService.getWorkerInfoByAddress(instanceInfo.getAppId(), instanceInfo.getTaskTrackerAddress());
            if (workerInfoOpt.isPresent()) {
                ServerStopInstanceReq req = new ServerStopInstanceReq(instanceId);
                WorkerInfo workerInfo = workerInfoOpt.get();
                transportService.tell(workerInfo.getProtocol(), ServerURLFactory.stopInstance2Worker(workerInfo.getAddress()), req);
                log.info("[Instance-{}] update instanceInfo and send 'stopInstance' request succeed.", instanceId);
            } else {
                log.warn("[Instance-{}] update instanceInfo successfully but can't find TaskTracker to stop instance", instanceId);
            }

        } catch (IllegalArgumentException ie) {
            throw ie;
        } catch (Exception e) {
            log.error("[Instance-{}] stopInstance failed.", instanceId, e);
            throw e;
        }
    }

    /**
     * 重试任务（只有结束的任务运行重试）
     *
     * @param instanceId 任务实例ID
     */
    @DesignateServer
    public void retryInstance(Long appId, Long instanceId) {

        log.info("[Instance-{}] retry instance in appId: {}", instanceId, appId);

        InstanceInfoDO instanceInfo = fetchInstanceInfo(instanceId);
        if (!InstanceStatus.FINISHED_STATUS.contains(instanceInfo.getStatus())) {
            throw new PowerJobException("Only stopped instance can be retry!");
        }
        // 暂时不支持工作流任务的重试
        if (instanceInfo.getWfInstanceId() != null) {
            throw new PowerJobException("Workflow's instance do not support retry!");
        }

        instanceInfo.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
        instanceInfo.setExpectedTriggerTime(System.currentTimeMillis());
        instanceInfo.setFinishedTime(null);
        instanceInfo.setActualTriggerTime(null);
        instanceInfo.setTaskTrackerAddress(null);
        instanceInfo.setResult(null);
        instanceInfoRepository.saveAndFlush(instanceInfo);

        // 派发任务
        Long jobId = instanceInfo.getJobId();
        JobInfoDO jobInfo = jobInfoRepository.findById(jobId).orElseThrow(() -> new PowerJobException("can't find job info by jobId: " + jobId));
        dispatchService.dispatch(jobInfo, instanceId,Optional.of(instanceInfo),Optional.empty());
    }

    /**
     * 取消任务实例的运行
     * 接口使用条件：调用接口时间与待取消任务的预计执行时间有一定时间间隔，否则不保证可靠性！
     *
     * @param instanceId 任务实例
     */
    @DesignateServer
    public void cancelInstance(Long appId, Long instanceId) {
        log.info("[Instance-{}] try to cancel the instance with appId {}.", instanceId, appId);

        try {
            InstanceInfoDO instanceInfo = fetchInstanceInfo(instanceId);
            TimerFuture timerFuture = InstanceTimeWheelService.fetchTimerFuture(instanceId);

            boolean success;
            // 本机时间轮中存在该任务且顺利取消，抢救成功！
            if (timerFuture != null) {
                success = timerFuture.cancel();
            } else {
                // 调用该接口时间和预计调度时间相近时，理论上会出现问题，cancel 状态还没写进去另一边就完成了 dispatch，随后状态会被覆盖
                // 解决该问题的成本极高（分布式锁），因此选择不解决
                // 该接口使用条件：调用接口时间与待取消任务的预计执行时间有一定时间间隔，否则不保证可靠性
                success = InstanceStatus.WAITING_DISPATCH.getV() == instanceInfo.getStatus();
            }

            if (success) {
                instanceInfo.setStatus(InstanceStatus.CANCELED.getV());
                instanceInfo.setResult(SystemInstanceResult.CANCELED_BY_USER);
                // 如果写 DB 失败，抛异常，接口返回 false，即取消失败，任务会被 HA 机制重新调度执行，因此此处不需要任何处理
                instanceInfoRepository.saveAndFlush(instanceInfo);
                log.info("[Instance-{}] cancel the instance successfully.", instanceId);
            } else {
                log.warn("[Instance-{}] cancel the instance failed.", instanceId);
                throw new PowerJobException("instance already up and running");
            }

        } catch (Exception e) {
            log.error("[Instance-{}] cancelInstance failed.", instanceId, e);
            throw e;
        }
    }

    public List<InstanceInfoDTO> queryInstanceInfo(PowerQuery powerQuery) {
        return instanceInfoRepository
                .findAll(QueryConvertUtils.toSpecification(powerQuery))
                .stream()
                .map(InstanceService::directConvert)
                .collect(Collectors.toList());
    }

    /**
     * 获取任务实例的信息
     *
     * @param instanceId 任务实例ID
     * @return 任务实例的信息
     */
    public InstanceInfoDTO getInstanceInfo(Long instanceId) {
        return directConvert(fetchInstanceInfo(instanceId));
    }

    /**
     * 获取任务实例的状态
     *
     * @param instanceId 任务实例ID
     * @return 任务实例的状态
     */
    public InstanceStatus getInstanceStatus(Long instanceId) {
        InstanceInfoDO instanceInfoDO = fetchInstanceInfo(instanceId);
        return InstanceStatus.of(instanceInfoDO.getStatus());
    }

    /**
     * 获取任务实例的详细运行详细
     *
     * @param instanceId 任务实例ID
     * @return 详细运行状态
     */
    public InstanceDetail getInstanceDetail(Long instanceId) {

        InstanceInfoDO instanceInfoDO = fetchInstanceInfo(instanceId);

        InstanceStatus instanceStatus = InstanceStatus.of(instanceInfoDO.getStatus());

        InstanceDetail detail = new InstanceDetail();
        detail.setStatus(instanceStatus.getV());

        // 只要不是运行状态，只需要返回简要信息
        if (instanceStatus != RUNNING) {
            BeanUtils.copyProperties(instanceInfoDO, detail);
            return detail;
        }

        Optional<WorkerInfo> workerInfoOpt = workerClusterQueryService.getWorkerInfoByAddress(instanceInfoDO.getAppId(), instanceInfoDO.getTaskTrackerAddress());
        if (workerInfoOpt.isPresent()) {
            WorkerInfo workerInfo = workerInfoOpt.get();
            ServerQueryInstanceStatusReq req = new ServerQueryInstanceStatusReq(instanceId);
            try {
                final URL url = ServerURLFactory.queryInstance2Worker(workerInfo.getAddress());
                AskResponse askResponse = transportService.ask(workerInfo.getProtocol(), url, req, AskResponse.class)
                        .toCompletableFuture()
                        .get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (askResponse.isSuccess()) {
                    InstanceDetail instanceDetail = askResponse.getData(InstanceDetail.class);
                    instanceDetail.setRunningTimes(instanceInfoDO.getRunningTimes());
                    instanceDetail.setInstanceParams(instanceInfoDO.getInstanceParams());
                    return instanceDetail;
                }else {
                    log.warn("[Instance-{}] ask InstanceStatus from TaskTracker failed, the message is {}.", instanceId, askResponse.getMessage());
                }
            } catch (Exception e) {
                log.warn("[Instance-{}] ask InstanceStatus from TaskTracker failed, exception is {}", instanceId, e.toString());
            }
        }

        // 失败则返回基础版信息
        BeanUtils.copyProperties(instanceInfoDO, detail);
        return detail;
    }

    private InstanceInfoDO fetchInstanceInfo(Long instanceId) {
        InstanceInfoDO instanceInfoDO = instanceInfoRepository.findByInstanceId(instanceId);
        if (instanceInfoDO == null) {
            log.warn("[Instance-{}] can't find InstanceInfo by instanceId", instanceId);
            throw new IllegalArgumentException("invalid instanceId: " + instanceId);
        }
        return instanceInfoDO;
    }

    private static InstanceInfoDTO directConvert(InstanceInfoDO instanceInfoDO) {
        InstanceInfoDTO instanceInfoDTO = new InstanceInfoDTO();
        BeanUtils.copyProperties(instanceInfoDO, instanceInfoDTO);
        return instanceInfoDTO;
    }
}
