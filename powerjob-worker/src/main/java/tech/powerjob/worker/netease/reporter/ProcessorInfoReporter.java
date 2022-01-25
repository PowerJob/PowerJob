package tech.powerjob.worker.netease.reporter;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.annotation.NetEaseCustomFeature;
import tech.powerjob.common.enums.CustomFeatureEnum;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.request.WorkerProcessorInfoReportReq;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.utils.AkkaUtils;
import tech.powerjob.worker.netease.scanner.ProcessorScanner;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * @author Echo009
 * @since 2022/1/25
 */
@Slf4j
@NetEaseCustomFeature(CustomFeatureEnum.PROCESSOR_AUTO_REGISTRY)
public class ProcessorInfoReporter {

    private final WorkerRuntime workerRuntime;

    private final Set<String> processors;

    private static final int RETRY_TIMES = 5;


    public ProcessorInfoReporter(WorkerRuntime workerRuntime,String scanPackages) {
        this.workerRuntime = workerRuntime;
        this.processors = new ProcessorScanner().scan(scanPackages);
    }

    public void reportProcessorInfo(String currentServer) {
        if (StringUtils.isEmpty(currentServer)) {
            log.warn("[ProcessorInfoReporter] No server available!");
            return;
        }

        if (processors.isEmpty()) {
            log.warn("[ProcessorInfoReporter] No processor available!");
            return;
        }

        WorkerProcessorInfoReportReq workerProcessorInfoReportReq = new WorkerProcessorInfoReportReq();
        workerProcessorInfoReportReq.setProcessors(processors);
        workerProcessorInfoReportReq.setAppId(workerRuntime.getAppId());
        workerProcessorInfoReportReq.setAppName(workerRuntime.getWorkerConfig().getAppName());
        workerProcessorInfoReportReq.setReportTime(System.currentTimeMillis());
        workerProcessorInfoReportReq.setWorkerAddress(workerRuntime.getWorkerAddress());

        String serverPath = AkkaUtils.getServerActorPath(currentServer);
        ActorSelection actorSelection = workerRuntime.getActorSystem().actorSelection(serverPath);
        actorSelection.tell(workerProcessorInfoReportReq, null);

        int count = 1;
        while (count <= RETRY_TIMES ){
            count ++ ;
            if (reportCore(workerProcessorInfoReportReq, actorSelection)){
                return;
            }
        }
        throw new PowerJobException("[ProcessorInfoReporter] fail to report processor info");
    }

    @SuppressWarnings("all")
    private boolean reportCore(WorkerProcessorInfoReportReq workerProcessorInfoReportReq, ActorSelection actorSelection) {
        CompletionStage<Object> askCs = Patterns.ask(actorSelection, workerProcessorInfoReportReq, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
        boolean serverAccepted = false;
        try {
            AskResponse askResponse = (AskResponse) askCs.toCompletableFuture().get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            serverAccepted = askResponse.isSuccess();
        } catch (Exception e) {
            log.warn("[ProcessorInfoReporter] report processor info failed!", e);
        }
        log.info("[ProcessorInfoReporter] report processor info success!");
        return serverAccepted;
    }

}
