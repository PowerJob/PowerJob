package com.github.kfcfans.oms.worker;

import com.github.kfcfans.oms.common.RemoteConstant;
import com.github.kfcfans.oms.worker.common.OhMyConfig;
import com.github.kfcfans.oms.worker.common.constants.StoreStrategy;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * 启动类
 *
 * @author tjq
 * @since 2020/5/20
 */
@Slf4j
@Command(name = "OhMyAgent", mixinStandardHelpOptions = true, version = "1.2.0", description = "OhMyScheduler-Worker代理")
public class MainApplication implements Runnable {

    @Option(names = {"-a", "--app"}, description = "worker-agent名称，可通过调度中心控制台创建", required = true)
    private String appName;

    @Option(names = {"-p", "--port"}, description = "worker-agent的ActorSystem监听端口，不建议更改")
    private Integer port = RemoteConstant.DEFAULT_WORKER_PORT;

    @Option(names = {"-e", "--persistence"}, description = "存储策略，枚举值，DISK 或 MEMORY")
    private String storeStrategy = "DISK";

    @Option(names = {"-s", "--server"}, description = "调度中心地址，多值英文逗号分隔，格式 IP:Port OR domain")
    private String server = "127.0.0.1:7700";

    @Option(names = {"-l", "--length"}, description = "返回值最大长度")
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
