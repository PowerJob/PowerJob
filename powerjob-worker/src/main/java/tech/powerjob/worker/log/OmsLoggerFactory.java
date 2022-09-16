package tech.powerjob.worker.log;

import tech.powerjob.common.model.LogConfig;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.log.impl.OmsLocalLogger;
import tech.powerjob.worker.log.impl.OmsServerLogger;

/**
 * OmsLoggerFactory
 *
 * @author tjq
 * @since 2022/9/17
 */
public class OmsLoggerFactory {

    public static OmsLogger build(Long instanceId, String logConfig, WorkerRuntime workerRuntime) {
        LogConfig cfg;
        if (logConfig == null) {
            cfg = new LogConfig();
        } else {
            try {
                cfg = JsonUtils.parseObject(logConfig, LogConfig.class);
            } catch (Exception ignore) {
                cfg = new LogConfig();
            }
        }

        if (LogConfig.LogType.LOCAL.getV().equals(cfg.getType())) {
            return new OmsLocalLogger(cfg);
        }
        return new OmsServerLogger(cfg, instanceId, workerRuntime.getOmsLogHandler());
    }
}
