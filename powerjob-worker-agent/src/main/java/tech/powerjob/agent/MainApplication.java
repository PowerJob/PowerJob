package tech.powerjob.agent;

import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.worker.PowerJobWorker;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.constants.StoreStrategy;
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
@Command(name = "PowerJobAgent", mixinStandardHelpOptions = true, version = "4.3.0", description = "powerjob-worker agent")
public class MainApplication implements Runnable {

    @Option(names = {"-a", "--app"}, description = "worker-agent's name", required = true)
    private String appName;

    @Option(names = {"-p", "--port"}, description = "transporter working port, not recommended to change")
    private Integer port = RemoteConstant.DEFAULT_WORKER_PORT;

    @Option(names = {"-o", "--protocol"}, description = "transporter protocol, AKKA or HTTP")
    private String protocol = Protocol.AKKA.name();

    @Option(names = {"-e", "--persistence"}, description = "storage strategy, DISK or MEMORY")
    private String storeStrategy = "DISK";

    @Option(names = {"-s", "--server"}, description = "oms-server's address, IP:Port OR domain", required = true)
    private String server = "localhost:7700";

    @Option(names = {"-l", "--length"}, description = "ProcessResult#msg max length")
    private int length = 1024;

    @Option(names = {"-t", "--tag"}, description = "worker-agent's tag")
    private String tag;

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new MainApplication());
        commandLine.execute(args);
    }

    @Override
    public void run() {

        PowerJobWorkerConfig cfg = new PowerJobWorkerConfig();
        try {

            cfg.setAppName(appName);
            cfg.setPort(port);
            cfg.setServerAddress(Splitter.on(",").splitToList(server));
            cfg.setStoreStrategy(StoreStrategy.MEMORY.name().equals(storeStrategy) ? StoreStrategy.MEMORY : StoreStrategy.DISK);
            cfg.setMaxResultLength(length);
            cfg.setTag(tag);
            cfg.setProtocol(Protocol.of(protocol));

            PowerJobWorker worker = new PowerJobWorker(cfg);

            worker.init();
        }catch (Exception e) {
            log.error("[PowerJobAgent] startup failed by config: {}.", cfg, e);
            ExceptionUtils.rethrow(e);
        }
    }
}
