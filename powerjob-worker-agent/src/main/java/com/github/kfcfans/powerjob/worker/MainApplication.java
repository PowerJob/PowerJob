package com.github.kfcfans.powerjob.worker;

import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.worker.common.OhMyConfig;
import com.github.kfcfans.powerjob.worker.common.constants.StoreStrategy;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * powerjob-worker-agent entry
 *
 * @author tjq
 * @since 2020/5/20
 */
@Slf4j
@Command(name = "OhMyAgent", mixinStandardHelpOptions = true, version = "3.4.3", description = "powerjob-worker agent")
public class MainApplication implements Runnable {

    @Option(names = {"-a", "--app"}, description = "worker-agent's name", required = true)
    private String appName;

    @Option(names = {"-p", "--port"}, description = "akka ActorSystem working port, not recommended to change")
    private Integer port = RemoteConstant.DEFAULT_WORKER_PORT;

    @Option(names = {"-e", "--persistence"}, description = "storage strategy, DISK or MEMORY")
    private String storeStrategy = "DISK";

    @Option(names = {"-s", "--server"}, description = "oms-server's address, IP:Port OR domain", required = true)
    private String server = "localhost:7700";

    @Option(names = {"-l", "--length"}, description = "ProcessResult#msg max length")
    private int length = 1024;

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new MainApplication());
        commandLine.execute(args);
    }

    @Override
    public void run() {

        OhMyConfig cfg = new OhMyConfig();
        try {

            cfg.setAppName(appName);
            cfg.setPort(port);
            cfg.setServerAddress(Splitter.on(",").splitToList(server));
            cfg.setStoreStrategy(StoreStrategy.MEMORY.name().equals(storeStrategy) ? StoreStrategy.MEMORY : StoreStrategy.DISK);
            cfg.setMaxResultLength(length);

            OhMyWorker ohMyWorker = new OhMyWorker();
            ohMyWorker.setConfig(cfg);

            ohMyWorker.init();
        }catch (Exception e) {
            log.error("[OhMyAgent] startup failed by config: {}.", cfg, e);
            ExceptionUtils.rethrow(e);
        }
    }
}
