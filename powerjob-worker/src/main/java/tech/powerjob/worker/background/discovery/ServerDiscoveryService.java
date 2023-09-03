package tech.powerjob.worker.background.discovery;

import tech.powerjob.common.model.WorkerAppInfo;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 服务发现
 *
 * @author tjq
 * @since 2023/9/2
 */
public interface ServerDiscoveryService {

    /**
     * 鉴权 & 附带信息下发
     * @return appInfo
     */
    WorkerAppInfo assertApp();

    /**
     * 获取当前的 server 地址
     * @return server 地址
     */
    String getCurrentServerAddress();

    /**
     * 定时检查
     * @param timingPool timingPool
     */
    void timingCheck(ScheduledExecutorService timingPool);
}
