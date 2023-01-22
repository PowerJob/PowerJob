package tech.powerjob.worker.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.PowerJobDKey;

/**
 * 文件工具
 *
 * @author tjq
 * @since 2023/1/22
 */
@Slf4j
public class PowerFileUtils {

    /**
     * 获取工作目录
     * @return 允许用户通过启动配置文件自定义存储目录，默认为 user.home
     */
    public static String workspace() {
        String workspaceByDKey = System.getProperty(PowerJobDKey.WORKER_WORK_SPACE);
        if (StringUtils.isNotEmpty(workspaceByDKey)) {
            log.info("[PowerFileUtils] [workspace] use custom workspace: {}", workspaceByDKey);
            return workspaceByDKey;
        }
        final String userHome = System.getProperty("user.home").concat("/powerjob/worker");
        log.info("[PowerFileUtils] [workspace] use user.home as workspace: {}", userHome);
        return userHome;
    }
}
