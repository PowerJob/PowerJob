package tech.powerjob.worker.log;

import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.enums.LogType;
import tech.powerjob.common.model.LogConfig;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.log.impl.*;

/**
 * OmsLoggerFactory
 *
 * @author tjq
 * @since 2022/9/17
 */
public class OmsLoggerFactory {

    public static OmsLogger build(Long instanceId, String logConfig, WorkerRuntime workerRuntime) {
        LogConfig cfg;
        if (StringUtils.isEmpty(logConfig)) {
            cfg = new LogConfig();
        } else {
            try {
                cfg = JsonUtils.parseObject(logConfig, LogConfig.class);
            } catch (Exception ignore) {
                cfg = new LogConfig();
            }
        }

        switch (LogType.of(cfg.getType())) {
            case LOCAL:
                return new OmsLocalLogger(cfg);
            case STDOUT:
                return new OmsStdOutLogger(cfg);
            case NULL:
                return new OmsNullLogger();
            case LOCAL_AND_ONLINE:
                return new OmsServerAndLocalLogger(cfg, instanceId, workerRuntime.getOmsLogHandler());
            default:
                return new OmsServerLogger(cfg, instanceId, workerRuntime.getOmsLogHandler());
        }
    }
}
