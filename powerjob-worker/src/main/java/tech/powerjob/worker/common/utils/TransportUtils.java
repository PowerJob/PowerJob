package tech.powerjob.worker.common.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.exception.PowerJobCheckedException;
import tech.powerjob.common.request.*;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.HandlerLocation;
import tech.powerjob.remote.framework.base.ServerType;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.pojo.request.*;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static tech.powerjob.common.RemoteConstant.*;

/**
 * 通讯工具
 *
 * @author tjq
 * @since 2023/1/20
 */
@Slf4j
public class TransportUtils {

    public static void ttReportInstanceStatus(TaskTrackerReportInstanceStatusReq req, String address, Transporter transporter) {
        final URL url = easyBuildUrl(ServerType.SERVER, S4W_PATH, S4W_HANDLER_REPORT_INSTANCE_STATUS, address);
        transporter.tell(url, req);
    }

    public static void ttStartPtTask(TaskTrackerStartTaskReq req, String address, Transporter transporter) {
        final URL url = easyBuildUrl(ServerType.WORKER, WPT_PATH, WPT_HANDLER_START_TASK, address);
        transporter.tell(url, req);
    }

    public static void ttStopPtInstance(TaskTrackerStopInstanceReq req, String address, Transporter transporter) {
        final URL url = easyBuildUrl(ServerType.WORKER, WPT_PATH, WPT_HANDLER_STOP_INSTANCE, address);
        transporter.tell(url, req);
    }

    public static void ptReportTask(ProcessorReportTaskStatusReq req, String address, WorkerRuntime workerRuntime) {
        final URL url = easyBuildUrl(ServerType.WORKER, WTT_PATH, WTT_HANDLER_REPORT_TASK_STATUS, address);
        workerRuntime.getTransporter().tell(url, req);
    }

    public static void ptReportSelfStatus(ProcessorTrackerStatusReportReq req, String address, WorkerRuntime workerRuntime) {
        final URL url = easyBuildUrl(ServerType.WORKER, WTT_PATH, WTT_HANDLER_REPORT_PROCESSOR_TRACKER_STATUS, address);
        workerRuntime.getTransporter().tell(url, req);
    }

    public static void reportLogs(WorkerLogReportReq req, String address, Transporter transporter) {
        final URL url = easyBuildUrl(ServerType.SERVER, S4W_PATH, S4W_HANDLER_REPORT_LOG, address);
        transporter.tell(url, req);
    }

    public static void reportWorkerHeartbeat(WorkerHeartbeat req, String address, Transporter transporter) {
        final URL url = easyBuildUrl(ServerType.SERVER, S4W_PATH, S4W_HANDLER_WORKER_HEARTBEAT, address);
        transporter.tell(url, req);
    }

    public static boolean reliablePtReportTask(ProcessorReportTaskStatusReq req, String address, WorkerRuntime workerRuntime) {
        try {
            return reliableAsk(ServerType.WORKER, WTT_PATH, WTT_HANDLER_REPORT_TASK_STATUS, address, req, workerRuntime.getTransporter()).isSuccess();
        } catch (Exception e) {
            log.warn("[PowerJobTransport] reliablePtReportTask failed: {}", req, e);
            return false;
        }
    }

    public static boolean reliableMapTask(ProcessorMapTaskRequest req, String address, WorkerRuntime workerRuntime) throws PowerJobCheckedException {
        try {
            return reliableAsk(ServerType.WORKER, WTT_PATH, WTT_HANDLER_MAP_TASK, address, req, workerRuntime.getTransporter()).isSuccess();
        } catch (Throwable throwable) {
            throw new PowerJobCheckedException(throwable);
        }
    }

    @SneakyThrows
    public static boolean reliableTtReportInstanceStatus(TaskTrackerReportInstanceStatusReq req, String address, Transporter transporter) {
        return reliableAsk(ServerType.SERVER, S4W_PATH, S4W_HANDLER_REPORT_INSTANCE_STATUS, address, req, transporter).isSuccess();
    }

    @SneakyThrows
    public static AskResponse reliableQueryJobCluster(WorkerQueryExecutorClusterReq req, String address, Transporter transporter) {
        return reliableAsk(ServerType.SERVER, S4W_PATH, S4W_HANDLER_QUERY_JOB_CLUSTER, address, req, transporter);
    }

    @SneakyThrows
    public static AskResponse reliableQueryContainerInfo(WorkerNeedDeployContainerRequest req, String address, Transporter transporter) {
        return reliableAsk(ServerType.SERVER, S4W_PATH, S4W_HANDLER_WORKER_NEED_DEPLOY_CONTAINER, address, req, transporter);
    }

    private static AskResponse reliableAsk(ServerType t, String rootPath, String handlerPath, String address, PowerSerializable req, Transporter transporter) throws Exception {
        final URL url = easyBuildUrl(t, rootPath, handlerPath, address);
        final CompletionStage<AskResponse> completionStage = transporter.ask(url, req, AskResponse.class);
        return completionStage
                .toCompletableFuture()
                .get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    public static URL easyBuildUrl(ServerType serverType, String rootPath, String handlerPath, String address) {
        HandlerLocation handlerLocation = new HandlerLocation()
                .setRootPath(rootPath)
                .setMethodPath(handlerPath);
        return new URL()
                .setServerType(serverType)
                .setAddress(Address.fromIpv4(address))
                .setLocation(handlerLocation);
    }

}
