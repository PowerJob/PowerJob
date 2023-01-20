package tech.powerjob.worker.common.utils;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.exception.PowerJobCheckedException;
import tech.powerjob.common.request.WorkerLogReportReq;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.HandlerLocation;
import tech.powerjob.remote.framework.base.ServerType;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.pojo.request.ProcessorMapTaskRequest;
import tech.powerjob.worker.pojo.request.ProcessorReportTaskStatusReq;
import tech.powerjob.worker.pojo.request.ProcessorTrackerStatusReportReq;

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

    public static void ptReportTask(ProcessorReportTaskStatusReq req, String address, WorkerRuntime workerRuntime) {
        final URL url = easyBuildUrl(ServerType.WORKER, WTT_PATH, WTT_HANDLER_REPORT_TASK_STATUS, address);
        workerRuntime.getTransporter().tell(url, req);
    }

    public static void ptReportSelfStatus(ProcessorTrackerStatusReportReq req, String address, WorkerRuntime workerRuntime) {
        final URL url = easyBuildUrl(ServerType.WORKER, WTT_PATH, WTT_HANDLER_REPORT_PROCESSOR_TRACKER_STATUS, address);
        workerRuntime.getTransporter().tell(url, req);
    }

    public static void reportLogs(WorkerLogReportReq req, String address, Transporter transporter) {
        final URL url = easyBuildUrl(ServerType.SERVER, SERVER_PATH, SERVER_HANDLER_REPORT_LOG, address);
        transporter.tell(url, req);
    }

    public static boolean reliablePtReportTask(ProcessorReportTaskStatusReq req, String address, WorkerRuntime workerRuntime) {
        try {
            final URL url = easyBuildUrl(ServerType.WORKER, WTT_PATH, WTT_HANDLER_REPORT_TASK_STATUS, address);
            final CompletionStage<AskResponse> completionStage = workerRuntime.getTransporter().ask(url, req, AskResponse.class);
            return completionStage
                    .toCompletableFuture()
                    .get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .isSuccess();

        } catch (Exception e) {
            log.warn("[PowerJobTransport] reliablePtReportTask failed: {}", req, e);
            return false;
        }
    }

    public static boolean reliableMapTask(ProcessorMapTaskRequest req, String address, WorkerRuntime workerRuntime) throws PowerJobCheckedException {
        try {
            final URL url = easyBuildUrl(ServerType.WORKER, WTT_PATH, WTT_HANDLER_MAP_TASK, address);
            final CompletionStage<AskResponse> completionStage = workerRuntime.getTransporter().ask(url, req, AskResponse.class);
            return completionStage
                    .toCompletableFuture()
                    .get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .isSuccess();

        } catch (Throwable throwable) {
            throw new PowerJobCheckedException(throwable);
        }
    }

    public static URL easyBuildUrl(ServerType serverType, String rootPath, String handlerPath, String address) {
        HandlerLocation handlerLocation = new HandlerLocation()
                .setServerType(serverType)
                .setRootPath(rootPath)
                .setMethodPath(handlerPath);
        return new URL()
                .setAddress(Address.fromIpv4(address))
                .setLocation(handlerLocation);
    }

}
