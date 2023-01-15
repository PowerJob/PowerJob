package tech.powerjob.worker.common;

import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.utils.JavaUtils;

/**
 * 获取 Worker 版本，便于开发者排查问题
 *
 * @author tjq
 * @since 2020/5/11
 */
public final class PowerJobWorkerVersion {

    private static String CACHE = null;

    /**
     * Return the full version string of the present OhMyScheduler-Worker codebase, or {@code null}
     * if it cannot be determined.
     * @return the version of OhMyScheduler-Worker or {@code null}
     * @see Package#getImplementationVersion()
     */
    public static String getVersion() {
        if (StringUtils.isEmpty(CACHE)) {
            CACHE = JavaUtils.determinePackageVersion(PowerJobWorkerVersion.class);
        }
        return CACHE;
    }

}
